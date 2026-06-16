"use strict";

// ---------------------------------------------------------------------------
// Qanary embedded frontend — talks to the pipeline's own REST endpoints.
// No build step. Only optional runtime dependency: YASGUI (loaded from CDN).
//
// Supports multiple question tabs (each an independent question + configuration)
// and stores every used configuration in the browser's database (IndexedDB) so
// it can be replayed later.
// ---------------------------------------------------------------------------

const QA = "http://www.wdaqua.eu/qa#";

// global state shared by all tabs
const state = {
  components: [],   // [{name, status, accessible, url, serviceUrl, host, port, ...}]
};

// per-tab state
let tabs = [];
let activeTabId = null;
let tabSeq = 0;

const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => [...document.querySelectorAll(sel)];
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) =>
  ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const shortUri = (u) => String(u ?? "").replace(/^.*[#/:]/, "");
const stringify = (v) => (typeof v === "string" ? v : JSON.stringify(v));
const isUrl = (v) => typeof v === "string" && /^https?:\/\//i.test(v);
const formatWhen = (ts) => { try { return ts ? new Date(ts).toLocaleString() : ""; } catch (_) { return ""; } };

// ---------------------------------------------------------------------------
// Tabs
// ---------------------------------------------------------------------------
function newTab(config) {
  const id = "t" + (++tabSeq);
  const tab = {
    id,
    question: (config && config.question) || "",
    language: (config && config.language) || "",
    additionaltriples: (config && config.additionaltriples) || "",
    order: ((config && config.components) || []).slice(),
    results: null,      // { graph, result }
    status: "",
    statusError: false,
    running: false,
    runStartedAt: null, // performance.now() when the current run began
    runElapsedMs: null, // frozen processing time once a run has finished
    error: null,        // { status, statusText, body } of a failed pipeline call
  };
  tabs.push(tab);
  return tab;
}
function activeTab() { return tabs.find((t) => t.id === activeTabId); }

function tabTitle(t) {
  const q = (t.question || "").trim();
  const title = q || "New question";
  return title.length > 28 ? title.slice(0, 28) + "…" : title;
}

function renderTabs() {
  const wrap = $("#tabs");
  wrap.innerHTML = "";
  tabs.forEach((t) => {
    const b = document.createElement("button");
    b.className = "tab" + (t.id === activeTabId ? " active" : "");
    b.dataset.tabId = t.id;
    b.setAttribute("role", "tab");
    b.innerHTML = `<span class="tab-title">${esc(tabTitle(t))}</span>` +
      (tabs.length > 1 ? `<span class="tab-close" title="close tab" aria-label="close tab">×</span>` : "");
    wrap.appendChild(b);
  });
}

function saveActiveTabInputs() {
  const t = activeTab();
  if (!t) return;
  t.question = $("#question").value;
  t.language = $("#language").value;
  t.additionaltriples = $("#additionaltriples").value;
}

function loadTabIntoDom() {
  const t = activeTab();
  if (!t) return;
  $("#question").value = t.question || "";
  $("#language").value = t.language || "";
  $("#additionaltriples").value = t.additionaltriples || "";
  renderComponents();
  renderOrder();
  $("#run").disabled = !!t.running;
  renderRunStatus(t);
  renderRunError(t);
  if (t.running) ensureRunTimer();
  if (t.results) {
    showResults(t.results.graph, t.results.result, { scroll: false });
  } else {
    $("#results").classList.add("hidden");
    clearResultsDom();
  }
}

function clearResultsDom() {
  $("#pipeline-response").innerHTML = "";
  $("#sparql-output").textContent = "—";
  $("#answer-table").innerHTML = "";
  $("#component-intermediate").innerHTML = "";
  $("#graph-meta").textContent = "";
}

function switchTab(id) {
  if (id === activeTabId) return;
  saveActiveTabInputs();
  activeTabId = id;
  renderTabs();
  loadTabIntoDom();
}

function addTab(config) {
  saveActiveTabInputs();
  const t = newTab(config);
  activeTabId = t.id;
  renderTabs();
  loadTabIntoDom();
  $("#question").focus();
  return t;
}

function closeTab(id) {
  const idx = tabs.findIndex((t) => t.id === id);
  if (idx < 0) return;
  const wasActive = id === activeTabId;
  tabs.splice(idx, 1);
  if (!tabs.length) { const t = newTab(); activeTabId = t.id; }
  else if (wasActive) { activeTabId = tabs[Math.max(0, idx - 1)].id; }
  renderTabs();
  if (wasActive) loadTabIntoDom();
}

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
// Saved configurations — browser database (IndexedDB)
// ---------------------------------------------------------------------------
const DB_NAME = "qanary-frontend";
const DB_VERSION = 1;
const STORE = "configurations";

function idb() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE)) {
        const os = db.createObjectStore(STORE, { keyPath: "id", autoIncrement: true });
        os.createIndex("savedAt", "savedAt");
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

async function getAllConfigurations() {
  const db = await idb();
  return new Promise((resolve, reject) => {
    const req = db.transaction(STORE, "readonly").objectStore(STORE).getAll();
    req.onsuccess = () => resolve((req.result || []).sort((a, b) => (b.savedAt || 0) - (a.savedAt || 0)));
    req.onerror = () => reject(req.error);
  });
}

// store a used configuration; identical configs are de-duplicated (savedAt updated)
async function saveConfiguration(cfg) {
  const all = await getAllConfigurations().catch(() => []);
  const key = (c) => JSON.stringify([c.question || "", c.components || [], c.language || "", c.additionaltriples || ""]);
  const existing = all.find((c) => key(c) === key(cfg));
  const db = await idb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, "readwrite");
    const os = tx.objectStore(STORE);
    if (existing) { existing.savedAt = cfg.savedAt; os.put(existing); }
    else os.add(cfg);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

async function deleteConfiguration(id) {
  const db = await idb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, "readwrite");
    tx.objectStore(STORE).delete(id);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

async function openHistory() {
  $("#history-modal").classList.remove("hidden");
  await renderHistoryList();
}

async function renderHistoryList() {
  const el = $("#history-list");
  let configs;
  try { configs = await getAllConfigurations(); }
  catch (e) { el.innerHTML = `<p class="hint">could not read saved configurations (${esc(e.message)})</p>`; return; }
  if (!configs.length) {
    el.innerHTML = `<p class="hint">No saved configurations yet — process a question and it will be stored here.</p>`;
    return;
  }
  const avail = new Set(state.components.filter((c) => c.accessible).map((c) => c.name));
  el.innerHTML = configs.map((cfg) => {
    const comps = cfg.components || [];
    const missing = comps.filter((n) => !avail.has(n));
    const usable = comps.length > 0 && missing.length === 0;
    const chips = comps.length
      ? comps.map((n) => `<span class="cfg-comp${avail.has(n) ? "" : " missing"}">${esc(n)}</span>`)
        .join(`<span class="cfg-arrow">→</span>`)
      : `<span class="hint">no components</span>`;
    const params = [];
    if (cfg.language) params.push("language: " + esc(cfg.language));
    if (cfg.additionaltriples) params.push("additional triples");
    return `<div class="cfg${usable ? "" : " disabled"}">
      <div class="cfg-main">
        <div class="cfg-q">${esc(cfg.question || "(no question)")}</div>
        <div class="cfg-comps">${chips}</div>
        ${params.length ? `<div class="cfg-params">${params.join(" · ")}</div>` : ""}
        ${missing.length ? `<div class="cfg-missing">unavailable: ${missing.map(esc).join(", ")}</div>` : ""}
        <div class="cfg-meta">${esc(formatWhen(cfg.savedAt))}</div>
      </div>
      <div class="cfg-actions">
        <button class="cfg-use" data-use="${cfg.id}" ${usable ? "" : "disabled"}
          title="${usable ? "open this configuration in a new tab" : "some required components are unavailable"}">Use</button>
        <button class="cfg-del" data-del="${cfg.id}" title="delete this configuration">🗑</button>
      </div>
    </div>`;
  }).join("");
}

function useConfiguration(cfg) {
  addTab({
    question: cfg.question,
    language: cfg.language,
    additionaltriples: cfg.additionaltriples,
    components: (cfg.components || []).slice(),
  });
  closeModal($("#history-modal"));
  window.scrollTo({ top: 0, behavior: "smooth" });
}

// ---------------------------------------------------------------------------
// "Run from code" overlay — curl / Python for the current configuration
// ---------------------------------------------------------------------------
let codeLang = "curl";

function currentConfig() {
  const t = activeTab();
  return {
    question: $("#question").value.trim(),
    language: $("#language").value.trim(),
    additionaltriples: $("#additionaltriples").value.trim(),
    components: t ? t.order.slice() : [],
  };
}

const shQuote = (s) => "'" + String(s).replace(/'/g, "'\\''") + "'";
const pyStr = (s) => '"' + String(s).replace(/\\/g, "\\\\").replace(/"/g, '\\"').replace(/\n/g, "\\n") + '"';

function buildCurl(cfg, endpoint) {
  const args = [`-X POST ${shQuote(endpoint)}`];
  args.push(`--data-urlencode ${shQuote("question=" + cfg.question)}`);
  if (cfg.language) args.push(`--data-urlencode ${shQuote("language=" + cfg.language)}`);
  if (cfg.additionaltriples) args.push(`--data-urlencode ${shQuote("additionaltriples=" + cfg.additionaltriples)}`);
  for (const c of cfg.components) args.push(`--data-urlencode ${shQuote("componentlist[]=" + c)}`);
  return "curl " + args.join(" \\\n     ");
}

function buildPython(cfg, endpoint) {
  const data = [`    ("question", ${pyStr(cfg.question)}),`];
  if (cfg.language) data.push(`    ("language", ${pyStr(cfg.language)}),`);
  if (cfg.additionaltriples) data.push(`    ("additionaltriples", ${pyStr(cfg.additionaltriples)}),`);
  for (const c of cfg.components) data.push(`    ("componentlist[]", ${pyStr(c)}),`);
  return [
    "import requests",
    "",
    `url = ${pyStr(endpoint)}`,
    "# form-encoded; componentlist[] repeats once per component, in pipeline order",
    "data = [",
    ...data,
    "]",
    "",
    "response = requests.post(url, data=data)",
    "response.raise_for_status()",
    "print(response.json())",
  ].join("\n");
}

function renderCodeModal() {
  const cfg = currentConfig();
  const endpoint = location.origin + "/startquestionansweringwithtextquestion";
  $("#code-empty").classList.toggle("hidden", !!(cfg.question && cfg.components.length));
  $("#code-output").textContent =
    codeLang === "python" ? buildPython(cfg, endpoint) : buildCurl(cfg, endpoint);
  $$("[data-code-lang]").forEach((b) => b.classList.toggle("active", b.dataset.codeLang === codeLang));
}

function openCodeModal() {
  saveActiveTabInputs();
  renderCodeModal();
  $("#code-modal").classList.remove("hidden");
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
// Modals (component info + saved configurations)
// ---------------------------------------------------------------------------
function closeModal(modal) {
  if (!modal) return;
  modal.classList.add("hidden");
  if (modal.id === "component-modal") $("#modal-iframe").removeAttribute("src");
}
document.addEventListener("click", (e) => {
  const c = e.target.closest("[data-modal-close]");
  if (!c) return;
  closeModal(c.closest(".modal"));
});
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape") $$(".modal:not(.hidden)").forEach(closeModal);
});

// ---------------------------------------------------------------------------
// Backend access
// ---------------------------------------------------------------------------
async function getJSON(url) {
  const r = await fetch(url, { headers: { Accept: "application/json" } });
  if (!r.ok) throw new Error(`${url} → HTTP ${r.status}`);
  return r.json();
}

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
  // drop selections that are no longer accessible / no longer registered (all tabs)
  const usable = new Set(list.filter((c) => c.accessible).map((c) => c.name));
  tabs.forEach((t) => { t.order = t.order.filter((n) => usable.has(n)); });
  renderComponents();
  renderOrder();
  const up = list.filter((c) => c.accessible).length;
  $("#pipeline-status").textContent = `${up}/${list.length} components available`;
  if (!$("#history-modal").classList.contains("hidden")) renderHistoryList();
}

function renderComponents() {
  const ul = $("#component-list");
  if (!state.components.length) {
    ul.innerHTML = `<li class="empty">no components registered with this pipeline</li>`;
    return;
  }
  const order = activeTab() ? activeTab().order : [];
  ul.innerHTML = "";
  for (const c of state.components) {
    const li = document.createElement("li");
    const selected = order.includes(c.name);
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
  const t = activeTab();
  if (!t) return;
  const i = t.order.indexOf(name);
  if (i >= 0) t.order.splice(i, 1);
  else t.order.push(name);
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

// ---------------------------------------------------------------------------
// Pipeline order (drag & drop)
// ---------------------------------------------------------------------------
function renderOrder() {
  const ol = $("#pipeline-order");
  const order = activeTab() ? activeTab().order : [];
  if (!order.length) {
    ol.innerHTML = `<li class="empty">no components selected yet</li>`;
    return;
  }
  ol.innerHTML = "";
  order.forEach((name, idx) => {
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
  const t = activeTab();
  if (t) t.order = [...$("#pipeline-order").querySelectorAll("li.item")].map((li) => li.dataset.name);
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
function setStatus(t, text, isError) {
  t.status = text;
  t.statusError = !!isError;
  renderRunStatus(t);
}
function setRunning(t, running) {
  t.running = running;
  if (activeTab() === t) $("#run").disabled = running;
  renderRunStatus(t);
}

// elapsed processing time — live while running, frozen afterwards
function elapsedOf(t) {
  if (t.running && t.runStartedAt != null) return performance.now() - t.runStartedAt;
  return t.runElapsedMs || 0;
}
function fmtElapsed(ms) {
  const s = (ms || 0) / 1000;
  if (s < 60) return s.toFixed(1) + " s";
  const m = Math.floor(s / 60);
  return `${m}:${(s % 60).toFixed(1).padStart(4, "0")} min`;
}

// a single low-frequency ticker drives the live timer of the active running tab
let runTimer = null;
function ensureRunTimer() { if (runTimer == null) runTimer = setInterval(tickRunTimer, 100); }
function tickRunTimer() {
  const t = activeTab();
  if (t && t.running) {
    const el = document.getElementById("run-timer");
    if (el) el.textContent = fmtElapsed(elapsedOf(t));
  }
  if (!tabs.some((x) => x.running) && runTimer != null) { clearInterval(runTimer); runTimer = null; }
}

// the run-status line: spinner + message + live timer while running;
// a frozen "completed in X" (or the error) once processing has finished.
function renderRunStatus(t) {
  if (activeTab() !== t) return;
  const el = $("#run-status");
  if (t.running) {
    el.className = "run-status running";
    el.innerHTML =
      `<span class="spinner" aria-hidden="true"></span>` +
      `<span class="run-msg">${esc(t.status || "Processing the question…")}</span>` +
      `<span class="run-timer" id="run-timer">${esc(fmtElapsed(elapsedOf(t)))}</span>`;
    return;
  }
  if (t.statusError) {
    el.className = "run-status error";
    const took = t.runElapsedMs != null ? ` <span class="run-timer">after ${esc(fmtElapsed(t.runElapsedMs))}</span>` : "";
    el.innerHTML = `<span class="run-msg">${esc(t.status)}</span>${took}`;
    return;
  }
  el.className = "run-status";
  if (!t.status && t.runElapsedMs != null) {
    el.innerHTML = `<span class="run-done">✓ completed in <strong>${esc(fmtElapsed(t.runElapsedMs))}</strong></span>`;
  } else {
    el.innerHTML = t.status ? `<span class="run-msg">${esc(t.status)}</span>` : "";
  }
}

// Detailed error panel — gives the user more than "HTTP 500" when the pipeline
// fails (parses the Spring error body: status/error/message/exception/path + raw).
function renderRunError(t) {
  if (activeTab() !== t) return;
  const card = $("#run-error-card");
  const el = $("#run-error");
  if (!t || !t.error) { card.classList.add("hidden"); el.innerHTML = ""; return; }
  const { status, statusText, body } = t.error;
  let parsed = null;
  try { parsed = JSON.parse(body); } catch (_) { /* body is not JSON */ }
  const fields = [];
  const add = (k, v) => { if (v != null && v !== "") fields.push([k, String(v)]); };
  add("HTTP status", [status, statusText].filter(Boolean).join(" "));
  if (parsed && typeof parsed === "object") {
    add("error", parsed.error);
    add("message", parsed.message);
    add("exception", parsed.exception);
    add("path", parsed.path);
    add("timestamp", parsed.timestamp);
  }
  const hint = Number(status) >= 500
    ? "The pipeline failed internally. Common causes: a component raised an error, the triplestore was not reachable, or an upstream service (e.g. the internet / a public SPARQL endpoint) was unavailable. The server's response is shown below."
    : Number(status) > 0
      ? "The pipeline rejected the request. The server's response is shown below."
      : "The request to the pipeline could not be completed (network error or the pipeline is unreachable).";
  const table = fields.length ? tableHTML(["field", "value"], fields, [false, true]) : "";
  const raw = body
    ? `<h3>Raw response</h3><div class="code-wrap">` +
      `<button class="copy-btn" data-copy-target="#run-error-raw" title="copy to clipboard">⧉ Copy</button>` +
      `<pre id="run-error-raw" class="code">${esc(body)}</pre></div>`
    : "";
  el.innerHTML = `<p class="hint">${esc(hint)}</p>${table}${raw}`;
  card.classList.remove("hidden");
}

async function run() {
  const t = activeTab();
  if (!t) return;
  const question = $("#question").value.trim();
  saveActiveTabInputs();
  setStatus(t, "");
  if (!question) { setStatus(t, "Please enter a question."); return; }
  if (!t.order.length) { setStatus(t, "Please select at least one component."); return; }

  rememberQuestion(question);
  const lang = $("#language").value.trim();
  const triples = $("#additionaltriples").value.trim();
  // store the used configuration in the browser database
  saveConfiguration({
    question, language: lang, additionaltriples: triples,
    components: t.order.slice(), savedAt: Date.now(),
  }).catch(() => { /* ignore storage errors */ });

  t.runStartedAt = performance.now();
  t.runElapsedMs = null;
  t.error = null;
  renderRunError(t);
  ensureRunTimer();
  setRunning(t, true);
  setStatus(t, "Processing the question… this can take a moment.");

  try {
    const body = new URLSearchParams();
    body.append("question", question);
    if (lang) body.append("language", lang);
    if (triples) body.append("additionaltriples", triples);
    for (const name of t.order) body.append("componentlist[]", name);

    const resp = await fetch("/startquestionansweringwithtextquestion", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded", Accept: "application/json" },
      body: body.toString(),
    });
    if (!resp.ok) {
      const body = await resp.text().catch(() => "");
      t.error = { status: resp.status, statusText: resp.statusText, body };
      throw new Error(`pipeline returned HTTP ${resp.status}`);
    }
    const result = await resp.json();
    const graph = result.outGraph || result.inGraph;
    if (!graph) throw new Error("no result graph returned by the pipeline");

    t.results = { graph, result };
    setStatus(t, "Reading the results from the triplestore…");
    if (activeTab() === t) await showResults(graph, result, { scroll: true });
    t.runElapsedMs = performance.now() - t.runStartedAt;
    setStatus(t, "");
  } catch (e) {
    t.results = null;
    if (t.runStartedAt != null) t.runElapsedMs = performance.now() - t.runStartedAt;
    setStatus(t, "Error: " + e.message, true);
  } finally {
    setRunning(t, false);
    renderRunError(t);
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
    if (c === "#") { let j = i; while (j < n && s[j] !== "\n") j++; tokens.push({ t: "comment", v: s.slice(i, j) }); i = j; continue; }
    if (c === "<") {
      const m = /^<[^<>"{}|^`\\\s]*>/.exec(s.slice(i));
      if (m) { tokens.push({ t: "iri", v: m[0] }); i += m[0].length; continue; }
      tokens.push({ t: "word", v: c }); i++; continue;
    }
    if (c === '"' || c === "'") {
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
    if (tk.t === "comment") { if (!atLineStart()) newline(); out += tk.v; newline(); prev = tk; continue; }
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
// Results: pipeline response, generated SPARQL, JSON answer table, facts
// ---------------------------------------------------------------------------
async function showResults(graph, result, opts) {
  const scroll = !opts || opts.scroll !== false;
  $("#results").classList.remove("hidden");
  showPipelineResponse(result);
  $("#graph-meta").textContent = "result graph: " + graph;
  if (scroll) $("#results").scrollIntoView({ behavior: "smooth", block: "start" });
  loadGraphIntoYasgui(graph);

  await Promise.all([
    showGeneratedSparql(graph),
    showAnswerTable(graph),
    showComponentFacts(graph),
  ]);
}

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

  const byComp = new Map();
  for (const r of rows) {
    const comp = shortUri(cell(r, "component")) || "(unknown)";
    if (!byComp.has(comp)) byComp.set(comp, []);
    byComp.get(comp).push(r);
  }
  el.innerHTML = "";
  for (const [comp, facts] of byComp) {
    const card = document.createElement("details");
    card.className = "comp-card";
    card.innerHTML =
      `<summary><span class="h4">${esc(comp)} ` +
      `<span class="count">${facts.length} annotation${facts.length === 1 ? "" : "s"}</span></span></summary>` +
      `<div class="facts-body">${facts.map(describeFact).join("")}</div>`;
    el.appendChild(card);
  }
}

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

function loadGraphIntoYasgui(graph) {
  setYasguiQuery(`# Everything stored for this question-answering run
SELECT ?subject ?predicate ?object FROM <${graph}> WHERE {
  ?subject ?predicate ?object
} LIMIT 200`);
}

// ---------------------------------------------------------------------------
// wire up + live refresh
// ---------------------------------------------------------------------------
function init() {
  initTheme();
  initYasgui();
  renderHistory();

  // start with one tab
  const t = newTab();
  activeTabId = t.id;
  renderTabs();
  loadTabIntoDom();

  $("#run").addEventListener("click", run);
  $("#refresh").addEventListener("click", loadComponents);
  $("#open-history").addEventListener("click", openHistory);
  $("#show-code").addEventListener("click", openCodeModal);
  $(".code-lang-switch").addEventListener("click", (e) => {
    const b = e.target.closest("[data-code-lang]");
    if (!b) return;
    codeLang = b.dataset.codeLang;
    renderCodeModal();
  });
  $("#tab-add").addEventListener("click", () => addTab());

  $("#tabs").addEventListener("click", (e) => {
    const tab = e.target.closest(".tab");
    if (!tab) return;
    const id = tab.dataset.tabId;
    if (e.target.closest(".tab-close")) { e.stopPropagation(); closeTab(id); return; }
    switchTab(id);
  });

  $("#question").addEventListener("input", () => {
    const tab = activeTab();
    if (!tab) return;
    tab.question = $("#question").value;
    const span = document.querySelector("#tabs .tab.active .tab-title");
    if (span) span.textContent = tabTitle(tab);
  });

  $("#history-list").addEventListener("click", async (e) => {
    const use = e.target.closest("[data-use]");
    const del = e.target.closest("[data-del]");
    if (use && !use.disabled) {
      const id = Number(use.dataset.use);
      const cfg = (await getAllConfigurations()).find((c) => c.id === id);
      if (cfg) useConfiguration(cfg);
    } else if (del) {
      await deleteConfiguration(Number(del.dataset.del));
      renderHistoryList();
    }
  });

  loadComponents();
  setInterval(loadComponents, 12000); // keep the component list up to date
}

init();
