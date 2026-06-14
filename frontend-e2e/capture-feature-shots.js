"use strict";

/**
 * Documentation helper for the frontend features that the pass/fail E2E
 * (frontend-e2e.js) does not capture on its own:
 *
 *   10-component-info-modal.png   the ⓘ component-information overlay
 *                                 (host/IP, port, service URL + embedded iframe)
 *   11-component-facts-expanded.png  a collapsed/expanded "what each component did"
 *                                 box, showing the pretty-printed AnnotationOfAnswerSPARQL
 *   06-answer-table.png           the JSON answer rendered as a table (with per-value
 *                                 copy buttons) using the frontend's own jsonToTable().
 *
 * The answer table normally needs QE-SparqlExecuter, which depends on the
 * rate-limited public Wikidata endpoint (see README). To document the renderer
 * reliably, this script fetches the real Wikidata answer for the example
 * question's entity in-page (browser CORS) and feeds it through the live
 * window.jsonToTable() — i.e. the actual frontend code, with real data.
 *
 * Env: BASE_URL, CHROME_PATH, OUT (defaults match frontend-e2e.js).
 */

const path = require("path");
const puppeteer = require("puppeteer-core");

const BASE_URL = process.env.BASE_URL || "http://localhost:40111";
const CHROME_PATH = process.env.CHROME_PATH || "/usr/bin/google-chrome";
const OUT = process.env.OUT || path.join(__dirname, "screenshots");
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// the example entity (Hawaiian pizza, Q590076) and the query the QB builds
const WDQS = "https://query.wikidata.org/sparql";
const ANSWER_QUERY = `PREFIX wikibase: <http://wikiba.se/ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
SELECT ?label ?pLabel ?oLabel WHERE {
  <http://www.wikidata.org/entity/Q590076> rdfs:label ?label .
  <http://www.wikidata.org/entity/Q590076> ?p ?o .
  ?o rdfs:label ?oLabel .
  ?prop wikibase:directClaim ?p ; rdfs:label ?pLabel .
  FILTER(LANG(?pLabel)="en") FILTER(LANG(?oLabel)="en") FILTER(LANG(?label)="en")
} LIMIT 12`;

(async () => {
  const browser = await puppeteer.launch({
    executablePath: CHROME_PATH, headless: "new",
    args: ["--no-sandbox", "--disable-gpu", "--disable-dev-shm-usage", "--force-color-profile=srgb"],
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 1180, height: 1024, deviceScaleFactor: 2 });
  await page.goto(BASE_URL + "/qa", { waitUntil: "domcontentloaded", timeout: 60000 });
  await page.waitForSelector("#component-list li.comp", { timeout: 30000 });

  // --- (6) component information overlay ---
  await page.evaluate(() => {
    const li = [...document.querySelectorAll("#component-list li.comp:not(.offline)")][0]
      || document.querySelector("#component-list li.comp");
    li.querySelector(".info-btn").click();
  });
  await page.waitForSelector("#component-modal:not(.hidden)", { timeout: 5000 });
  await sleep(2500); // let the embedded service page load in the iframe
  await page.screenshot({ path: path.join(OUT, "10-component-info-modal.png") });
  console.log("saved 10-component-info-modal.png");
  await page.evaluate(() => document.querySelector(".modal-close").click());
  await sleep(300);

  // --- process the question with the query-generating components ---
  await page.type("#question", "Who is the inventor of the Hawaiian Pizza?");
  const want = ["NEL-WikidataLookup", "QB-Wikidata"];
  for (let a = 0; a < 6; a++) {
    for (const n of want) {
      await page.evaluate((nm) => {
        const li = [...document.querySelectorAll("#component-list li.comp")].find((x) =>
          x.querySelector(".name").textContent.trim() === nm
          && !x.classList.contains("offline") && !x.classList.contains("selected"));
        if (li) li.click();
      }, n);
      await sleep(150);
    }
    if (await page.$$eval("#pipeline-order li.item", (n) => n.length) >= 2) break;
  }
  await page.click("#run");
  await page.waitForFunction(() => {
    const o = document.querySelector("#sparql-output");
    return o && o.textContent.trim() && o.textContent.trim() !== "—";
  }, { timeout: 90000, polling: 1000 });
  await sleep(500);

  // --- (8/10) expand a component box (shows pretty-printed AnnotationOfAnswerSPARQL) ---
  await page.evaluate(() => {
    const cards = [...document.querySelectorAll("#component-intermediate details.comp-card")];
    const qb = cards.find((c) => /QB-Wikidata/.test(c.textContent)) || cards[0];
    if (qb) qb.open = true;
  });
  await sleep(300);
  await page.evaluate(() => {
    const h = [...document.querySelectorAll("#results h3")].find((x) => x.textContent.startsWith("What each component"));
    if (h) { h.scrollIntoView({ block: "start" }); window.scrollBy(0, -90); }
  });
  await sleep(300);
  await page.screenshot({ path: path.join(OUT, "11-component-facts-expanded.png") });
  console.log("saved 11-component-facts-expanded.png");

  // --- answer table: real Wikidata answer through the frontend's own jsonToTable() ---
  const answer = await page.evaluate(async (endpoint, query) => {
    const r = await fetch(endpoint + "?query=" + encodeURIComponent(query),
      { headers: { Accept: "application/sparql-results+json" } });
    return r.ok ? await r.text() : null;
  }, WDQS, ANSWER_QUERY);

  if (answer) {
    await page.waitForFunction(() => typeof window.jsonToTable === "function", { timeout: 10000 });
    await page.evaluate(() => { const t = document.querySelector(".topbar"); if (t) t.style.position = "static"; });
    await page.evaluate((a) => {
      document.querySelector("#answer-table").innerHTML =
        '<p class="answer-by">provided by <strong>QE-SparqlExecuter</strong></p>' + window.jsonToTable(a);
    }, answer);
    await sleep(200);
    const box = await page.evaluate(() => {
      const h = [...document.querySelectorAll("#results h3")].find((x) => x.textContent.startsWith("Answer"));
      const t = document.querySelector("#answer-table");
      const top = h.getBoundingClientRect().top + window.scrollY - 12;
      const r = t.getBoundingClientRect();
      return { x: Math.max(0, r.left - 20), y: Math.max(0, top), w: r.width + 40, h: r.height + (r.top + window.scrollY - top) + 20 };
    });
    await page.screenshot({ path: path.join(OUT, "06-answer-table.png"), clip: { x: box.x, y: box.y, width: box.w, height: box.h } });
    console.log("saved 06-answer-table.png (populated)");
  } else {
    console.warn("WDQS did not return an answer; left 06-answer-table.png as the live (empty) state");
  }

  await browser.close();
})().catch((e) => { console.error(e); process.exit(1); });
