
# Build and Run the Qanary Pipeline

<img align="right"  width="300" src="https://github.com/WDAqua/Qanary/blob/master/doc/logo-qanary-pixelart-black-background.png?raw=true">

[The Qanary wiki pages](https://github.com/WDAqua/Qanary/wiki/What-is-Qanary%3F) should answer all questions that might come up when developing a question answering system using Qanary.

These pages in particular are relevant for configuring, building, and starting a pipeline:

* For a tutorial on how to build and start a simple pipeline, please refer to [this wiki page](https://github.com/WDAqua/Qanary/wiki/Qanary-tutorial:-How-to-build-a-trivial-Question-Answering-pipeline).
* For questions about the configuration of a pipeline, please refer to [this wiki page](https://github.com/WDAqua/Qanary/wiki/Configuration-Parameters-of-a-Qanary-Pipeline).
* For anything else, please refer to [the FAQ page in the wiki](https://github.com/WDAqua/Qanary/wiki/Frequently-Asked-Questions).

## Build the Qanary Pipeline

Requirement: The [`qanary-commons` package](https://github.com/WDAqua/Qanary/tree/master/qanary_commons) is installed on your system.

To build an executable JAR file and the corresponding Docker image, run the command:

```shell
mvn package
```

The created JAR file is located in the automatically created directory `target`.

## Execute the Qanary Pipeline JAR file

To execute the JAR file with Java until version 16, run the following command:

```shell
java -jar --illegal-access=permit target/qa.pipeline-X.Y.Z.jar
```

From Java 17 on, you need to add the following options to the command line to allow the Qanary pipeline to access the Java internals.

```shell
java --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED -jar target/qa.pipeline-X.Y.Z.jar
```

### Remark

While running an initial Qanary pipeline, you need to configure the Qanary pipeline to use the correct endpoint of the Qanary triplestore. Please see [this description](https://github.com/WDAqua/Qanary/blob/master/qanary_commons/src/main/java/eu/wdaqua/qanary/commons/triplestoreconnectors/README.adoc).

While starting the Qanary pipeline, you can recognize the correct configuration by the following log message:

```shell
Triplestore is accessible and returns triples.
```

Otherwise, a warning will be displayed that the Qanary pipeline cannot access the configured Qanary triplestore, i.e., the knowledge base where process information (all state information about each given task) is stored. 
The warning is shown three times.
If the triplestore is still not accessible, the Qanary pipeline will stop, and the Java application will consequently terminate.

## Web frontend (`/qa`)

A running pipeline serves a dependency-free web frontend at
[`http://localhost:40111/qa`](http://localhost:40111/qa) (adjust host/port to your
configuration) for using and inspecting the pipeline from a browser — no separate
build or deployment step. Its sources live in
`src/main/resources/static/qanary-ui/` (`index.html`, `app.js`, `styles.css`).

It lets you:

* enter a question and **configure the pipeline** by dragging the live, registered
  components into their processing order (offline components are shown but not
  selectable; the ⓘ button shows a component's host/port/URLs and service page);
* work on several questions at once in independent **question tabs**;
* **process** the question and inspect the generated SPARQL query, the JSON answer
  rendered as a copyable **table**, and a plain-language summary of what each
  component did (read directly from the triplestore);
* get a ready-to-run **cURL / Python snippet** ("Run from code") for the current
  configuration, and reopen earlier runs from **saved configurations** (stored in
  the browser);
* query the triplestore yourself with the embedded **YASGUI** SPARQL editor;
* switch between **light, dark and high-contrast themes**; on a pipeline error
  (e.g. HTTP 500) an error card explains the cause.

A **scroll-to-top** button (lower-left) and a **"Fork me on GitHub"** ribbon
(lower-right) are always available.

The frontend has an end-to-end test with step-by-step screenshots; see
[`frontend-e2e/`](https://github.com/WDAqua/Qanary/tree/master/frontend-e2e).

## Starting a question answering process via JSON

A process is started with `POST /startquestionansweringwithtextquestion` (or `/startquestionansweringwithtextquestionThroughJson`, which is the same endpoint for Swagger UI) and a JSON body. Only `question` is required. The `componentlist` defines which components are called, and in which order:

```json
{
  "question": "Who invented the Hawaiian pizza?",
  "componentlist": ["NED-DBpediaSpotlight", "QueryBuilder", "SparqlExecuter"]
}
```

The components of the `componentlist` are called one after another. An element of the `componentlist` can also be a *parallel block*: an object whose properties are named streams. Each stream is a list like the `componentlist` itself. All streams of a parallel block run at the same time; the components within a stream run one after another; the element after the parallel block starts when all its streams have finished.

```json
"componentlist": ["a", { "p1": ["b"], "x": ["c", "d"] }, "e"]
```

This calls `a`, then `b` in parallel to `c` and `d` (`d` starts after `c` has finished), and `e` after `b` and `d` have finished. A stream can contain parallel blocks itself.

The stream names (`p1`, `x`) are chosen freely. They identify the streams if anything goes wrong, so they have to be unique within the `componentlist`:

* If a component of a stream fails, the stream stops; the other streams of the parallel block are completed, then the process stops with HTTP 500. `GET /questionanswering/errors?questionuri=<question URI>` lists each failed stream with its `stream` name and the failed `component`.
* A component that is not available is skipped, as in a list without parallel blocks; its error entry names the `stream` as well.
* An invalid `componentlist` (e.g., an empty stream, a stream name used twice, a number instead of a component name) is rejected with HTTP 400 before anything is executed; the [problem details](https://www.rfc-editor.org/rfc/rfc9457) of the response explain what is wrong.

Every stream starts with the same Qanary message (the graph of the process), so all components write to the same graph. Components that run in parallel must not depend on each other's annotations; put them into the same stream if they do.
