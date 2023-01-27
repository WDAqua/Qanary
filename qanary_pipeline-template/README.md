# Build and Run the Qanary Pipleine

[The Qanary wiki pages](https://github.com/WDAqua/Qanary/wiki/What-is-Qanary%3F) should answer all questions that might come up when developing a question answering system using Qanary.

These pages in particular are relevant for configuring, building and starting a pipeline:

* For a tutorial on how to build and start a simple pipeline, please refer to [this wiki page](https://github.com/WDAqua/Qanary/wiki/Qanary-tutorial:-How-to-build-a-trivial-Question-Answering-pipeline).
* For questions about the configuration of a pipeline, please refer to [this wiki page](https://github.com/WDAqua/Qanary/wiki/Configuration-Parameters-of-a-Qanary-Pipeline).
* For anything else, please refer to [the FAQ page in the wiki](https://github.com/WDAqua/Qanary/wiki/Frequently-Asked-Questions).

## Build the Qanary Pipeline

Requirement: The [`qanary-commons` package](https://github.com/WDAqua/Qanary/tree/master/qanary_commons) is installed on your system.

To build an executable JAR file and a corresponding Docker image run:

```shell
mvn package
```

The created JAR file is located in the automatically created directory `target`.

## Execute the Qanary Pipeline JAR file

To execute the JAR file with Java until version 16 run the following command:

```shell
java -jar --illegal-access=permit target/qa.pipeline-X.Y.Z.jar
```

From Java 17 on, you need to add the following options to the command line to allow the Qanary pipeline to access the Java internals.

```shell
java --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED -jar target/qa.pipeline-X.Y.Z.jar
```

### Remark

While running an initial Qanary pipeline, you need to configure the Qanary pipeline to use the correct endpoint of the Qanary triplestore. Please see [this description](https://github.com/WDAqua/Qanary/blob/master/qanary_commons/src/main/java/eu/wdaqua/qanary/commons/triplestoreconnectors/README.adoc).

While starting the Qanary pipeline you can recoginize the correct configuration by the following log message:

```shell
Triplestore is accessible and returns triples.
```