"use strict";

// ---------------------------------------------------------------------------
// Qanary embedded frontend — talks to the pipeline's own REST endpoints.
// No build step. Only optional runtime dependency: YASGUI (loaded from CDN).
// ---------------------------------------------------------------------------

const QA = "http://www.wdaqua.eu/qa#";
const state = {
  components: [],   // [{name, status, accessible, url, serviceUrl, host, port, ...}]
  order: [],        // selected component names, in execution order
};

const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => [...document.querySelectorAll(sel)];
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) =>
  ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const shortUri = (u) => String(u ?? "").replace(/^.*[#/:]/, "");
const stringify = (v) => (typeof v === "string" ? v : JSON.stringify(v));
const isUrl = (v) => typeof v === "string" && /^https?:\/\//i.test(v);

// ---------------------------------------------------------------------------
// Theme switching: light / dark / high contrast (persisted in localStorage)
// ---------------------------------------------------------------------------
const THEME_KEY = "qanary-theme";
function applyTheme(theme) {
  document.documentElement.setAttribute("data-theme", theme);
  $$("[data-theme-btn]").forEach((b) =>
    b.setAttribute("aria-pressed", String(b.dataset.themeBtn === theme)));
  try { localStorage.setItem(THEME_KEY, theme); } catch (_) { /* ignore */ }
}
function initTheme() {
  let theme = null;
  try { theme = localStorage.getItem(THEME_KEY); } catch (_) { /* ignore */ }
  if (!theme) {
    theme = (window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches) ? "dark" : "light";
  }
  applyTheme(theme);
  $$("[data-theme-btn]").forEach((b) =>
    b.addEventListener("click", () => applyTheme(b.dataset.themeBtn)));
}

// ---------------------------------------------------------------------------
// Question history → <datalist> (auto-completion from earlier user inputs)
// ---------------------------------------------------------------------------
const HISTORY_KEY = "qanary-question-history";
function loadHistory() {
  try { return JSON.parse(localStorage.getItem(HISTORY_KEY)) || []; } catch (_) { return []; }
}
function renderHistory() {
  $("#question-history").innerHTML = loadHistory().map((q) => `<option value="${esc(q)}"></option>`).join("");
}
function rememberQuestion(q) {
  if (!q) return;
  let h = loadHistory().filter((x) => x !== q);
  h.unshift(q);
  h = h.slice(0, 25);
  try { localStorage.setItem(HISTORY_KEY, JSON.stringify(h)); } catch (_) { /* ignore */ }
  renderHistory();
}

// ---------------------------------------------------------------------------
// Copy to clipboard (with a fallback for non-secure contexts)
// ---------------------------------------------------------------------------
async function copyText(text) {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch (_) { /* fall through to legacy path */ }
  try {
    const ta = document.createElement("textarea");
    ta.value = text;
    ta.style.position = "fixed";
    ta.style.opacity = "0";
    document.body.appendChild(ta);
    ta.select();
    const ok = document.execCommand("copy");
    document.body.removeChild(ta);
    return ok;
  } catch (_) { return false; }
}

// delegated handler for every ⧉ copy button (SPARQL output + table cells)
document.addEventListener("click", async (e) => {
  const btn = e.target.closest(".copy-btn");
  if (!btn) return;
  let text = btn.dataset.copyText;
  if (text == null && btn.dataset.copyTarget) {
    const tgt = document.querySelector(btn.dataset.copyTarget);
    text = tgt ? tgt.innerText : "";
  }
  const ok = await copyText(text ?? "");
  const label = btn.textContent;
  btn.classList.toggle("copied", ok);
  btn.textContent = ok ? "✓ Copied" : "copy failed";
  setTimeout(() => { btn.textContent = label; btn.classList.remove("copied"); }, 1400);
});

// ---------------------------------------------------------------------------
// Backend access
// ---------------------------------------------------------------------------
async function getJSON(url) {
  const r = await fetch(url, { headers: { Accept: "application/json" } });
  if (!r.ok) throw new Error(`${url} → HTTP ${r.status}`);
  return r.json();
}

// run a SELECT against the pipeline's triplestore proxy; returns the binding rows
async function sparql(query) {
  const r = await fetch("/sparql?query=" + encodeURIComponent(query),
    { headers: { Accept: "application/sparql-results+json" } });
  if (!r.ok) throw new Error(`/sparql → HTTP ${r.status}`);
  const data = await r.json();
  return (data.results && data.results.bindings) || [];
}
const cell = (row, v) => (row[v] ? row[v].value : undefined);

// ---------------------------------------------------------------------------
// Components
// ---------------------------------------------------------------------------
async function loadComponents() {
  let list;
  try {
    list = await getJSON("/components/availability");
  } catch (e) {
    $("#component-list").innerHTML = `<li class="empty">could not load components (${esc(e.message)})</li>`;
    return;
  }
  list.sort((a, b) => a.name.localeCompare(b.name));
  state.components = list;
  // drop selections that are no longer accessible / no longer registered
  const usable = new Set(list.filter((c) => c.accessible).map((c) => c.name));
  state.order = state.order.filter((n) => usable.has(n));
  renderComponents();
  renderOrder();
  const up = list.filter((c) => c.accessible).length;
  $("#pipeline-status").textContent = `${up}/${list.length} components available`;
}

function renderComponents() {
  const ul = $("#component-list");
  if (!state.components.length) {
    ul.innerHTML = `<li class="empty">no components registered with this pipeline</li>`;
    return;
  }
  ul.innerHTML = "";
  for (const c of state.components) {
    const li = document.createElement("li");
    const selected = state.order.includes(c.name);
    li.className = "comp" + (c.accessible ? "" : " offline") + (selected ? " selected" : "");
    li.innerHTML =
      `<span class="name">${esc(c.name)}</span>` +
      (c.accessible
        ? `<span class="badge up">up</span><span class="add">${selected ? "✓ added" : "+ add"}</span>`
        : `<span class="badge down">${esc(c.status || "offline")}</span>`) +
      `<button class="info-btn" title="component information" aria-label="component information">ⓘ</button>`;
    if (c.accessible) li.addEventListener("click", () => toggleComponent(c.name));
    else li.title = "registered but not reachable — cannot be selected";
    li.querySelector(".info-btn").addEventListener("click", (e) => {
      e.stopPropagation();
      openComponentModal(c);
    });
    ul.appendChild(li);
  }
}

function toggleComponent(name) {
  const i = state.order.indexOf(name);
  if (i >= 0) state.order.splice(i, 1);
  else state.order.push(name);
  renderComponents();
  renderOrder();
}

// ---------------------------------------------------------------------------
// Component information overlay (IP / port / service URL + embedded iframe)
// ---------------------------------------------------------------------------
function openComponentModal(c) {
  $("#modal-title").textContent = c.name;
  const rows = [
    ["Status", (c.status || "?") + (c.accessible ? " · selectable" : " · not selectable")],
    ["Host / IP", c.host],
    ["Port", c.port != null ? String(c.port) : null],
    ["Service URL", c.serviceUrl],
    ["Health URL", c.healthUrl],
    ["Management URL", c.managementUrl],
    ["Pipeline proxy", c.url],
  ].filter(([, v]) => v != null && v !== "");
  $("#modal-info").innerHTML = rows.map(([k, v]) =>
    `<div class="kv"><span class="k">${esc(k)}</span><span class="v">` +
    (isUrl(v) ? `<a href="${esc(v)}" target="_blank" rel="noopener">${esc(v)}</a>` : esc(v)) +
    `</span></div>`).join("");

  const iframe = $("#modal-iframe");
  const note = document.querySelector(".modal-iframe-note");
  if (c.serviceUrl) {
    iframe.src = c.serviceUrl;
    iframe.classList.remove("hidden");
    note.classList.remove("hidden");
  } else {
    iframe.removeAttribute("src");
    iframe.classList.add("hidden");
    note.classList.add("hidden");
  }
  $("#component-modal").classList.remove("hidden");
}
function closeComponentModal() {
  $("#component-modal").classList.add("hidden");
  $("#modal-iframe").removeAttribute("src"); // stop loading the embedded page
}
document.addEventListener("click", (e) => {
  if (e.target.closest("[data-modal-close]")) closeComponentModal();
});
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape" && !$("#component-modal").classList.contains("hidden")) closeComponentModal();
});

// ---------------------------------------------------------------------------
// Pipeline order (drag & drop)
// ---------------------------------------------------------------------------
function renderOrder() {
  const ol = $("#pipeline-order");
  if (!state.order.length) {
    ol.innerHTML = `<li class="empty">no components selected yet</li>`;
    return;
  }
  ol.innerHTML = "";
  state.order.forEach((name, idx) => {
    const li = document.createElement("li");
    li.className = "item";
    li.draggable = true;
    li.dataset.name = name;
    li.innerHTML =
      `<span class="ord">${idx + 1}</span>` +
      `<span class="grip" aria-hidden="true">⠿</span>` +
      `<span class="name">${esc(name)}</span>` +
      `<button class="remove" title="remove">×</button>`;
    li.querySelector(".remove").addEventListener("click", () => toggleComponent(name));
    li.addEventListener("dragstart", () => li.classList.add("dragging"));
    li.addEventListener("dragend", onDragEnd);
    ol.appendChild(li);
  });
}

function onDragEnd(e) {
  e.target.classList.remove("dragging");
  // read the new DOM order back into state, then renumber
  state.order = [...$("#pipeline-order").querySelectorAll("li.item")].map((li) => li.dataset.name);
  renderOrder();
}

$("#pipeline-order").addEventListener("dragover", (e) => {
  e.preventDefault();
  const ol = e.currentTarget;
  const dragging = ol.querySelector(".dragging");
  if (!dragging) return;
  const over = e.target.closest("li.item");
  if (!over || over === dragging) return;
  const rect = over.getBoundingClientRect();
  const after = e.clientY - rect.top > rect.height / 2;
  ol.insertBefore(dragging, after ? over.nextSibling : over);
});

// ---------------------------------------------------------------------------
// Run the pipeline
// ---------------------------------------------------------------------------
async function run() {
  const question = $("#question").value.trim();
  const runStatus = $("#run-status");
  runStatus.className = "run-status";
  if (!question) { runStatus.textContent = "Please enter a question."; return; }
  if (!state.order.length) { runStatus.textContent = "Please select at least one component."; return; }

  rememberQuestion(question); // make it available for auto-completion next time

  const btn = $("#run");
  btn.disabled = true;
  runStatus.textContent = "processing the question… this can take a moment.";

  try {
    const body = new URLSearchParams();
    body.append("question", question);
    const lang = $("#language").value.trim();
    if (lang) body.append("language", lang);
    const triples = $("#additionaltriples").value.trim();
    if (triples) body.append("additionaltriples", triples);
    for (const name of state.order) body.append("componentlist[]", name);

    const resp = await fetch("/startquestionansweringwithtextquestion", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded", Accept: "application/json" },
      body: body.toString(),
    });
    if (!resp.ok) throw new Error(`pipeline returned HTTP ${resp.status}`);
    const result = await resp.json();
    const graph = result.outGraph || result.inGraph;
    if (!graph) throw new Error("no result graph returned by the pipeline");

    runStatus.textContent = "done — reading the results from the triplestore…";
    await showResults(graph, result);
    runStatus.textContent = "";
  } catch (e) {
    runStatus.className = "run-status error";
    runStatus.textContent = "Error: " + e.message;
  } finally {
    btn.disabled = false;
  }
}

// ---------------------------------------------------------------------------
// SPARQL pretty-printer (whitespace-only; safe — never alters token content).
// Returns { ok, text }: ok=false → input is not valid SPARQL, show as text.
// ---------------------------------------------------------------------------
function tokenizeSparql(s) {
  const tokens = [];
  const n = s.length;
  let i = 0;
  while (i < n) {
    const c = s[i];
    if (c === " " || c === "\t" || c === "\r" || c === "\n") { i++; continue; }
    if (c === "#") { // comment to end of line
      let j = i; while (j < n && s[j] !== "\n") j++;
      tokens.push({ t: "comment", v: s.slice(i, j) }); i = j; continue;
    }
    if (c === "<") { // IRI (no whitespace inside) — otherwise a '<' operator
      const m = /^<[^<>"{}|^`\\\s]*>/.exec(s.slice(i));
      if (m) { tokens.push({ t: "iri", v: m[0] }); i += m[0].length; continue; }
      tokens.push({ t: "word", v: c }); i++; continue;
    }
    if (c === '"' || c === "'") { // string literal (single or triple quoted)
      const triple = s.substr(i, 3);
      let len;
      if (triple === '"""' || triple === "'''") {
        let j = i + 3;
        while (j < n && s.substr(j, 3) !== triple) { if (s[j] === "\\") j++; j++; }
        len = Math.min(j + 3, n) - i;
      } else {
        let j = i + 1;
        while (j < n && s[j] !== c) { if (s[j] === "\\") j++; j++; }
        len = Math.min(j + 1, n) - i;
      }
      tokens.push({ t: "string", v: s.substr(i, len) }); i += len; continue;
    }
    if ("{}()[].;,".includes(c)) { tokens.push({ t: "punc", v: c }); i++; continue; }
    const m = /^[^\s{}()\[\].;,"'#<]+/.exec(s.slice(i));
    if (m) { tokens.push({ t: "word", v: m[0] }); i += m[0].length; continue; }
    tokens.push({ t: "word", v: c }); i++;
  }
  return tokens;
}

function bracketsBalanced(tokens) {
  const stack = [];
  const close = { ")": "(", "]": "[", "}": "{" };
  for (const tk of tokens) {
    if (tk.t !== "punc") continue;
    if (tk.v === "(" || tk.v === "[" || tk.v === "{") stack.push(tk.v);
    else if (close[tk.v] && stack.pop() !== close[tk.v]) return false;
  }
  return stack.length === 0;
}

function renderSparql(tokens) {
  const IND = "  ";
  const noSpaceBefore = new Set([")", "]", ",", ";", ".", "("]);
  const noSpaceAfter = new Set(["(", "["]);
  const forms = /^(PREFIX|BASE|SELECT|ASK|CONSTRUCT|DESCRIBE|WITH|INSERT|DELETE|CLEAR|LOAD|CREATE|DROP)$/i;
  let out = "";
  let depth = 0;
  let prev = null;
  const atLineStart = () => out === "" || /\n[ \t]*$/.test(out);
  const newline = () => { out = out.replace(/[ \t]+$/, ""); out += "\n" + IND.repeat(Math.max(depth, 0)); };

  for (const tk of tokens) {
    if (tk.t === "comment") {
      if (!atLineStart()) newline();
      out += tk.v; newline(); prev = tk; continue;
    }
    if (tk.t === "punc" && tk.v === "{") {
      if (!atLineStart() && !out.endsWith(" ")) out += " ";
      out += "{"; depth++; newline(); prev = tk; continue;
    }
    if (tk.t === "punc" && tk.v === "}") {
      depth = Math.max(depth - 1, 0);
      out = out.replace(/[ \t]*$/, ""); out += "\n" + IND.repeat(depth);
      out += "}"; newline(); prev = tk; continue;
    }
    if (tk.t === "punc" && (tk.v === "." || tk.v === ";")) {
      out = out.replace(/[ \t]+$/, ""); out += " " + tk.v; newline(); prev = tk; continue;
    }
    if (tk.t === "word" && forms.test(tk.v) && !atLineStart()) newline();

    let sep = "";
    if (!atLineStart()) {
      if (prev && prev.t === "punc" && noSpaceAfter.has(prev.v)) sep = "";
      else if (tk.t === "punc" && noSpaceBefore.has(tk.v)) sep = "";
      else sep = " ";
    }
    out += sep + tk.v;
    prev = tk;
  }
  return out.split("\n").map((l) => l.replace(/[ \t]+$/, "")).join("\n")
    .replace(/\n{3,}/g, "\n\n").replace(/^\n+/, "").replace(/\n+$/, "");
}

function formatSparql(raw) {
  const text = String(raw ?? "").trim();
  if (!text) return { ok: false, text: "" };
  let tokens;
  try { tokens = tokenizeSparql(text); } catch (_) { return { ok: false, text }; }
  const hasForm = tokens.some((t) => t.t === "word" &&
    /^(SELECT|ASK|CONSTRUCT|DESCRIBE|INSERT|DELETE|LOAD|CLEAR|CREATE|DROP|WITH)$/i.test(t.v));
  if (!hasForm || !bracketsBalanced(tokens)) return { ok: false, text };
  try { return { ok: true, text: renderSparql(tokens) }; }
  catch (_) { return { ok: false, text }; }
}

// ---------------------------------------------------------------------------
// Results: generated SPARQL, JSON answer table, per-component intermediate info
// ---------------------------------------------------------------------------
async function showResults(graph, result) {
  $("#results").classList.remove("hidden");
  showPipelineResponse(result);
  $("#graph-meta").textContent = "result graph: " + graph;
  $("#results").scrollIntoView({ behavior: "smooth", block: "start" });
  loadGraphIntoYasgui(graph);

  await Promise.all([
    showGeneratedSparql(graph),
    showAnswerTable(graph),
    showComponentFacts(graph),
  ]);
}

// The raw JSON the pipeline returns (endpoint, inGraph, outGraph, question) —
// shown as a key/value table (copy each value) plus the raw JSON (copy all),
// so other applications can integrate against this pipeline run.
function showPipelineResponse(result) {
  const el = $("#pipeline-response");
  if (!result || typeof result !== "object") { el.innerHTML = ""; return; }
  const rows = Object.entries(result).map(([k, v]) => [k, stringify(v)]);
  const raw = JSON.stringify(result, null, 2);
  el.innerHTML =
    tableHTML(["field", "value"], rows, [false, true]) +
    `<div class="code-wrap pipeline-raw">` +
      `<button class="copy-btn" data-copy-target="#pipeline-response-raw" title="copy JSON">⧉ Copy JSON</button>` +
      `<pre id="pipeline-response-raw" class="code">${esc(raw)}</pre>` +
    `</div>`;
}

async function showGeneratedSparql(graph) {
  const el = $("#sparql-output");
  try {
    const rows = await sparql(`PREFIX qa: <${QA}>
PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
SELECT ?sparql ?component ?time FROM <${graph}> WHERE {
  ?a a qa:AnnotationOfAnswerSPARQL ; oa:hasBody ?sparql ; oa:annotatedBy ?component .
  OPTIONAL { ?a oa:annotatedAt ?time }
} ORDER BY DESC(?time)`);
    if (!rows.length) { el.textContent = "— no SPARQL query was generated for this run —"; return; }
    el.textContent = rows.map((r) => {
      const f = formatSparql(cell(r, "sparql"));
      const header = `# generated by ${shortUri(cell(r, "component"))}` +
        (f.ok ? "" : "  (not valid SPARQL — shown as text)");
      return `${header}\n${f.text}`;
    }).join("\n\n");
  } catch (e) {
    el.textContent = "could not read the generated SPARQL query (" + e.message + ")";
  }
}

// Show the backend's JSON answer as a table; every value cell gets a copy button.
async function showAnswerTable(graph) {
  const el = $("#answer-table");
  let rows;
  try {
    rows = await sparql(`PREFIX qa: <${QA}>
PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
SELECT ?value ?component ?time FROM <${graph}> WHERE {
  ?a a qa:AnnotationOfAnswerJson ; oa:hasBody ?b ; oa:annotatedBy ?component .
  ?b rdf:value ?value .
  OPTIONAL { ?a oa:annotatedAt ?time }
} ORDER BY DESC(?time)`);
  } catch (e) {
    el.innerHTML = `<div class="answer-none">could not read the answer (${esc(e.message)})</div>`;
    return;
  }
  if (!rows.length) {
    el.innerHTML = `<div class="answer-none">No JSON answer was stored for this run.</div>`;
    return;
  }
  const r = rows[0];
  el.innerHTML = `<p class="answer-by">provided by <strong>${esc(shortUri(cell(r, "component")))}</strong></p>` +
    jsonToTable(cell(r, "value"));
}

// Turn a JSON answer string into an HTML table (handles SPARQL-results JSON,
// arrays of objects, arrays of scalars, plain objects and scalars).
function jsonToTable(raw) {
  let data;
  try { data = JSON.parse(raw); } catch (_) { return singleValueTable(raw); }

  if (data && data.results && Array.isArray(data.results.bindings)) {
    const b = data.results.bindings;
    if (!b.length) return `<div class="answer-none">The query returned no bindings.</div>`;
    const vars = (data.head && data.head.vars) || [...new Set(b.flatMap((x) => Object.keys(x)))];
    const rows = b.map((row) => vars.map((v) => (row[v] ? row[v].value : "")));
    return tableHTML(vars, rows);
  }
  if (Array.isArray(data)) {
    if (!data.length) return `<div class="answer-none">Answer is an empty list.</div>`;
    if (data[0] !== null && typeof data[0] === "object") {
      const cols = [...new Set(data.flatMap((o) => Object.keys(o)))];
      return tableHTML(cols, data.map((o) => cols.map((c) => stringify(o[c] ?? ""))));
    }
    return tableHTML(["#", "value"], data.map((v, i) => [String(i + 1), stringify(v)]), [false, true]);
  }
  if (data && typeof data === "object") {
    const entries = Object.entries(data);
    if (!entries.length) return `<div class="answer-none">Answer is an empty object.</div>`;
    return tableHTML(["field", "value"], entries.map(([k, v]) => [k, stringify(v)]), [false, true]);
  }
  return singleValueTable(stringify(data));
}

function tableHTML(cols, rows, copyable) {
  const can = (i) => (copyable ? !!copyable[i] : true);
  const head = `<thead><tr>${cols.map((c) => `<th>${esc(c)}</th>`).join("")}</tr></thead>`;
  const body = rows.map((cells) =>
    `<tr>${cells.map((v, i) => valueCell(v, can(i))).join("")}</tr>`).join("");
  return `<table class="answer-table">${head}<tbody>${body}</tbody></table>`;
}

function valueCell(text, copyable) {
  const t = stringify(text ?? "");
  const btn = (copyable && t !== "")
    ? `<button class="copy-btn" data-copy-text="${esc(t)}" title="copy value">⧉</button>` : "";
  return `<td><div class="val"><span>${esc(t)}</span>${btn}</div></td>`;
}

function singleValueTable(text) {
  return tableHTML(["value"], [[stringify(text)]]);
}

async function showComponentFacts(graph) {
  const el = $("#component-intermediate");
  let rows;
  try {
    // exclude AnnotationOfLogMethod (internal method-call log noise)
    rows = await sparql(`PREFIX qa: <${QA}>
PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
SELECT ?type ?component ?target ?body ?value ?time FROM <${graph}> WHERE {
  ?annotation a ?type ; oa:annotatedBy ?component .
  OPTIONAL { ?annotation oa:hasTarget ?target }
  OPTIONAL { ?annotation oa:annotatedAt ?time }
  OPTIONAL { ?annotation oa:hasBody ?body }
  OPTIONAL { ?annotation oa:hasBody ?bn . ?bn rdf:value ?value }
  FILTER(STRSTARTS(STR(?type), "${QA}"))
  FILTER(?type != qa:AnnotationOfLogMethod)
} ORDER BY ?component ?time`);
  } catch (e) {
    el.innerHTML = `<p class="hint">could not read the component information (${esc(e.message)})</p>`;
    return;
  }
  if (!rows.length) { el.innerHTML = `<p class="hint">no component annotations were stored.</p>`; return; }

  // group by component
  const byComp = new Map();
  for (const r of rows) {
    const comp = shortUri(cell(r, "component")) || "(unknown)";
    if (!byComp.has(comp)) byComp.set(comp, []);
    byComp.get(comp).push(r);
  }
  el.innerHTML = "";
  for (const [comp, facts] of byComp) {
    // collapsible box (collapsed by default; click the summary to open/close)
    const card = document.createElement("details");
    card.className = "comp-card";
    card.innerHTML =
      `<summary><span class="h4">${esc(comp)} ` +
      `<span class="count">${facts.length} annotation${facts.length === 1 ? "" : "s"}</span></span></summary>` +
      `<div class="facts-body">${facts.map(describeFact).join("")}</div>`;
    el.appendChild(card);
  }
}

// Turn one annotation row into a human-readable sentence (no LLM, just templates)
function describeFact(r) {
  const type = shortUri(cell(r, "type"));
  const body = cell(r, "body");
  const value = cell(r, "value");
  const content = value ?? body;
  const isUri = content && /^(https?:|urn:)/.test(content);
  const fmt = (c) => c == null ? "" : (isUri ? `<code>${esc(c)}</code>` : esc(c));

  const tag = (label, html) => `<div class="fact"><span class="ftype">${esc(type)}</span>${label}${html ? " " + html : ""}</div>`;

  switch (type) {
    case "AnnotationOfInstance":
    case "AnnotationOfSpotInstance":
      return tag("Recognised an entity mention in the question, linked to", fmt(content));
    case "AnnotationOfQuestionLanguage":
      return tag("Detected the question language:", fmt(content));
    case "AnnotationOfQuestionTranslation":
      return tag("Translated the question to:", fmt(content));
    case "AnnotationOfImprovedQuestion":
      return tag("Refined the question:", fmt(content));
    case "AnnotationOfTextRepresentation":
    case "AnnotationOfTextualRepresentation":
      return tag("Provided a text representation:", fmt(content));
    case "AnnotationOfRelation":
      return tag("Identified a relation:", fmt(content));
    case "AnnotationOfClass":
      return tag("Identified a class/type:", fmt(content));
    case "AnnotationOfAnswerDataType":
      return tag("Determined the answer data type:", fmt(content));
    case "AnnotationOfAnswerSPARQL": {
      const f = formatSparql(content);
      return tag(f.ok ? "Generated a SPARQL query:" : "Generated a query (not valid SPARQL — shown as text):",
        `<code class="miniquery">${esc((f.text || "").slice(0, 1200))}</code>`);
    }
    case "AnnotationOfAnswerJson":
    case "AnnotationOfTextAnswerJson":
      return tag("Produced the answer.", "");
    case "AnnotationOfAnswerInGraph":
      return tag("Stored the answer in the result graph.", "");
    case "AnnotationOfLogQuery":
    case "AnnotationOfLog":
      return tag("Recorded a processing-log entry.", "");
    default:
      return tag(type.replace(/^AnnotationOf/, "") + ":", fmt(content));
  }
}

// ---------------------------------------------------------------------------
// Embedded YASGUI (optional — only if the library loaded from the CDN)
// ---------------------------------------------------------------------------
let yasgui = null;
function initYasgui() {
  const host = $("#yasgui");
  if (typeof Yasgui === "undefined") {
    host.classList.add("hidden");
    $("#yasgui-fallback").classList.remove("hidden");
    return;
  }
  try {
    yasgui = new Yasgui(host, {
      requestConfig: { endpoint: location.origin + "/sparql", method: "POST" },
      copyEndpointOnNewTab: false,
    });
    setYasguiQuery(`# Explore the Qanary triplestore. Each question-answering run is stored in its own graph.
SELECT ?graph (COUNT(*) AS ?triples) WHERE {
  GRAPH ?graph { ?s ?p ?o }
} GROUP BY ?graph ORDER BY DESC(?triples) LIMIT 25`);
  } catch (e) {
    host.classList.add("hidden");
    $("#yasgui-fallback").classList.remove("hidden");
  }
}

function setYasguiQuery(q) {
  try { if (yasgui && yasgui.getTab()) yasgui.getTab().setQuery(q); } catch (_) { /* ignore */ }
}

// After a run, pre-load the result graph into the YASGUI editor for convenience.
function loadGraphIntoYasgui(graph) {
  setYasguiQuery(`# Everything stored for this question-answering run
SELECT ?subject ?predicate ?object FROM <${graph}> WHERE {
  ?subject ?predicate ?object
} LIMIT 200`);
}

// ---------------------------------------------------------------------------
// wire up + live refresh
// ---------------------------------------------------------------------------
initTheme();
initYasgui();
renderHistory();
$("#run").addEventListener("click", run);
$("#refresh").addEventListener("click", loadComponents);
loadComponents();
setInterval(loadComponents, 12000); // keep the component list up to date
