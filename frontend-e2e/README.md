# Qanary frontend — end-to-end test

Drives the **embedded Qanary frontend** (`/qa`) with the example question of
`Qanary_minimal_Python_example` ("Who is the inventor of the Hawaiian Pizza?"),
runs the pipeline through the UI, and asserts that the **generated SPARQL query**
is retrieved and displayed. A screenshot of every step is captured to document
the frontend process.

## Prerequisites

1. A running Qanary pipeline that serves the frontend, e.g. start the minimal
   example:
   ```bash
   cd ../../Qanary_minimal_Python_example   # or wherever the example lives
   docker compose up -d
   ```
   Wait until the three components are registered (the frontend's status pill
   shows `3/3 components available`).
2. A local Chrome/Chromium browser.
3. Node.js 18+.

## Run

```bash
npm install
npm test
```

Configuration via environment variables:

| Variable      | Default                     | Meaning                                          |
|---------------|-----------------------------|--------------------------------------------------|
| `BASE_URL`    | `http://localhost:40111`    | pipeline base URL                                |
| `CHROME_PATH` | `/usr/bin/google-chrome`    | Chrome/Chromium executable                       |
| `OUT`         | `./screenshots`             | screenshot output directory                      |
| `RUN_TIMEOUT` | `180000`                    | ms to wait for the pipeline run to finish        |
| `COMPONENTS`  | *(all reachable)*           | comma-separated component names to select        |

The process exits `0` when a SPARQL query was retrieved and looks valid, and
non-zero otherwise (the failing screenshot is saved too).

### Deterministic runs and the Wikidata rate limit

The **generated SPARQL query** is produced by the query builder (`QB-Wikidata`)
from the entity found by `NEL-WikidataLookup`; it does **not** require the query
executer. `QE-SparqlExecuter` only *runs* that query against the public Wikidata
endpoint (`query.wikidata.org`), which is aggressively rate-limited (HTTP 429)
and, on a 429, can block the synchronous pipeline for a long time.

For a deterministic, repeatable test of "retrieve the generated SPARQL query",
pin the query-generating components:

```bash
COMPONENTS=NEL-WikidataLookup,QB-Wikidata npm test
```

With all components selected (the default), the JSON **answer table** is also
populated — but only when Wikidata serves `QE-SparqlExecuter` (i.e. when it is
not throttling this host).

## What the test asserts

Besides retrieving the generated SPARQL query, `npm test` also asserts:

- the **pipeline response** block contains the integration fields `endpoint`,
  `inGraph`, `outGraph`, `question`;
- a **second tab** can be created and starts with an empty question;
- the processed run is **stored as a configuration** in the browser database and
  appears (usable) in the saved-configurations overlay;
- **reusing** that configuration opens a new tab with the question and ordered
  components restored.

## Documented steps

`npm test` captures, in order:

1. `frontend-loaded` – the UI with the live component list ("System configuration")
2. `question-entered` – the example question typed into the input field
3. `components-selected` – components selected and ordered (pipeline-order column)
4. `pipeline-running` – the run in progress
5. `result-sparql` – the generated SPARQL query (pretty-printed), with the
   collapsed "what each component did" boxes
6. `answer-table` – the backend JSON answer rendered as a table (with copy buttons)
7. `yasgui-editor` – the embedded YASGUI SPARQL editor
8. `pipeline-response` – the pipeline's JSON response (4 integration fields)
9. `multiple-tabs` – a second, independent question tab
10. `saved-configurations` – the saved-configurations overlay (IndexedDB)
11. `reused-configuration` – a configuration reopened in a new tab
12. `theme-dark` – dark theme
13. `theme-contrast` – high-contrast theme

### Feature screenshots

`node capture-feature-shots.js` documents the features the pass/fail test does
not exercise on its own:

14. `component-info-modal` – the ⓘ overlay (host/IP, port, service URL + iframe of
    the component's own service page)
15. `component-facts-expanded` – a collapsed/expanded component box showing the
    pretty-printed `AnnotationOfAnswerSPARQL`

It also re-renders `06-answer-table.png` populated with the real Wikidata answer
through the frontend's own `jsonToTable()` (because a live answer needs
`QE-SparqlExecuter`, which depends on the rate-limited Wikidata endpoint).
