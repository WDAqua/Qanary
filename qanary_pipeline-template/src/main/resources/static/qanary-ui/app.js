"use strict";

// ---------------------------------------------------------------------------
// Qanary embedded frontend — talks to the pipeline's own REST endpoints.
// No build step, no external dependencies.
// ---------------------------------------------------------------------------

const QA = "http://www.wdaqua.eu/qa#";
const state = {
  components: [],   // [{name, status, accessible, url}]
  order: [],        // selected component names, in execution order
};

const $ = (sel) => document.querySelector(sel);
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) =>
  ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const shortUri = (u) => String(u ?? "").replace(/^.*[#/:]/, "");
const looksLikeSparql = (s) => /\b(SELECT|ASK|CONSTRUCT|DESCRIBE|PREFIX|INSERT)\b/i.test(String(s ?? ""));

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
        : `<span class="badge down">${esc(c.status || "offline")}</span>`);
    if (c.accessible) li.addEventListener("click", () => toggleComponent(c.name));
    else li.title = "registered but not reachable — cannot be selected";
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

  const btn = $("#run");
  btn.disabled = true;
  runStatus.textContent = "running the pipeline… this can take a moment.";

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
    await showResults(graph);
    runStatus.textContent = "";
  } catch (e) {
    runStatus.className = "run-status error";
    runStatus.textContent = "Error: " + e.message;
  } finally {
    btn.disabled = false;
  }
}

// ---------------------------------------------------------------------------
// Results: answer, generated SPARQL, per-component intermediate info
// ---------------------------------------------------------------------------
async function showResults(graph) {
  $("#results").classList.remove("hidden");
  $("#graph-meta").textContent = "result graph: " + graph;
  $("#results").scrollIntoView({ behavior: "smooth", block: "start" });

  await Promise.all([
    showAnswer(graph),
    showGeneratedSparql(graph),
    showComponentFacts(graph),
  ]);
}

async function showAnswer(graph) {
  const el = $("#answer-summary");
  try {
    const rows = await sparql(`PREFIX qa: <${QA}>
PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
SELECT ?value ?component FROM <${graph}> WHERE {
  ?a a qa:AnnotationOfAnswerJson ; oa:hasBody ?b ; oa:annotatedBy ?component .
  ?b rdf:value ?value .
} LIMIT 5`);
    if (!rows.length) {
      el.innerHTML = `<div class="answer none"><span class="lbl">Answer</span><br>No answer was stored for this run.</div>`;
      return;
    }
    const r = rows[0];
    el.innerHTML = `<div class="answer"><span class="lbl">Answer · by ${esc(shortUri(cell(r, "component")))}</span>` +
      `<div>${renderAnswerValue(cell(r, "value"))}</div></div>`;
  } catch (e) {
    el.innerHTML = `<div class="answer none"><span class="lbl">Answer</span><br>could not read the answer (${esc(e.message)})</div>`;
  }
}

// Render a stored answer in a readable way: SPARQL-results JSON → "subject — predicate — object" lines
function renderAnswerValue(value) {
  try {
    const obj = JSON.parse(value);
    const b = obj && obj.results && obj.results.bindings;
    if (Array.isArray(b) && b.length) {
      const items = b.slice(0, 25).map((row) => {
        const vals = Object.keys(row).map((k) => `<code>${esc(row[k].value)}</code>`);
        return `<li>${vals.join(" — ")}</li>`;
      });
      return `<ul class="answer-rows">${items.join("")}</ul>`;
    }
  } catch (_) { /* not JSON — show as-is */ }
  return esc(value);
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
    el.textContent = rows.map((r) =>
      `# generated by ${shortUri(cell(r, "component"))}\n${cell(r, "sparql")}`).join("\n\n");
  } catch (e) {
    el.textContent = "could not read the generated SPARQL query (" + e.message + ")";
  }
}

async function showComponentFacts(graph) {
  const el = $("#component-intermediate");
  let rows;
  try {
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
    const card = document.createElement("div");
    card.className = "comp-card";
    card.innerHTML = `<h4>${esc(comp)} <span class="count">${facts.length} annotation${facts.length === 1 ? "" : "s"}</span></h4>` +
      facts.map(describeFact).join("");
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
    case "AnnotationOfAnswerSPARQL":
      return tag("Generated a SPARQL query:",
        `<code class="miniquery">${esc((content || "").slice(0, 600))}</code>`);
    case "AnnotationOfAnswerJson":
    case "AnnotationOfTextAnswerJson":
      return tag("Produced the answer.", "");
    case "AnnotationOfAnswerInGraph":
      return tag("Stored the answer in the result graph.", "");
    case "AnnotationOfLogQuery":
    case "AnnotationOfLogMethod":
    case "AnnotationOfLog":
      return tag("Recorded a processing-log entry.", "");
    default:
      return tag(type.replace(/^AnnotationOf/, "") + ":", fmt(content));
  }
}

// ---------------------------------------------------------------------------
// wire up + live refresh
// ---------------------------------------------------------------------------
$("#run").addEventListener("click", run);
$("#refresh").addEventListener("click", loadComponents);
loadComponents();
setInterval(loadComponents, 12000); // keep the component list up to date
