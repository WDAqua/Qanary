"use strict";

/**
 * End-to-end test for the embedded Qanary frontend.
 *
 * As a developer (see new-frontend.md), this test uses the example question of
 * Qanary_minimal_Python_example in the frontend and verifies that the generated
 * SPARQL query is retrieved. It also captures a screenshot of every step so the
 * frontend process is documented.
 *
 * Prerequisites: a running Qanary pipeline that serves the frontend at BASE_URL/qa
 * (e.g. start Qanary_minimal_Python_example with docker compose up) and a local
 * Chrome/Chromium.
 *
 * Configuration (env):
 *   BASE_URL     pipeline base URL              (default http://localhost:40111)
 *   CHROME_PATH  Chrome/Chromium executable     (default /usr/bin/google-chrome)
 *   OUT          screenshot output directory    (default ./screenshots)
 *   RUN_TIMEOUT  ms to wait for a pipeline run  (default 180000)
 *   COMPONENTS   comma-separated component names to select; if unset, all
 *                currently-reachable components are selected.
 *
 * Note: the generated SPARQL query is produced by the query builder (QB). The
 * query executer (QE-SparqlExecuter) only runs that query against the public
 * Wikidata endpoint, which is aggressively rate-limited (HTTP 429) and can block
 * the synchronous pipeline. To make this test deterministic, pin the
 * query-generating components, e.g.
 *   COMPONENTS=NEL-WikidataLookup,QB-Wikidata npm test
 *
 * Exit code 0 = SPARQL query retrieved; non-zero = failure.
 */

const fs = require("fs");
const path = require("path");
const puppeteer = require("puppeteer-core");

// The designated example question of Qanary_minimal_Python_example
// (see Qanary_minimal_Python_example/qanary_client.py).
const QUESTION = "Who is the inventor of the Hawaiian Pizza?";

const BASE_URL = process.env.BASE_URL || "http://localhost:40111";
const CHROME_PATH = process.env.CHROME_PATH || "/usr/bin/google-chrome";
const OUT = process.env.OUT || path.join(__dirname, "screenshots");
const RUN_TIMEOUT = parseInt(process.env.RUN_TIMEOUT || "180000", 10);
const COMPONENTS = (process.env.COMPONENTS || "").split(",").map((s) => s.trim()).filter(Boolean);

let stepNo = 0;
// Capture a focused, readable viewport screenshot. If `sel` is given, scroll
// that element just below the sticky top bar; otherwise capture the page top.
async function snap(page, name, sel) {
  stepNo += 1;
  const file = path.join(OUT, `${String(stepNo).padStart(2, "0")}-${name}.png`);
  if (sel) {
    await page.evaluate((s) => {
      const e = document.querySelector(s);
      if (e) { e.scrollIntoView({ block: "start" }); window.scrollBy(0, -90); }
    }, sel);
  } else {
    await page.evaluate(() => window.scrollTo(0, 0));
  }
  await new Promise((r) => setTimeout(r, 250));
  await page.screenshot({ path: file }); // viewport only — readable
  console.log(`  📸 step ${stepNo}: ${name} → ${file}`);
}

function fail(msg) {
  console.error("\n❌ E2E FAILED: " + msg);
  process.exitCode = 1;
}

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  console.log(`Qanary frontend E2E\n  base url : ${BASE_URL}\n  question : ${QUESTION}\n  out dir  : ${OUT}\n`);

  const browser = await puppeteer.launch({
    executablePath: CHROME_PATH,
    headless: "new",
    args: ["--no-sandbox", "--disable-gpu", "--disable-dev-shm-usage", "--force-color-profile=srgb"],
  });

  try {
    const page = await browser.newPage();
    await page.setViewport({ width: 1180, height: 1024, deviceScaleFactor: 2 });

    // --- Step: load the frontend -------------------------------------------
    console.log("→ loading the frontend");
    await page.goto(BASE_URL + "/qa", { waitUntil: "networkidle0", timeout: 60000 });
    await page.waitForSelector("#component-list li.comp", { timeout: 30000 });
    await snap(page, "frontend-loaded");

    // --- Step: enter the example question ----------------------------------
    console.log("→ entering the example question");
    await page.click("#question");
    await page.type("#question", QUESTION);
    await snap(page, "question-entered");

    // --- Step: select the components ---------------------------------------
    // Select deterministically by name (the list re-renders on each selection
    // and on the periodic refresh, so handles go stale). Retry until every
    // requested component is selected.
    const allReachable = await page.$$eval(
      "#component-list li.comp:not(.offline) .name", (n) => n.map((x) => x.textContent.trim()));
    if (!allReachable.length) throw new Error("no reachable components are registered");
    const reachable = COMPONENTS.length
      ? COMPONENTS.filter((n) => allReachable.includes(n))
      : allReachable;
    if (COMPONENTS.length) {
      const missing = COMPONENTS.filter((n) => !allReachable.includes(n));
      if (missing.length) console.warn("  ⚠ requested but not reachable (skipped): " + missing.join(", "));
    }
    console.log("→ selecting components: " + reachable.join(", "));
    for (let attempt = 0; attempt < 6; attempt++) {
      for (const name of reachable) {
        await page.evaluate((n) => {
          const li = [...document.querySelectorAll("#component-list li.comp")]
            .find((x) => x.querySelector(".name").textContent.trim() === n
              && !x.classList.contains("offline") && !x.classList.contains("selected"));
          if (li) li.click();
        }, name);
        await new Promise((r) => setTimeout(r, 150));
      }
      const sel = await page.$$eval("#pipeline-order li.item .name", (n) => n.length);
      if (sel >= reachable.length) break;
    }
    await page.waitForSelector("#pipeline-order li.item", { timeout: 5000 });
    const selected = await page.$$eval("#pipeline-order li.item .name", (n) => n.map((x) => x.textContent));
    console.log("  selected pipeline order: " + selected.join(" → "));
    if (!selected.length) throw new Error("no components could be selected");
    await snap(page, "components-selected", "#components-card");

    // --- Step: run the pipeline --------------------------------------------
    console.log("→ running the pipeline");
    await page.click("#run");
    await new Promise((r) => setTimeout(r, 600));
    await snap(page, "pipeline-running", "#run");

    // wait until the result card is shown and the SPARQL output is populated
    console.log(`→ waiting for results (up to ${Math.round(RUN_TIMEOUT / 1000)}s)`);
    await page.waitForFunction(() => {
      const res = document.querySelector("#results");
      const out = document.querySelector("#sparql-output");
      const err = document.querySelector("#run-status.error");
      if (err && err.textContent.trim()) return true; // surface errors quickly
      return res && !res.classList.contains("hidden") && out && out.textContent.trim() && out.textContent.trim() !== "—";
    }, { timeout: RUN_TIMEOUT, polling: 1000 });

    const runError = await page.$eval("#run-status", (e) => e.textContent.trim()).catch(() => "");
    if (runError && /error/i.test(await page.$eval("#run-status", (e) => e.className))) {
      await snap(page, "run-error");
      throw new Error("pipeline run reported: " + runError);
    }

    // --- Step: retrieve the generated SPARQL query -------------------------
    const sparql = (await page.$eval("#sparql-output", (e) => e.textContent)).trim();
    console.log("\n→ generated SPARQL query retrieved:\n" +
      sparql.split("\n").map((l) => "    " + l).join("\n") + "\n");
    await snap(page, "result-sparql", "#results");

    // --- Step: answer table + intermediate facts ---------------------------
    const answerRows = await page.$$eval("#answer-table .answer-table tbody tr", (r) => r.length).catch(() => 0);
    console.log(`  answer table rows: ${answerRows}`);
    await snap(page, "answer-table", "#answer-table");

    // --- Step: embedded YASGUI ---------------------------------------------
    await snap(page, "yasgui-editor", "#yasgui-card");

    // --- Step: theme switching (dark + high contrast) ----------------------
    console.log("→ switching themes");
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.click('[data-theme-btn="dark"]');
    await new Promise((r) => setTimeout(r, 300));
    await snap(page, "theme-dark");
    await page.click('[data-theme-btn="contrast"]');
    await new Promise((r) => setTimeout(r, 300));
    await snap(page, "theme-contrast");
    await page.click('[data-theme-btn="light"]');

    // --- Assertion ---------------------------------------------------------
    const looksLikeSparql = /\b(SELECT|ASK|CONSTRUCT|DESCRIBE|PREFIX|INSERT)\b/i.test(sparql);
    if (!looksLikeSparql) {
      fail("a result was shown but it does not look like a SPARQL query:\n" + sparql);
    } else {
      console.log("✅ E2E PASSED: the frontend retrieved a generated SPARQL query for the example question.");
    }
  } catch (e) {
    fail(e.message);
  } finally {
    await browser.close();
  }
})();
