![](https://raw.githubusercontent.com/WDAqua/Qanary/master/doc/logo-qanary_s.png)

# A Reference Implementation for Creating Question Answering Systems following the Qanary Methodology
## Qanary in a Nutshell

Qanary is a Methodology for Creating Question Answering Systems it is part of the [WDAqua project](http://wdaqua.informatik.uni-bonn.de) where question answering systems are researched and developed. Here, we are providing our key contributions on-top of the [RDF vocabulary qa](https://github.com/WDAqua/QAOntology) the reference implementation of the Qanary methodology. This repository contributes several sub-resources for Question Answring Community to build knowledge driven QA systems incorporating a standard [RDF vocabulary qa](https://github.com/WDAqua/QAOntology). All the resources are reusable. For detailed description of individual resources, kindly refer to Wiki section of this repository. In brief, the following sub-projects are available all aiming at establishing an ecosystem for question answering systems.

 * [**Qanary Pipeline**](#qanarypipeline) implementation: a central component where components for question answering systems are connected automatically and can be called by Web UIs
 * Qanary component implementations: components providing wrappers to existing functionality or implement new question answering approaches
   * a [**Qanary component template**](#qanarycomponenttemplate) implementation: use this to build you own component ([howto]()) as it provides [several features]()
   * [**Qanary AGDISTIS**](#qanaryagdists): a wrapper for disambiguating named entity in text using the [AGDISTIS](http://aksw.org/Projects/AGDISTIS.html) (NED) tool
   * [**Qanary Alchemy**](#qanaryalchemy): a wrapper for the [Alchemy Entity Extraction API](http://www.alchemyapi.com/products/alchemylanguage/entity-extraction) (commercial, but offers a free API key) computing named entities and disambiguates them (NER+NED)
   * [**Qanary DBpedia Spotlight NER**](#qanarydbpediaspotlight): a wrapper for the service interface of [DBpedia Spotlight](https://github.com/dbpedia-spotlight/dbpedia-spotlight) spotting named entities within text (NER)
   * [**Qanary DBpedia Spotlight NED**](#qanarydbpediaspotlight): a wrapper for the service interface of [DBpedia Spotlight](https://github.com/dbpedia-spotlight/dbpedia-spotlight) disambiguating named entities within text (NED) to [DBpedia](http://dbpedia.org) resources
   * [**Qanary FOX**](#qanaryfox): a wrapper for the [FOX](http://aksw.org/Projects/FOX.html) tool for recognizing named entities within text (NER)
   * [**Qanary Lucene Linker**](#qanarylucenelinker): a tool for recognizing and disambiguating named entities within text derived from an implementation within the [SINA](http://aksw.org/Projects/SINA.html) question answering system (NER+NED)
   * [**Qanary Stanford NER**](#qanarystanfordner): a wrapper for the [Stanford Named Entity Recognizer](http://nlp.stanford.edu/ner/) recognizing named entities within text
 * Qanary benchmarking
   * [**QALD evaluator**](#qaldevaluator): a client for the Qanary Pipeline evaluating the capabilities w.r.t. named entity recognition and disambiguation of a given Qanary Pipeline configuration with the [QALD benchmark](http://qald.sebastianwalter.org/) (Question Answering over Linked Data) data
   * [**QALD annotated with named entities**](#qaldnerddataset): questions of [QALD](http://qald.sebastianwalter.org/) annotated with named entities containing

<a name="qanarypipeline"></a>
## Qanary Pipeline

[source](https://github.com/WDAqua/Qanary/tree/master/qanary_pipeline-template)

More details follow soon.


<a name="qanarycomponents"></a>
## Qanary Components


<a name="qanarycomponenttemplate"></a>
### Qanary component template
[source]()

More details follow soon.


<a name="qanaryagdists"></a>
### Qanary AGDISTIS
AGDISTIS is a NED tool that uses the graph-structure of an ontology to disambiguate the entities. It starts with a spotted text and it tries to link the spots to resources in the ontology. The idea behind the algorithm is to take, the candidates which are more connected in G. This can be applied to any ontology making this approach ontology-independent. Moreover it is language independent. As far as we know it was never uses by any QA system.
[source](https://github.com/WDAqua/Qanary/tree/master/qanary_component-AGDISTIS-NED)


<a name="qanaryalchemy"></a>
### Qanary Alchemy
Alchemy API is a private company owned by IBM that offers as a web service several tools. Among others it offers an entity linking service to DBpedia, Yago and Freebase. As far as we know it was never uses by any QA system.
[source](https://github.com/WDAqua/Qanary/tree/master/qanary_component-Alchemy-NERD)


<a name="qanarydbpediaspotlight"></a>
### Qanary DBpedia Spotlight
DBpedia Spotlight is a tool that can be used both as a spotter and as a \NED tool. We consider it here as two separate tools.

####DBpedia Spotlight Spotter
The spotter of DBpedia Spotlight uses lexicalizations, i.e. ways to expresse NE, that are available directly in DBpedia or in Wikipedia. These includes the RDFs labels, the redirect information (i.e. dbr:America_(USA) is redirected to dbr:United_States saying that the entity *United States* can also be expressed as "America"), the disambiguation links (i.e. USA can refer to dbr:United_States but also to dbr:University_of_South_Alabama) and the anchor texts in Wikipedia. The Spotter selects the part of a text in a question that correspond to one lexicalization and that are ranked as the most important one.

####DBpedia Spotlight Disambiguator
The \NED part of DBpedia Spotlight disambiguates the entities by using statistics extracted from the Wikipedia texts. The decision is made by  combining the following features: how often does an entity appear in the text, how probable is the lexical form of the entity in the question (i.e. how often is dbr:United_States expressed as USA) and how often does an entity appear together with the other entities.

DBpedia Spotlight can be use only for the DBpedia ontology and works for several languages.
 * Qanary DBpedia Spotlight NER: [source](https://github.com/WDAqua/Qanary/tree/master/qanary_component-DBpedia-Spotlight-NER)
 * Qanary DBpedia Spotlight NED: [source](https://github.com/WDAqua/Qanary/tree/master/qanary_component-DBpedia-Spotlight-NED)

<a name="qanaryfox"></a>
### Qanary FOX
FOX is a Named Entity Recognition Tool that integrates four different NER tools, namely: the Standford Named Entity Recognition Tool, the Illinois Named Entity Tagger (Illinois), the Ottawa Baseline Information Extraction (Balie) and the Apache OpenNLP Name Finder (OpenNLP). The combination is done using ensamble learning, \ie the tags generated by the four taggers are combined using a machine learning algorithm. It is clear that this tool can be used in the same cases as the Stanford NER tool. As far as we know it was never uses by any QA system.
[source](https://github.com/WDAqua/Qanary/tree/master/qanary_component-FOX-NER)

<a name="qanarylucenelinker"></a>
### Qanary Lucene Linker
We implemented a component following the idea of the QA system SINA which is based on information retrieval methods.
[source](https://github.com/WDAqua/Qanary/tree/master/qanary_component-Lucene-Linker-NERD)


<a name="qanarystanfordner"></a>
### Qanary Stanford NER
The Stanford Named Entity Recognition Tool is a popular tool from NLP. It uses a machine learning algorithm based on Conditional Random Fields to spot Named Entities in a text. The decision to tag a word as named entity or not is based mainly on syntactic features like: the POS tag of the word and of the surrounding words, n-gram sequences of characters of the word (to detect for example particular endings) and the shape of the word (to detect for example capital letters). This tool can be potentially used to spot entities for any ontology but can be used only for the languages where a model is available (currently English, German, Spanish and Chinese).
[source](https://github.com/WDAqua/Qanary/tree/master/qanary_component-stanford-NER)

## Additional Resources


<a name="qaldevaluator"></a>
### QALD evaluator
[source](https://github.com/WDAqua/Qanary/tree/master/qald-evaluator)

More details follow soon.


<a name="qaldnerddataset"></a>
### QALD annotated with named entities
[source](https://github.com/WDAqua/Qanary/tree/master/ISWC-results)

More details follow soon.

## Publications / References

If you want to inform yourself about the Qanary methodology in general, please use this publication:  *Andreas Both, Dennis Diefenbach, Kuldeep Signh, Saedeeh Shekarpour, Didier Cherix and Christoph Lange: Qanary - A Methodology for Vocabulary-driven Open Question Answering Systems* appearing in [13th Extended Semantic Web Conference](http://2016.eswc-conferences.org), 2016.


## Stuff used to make this:

 * [Spring Boot](http://projects.spring.io/spring-boot/) project

## How to run the code

### Without docker

 * Clone the GitHub repository: `git clone https://github.com/WDAqua/Qanary`

 * Install Java 8 (see <http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html> for details)

 * Install maven (see <https://maven.apache.org/install.html> for details)

 * Compile and package your project using maven: `mvn clean install -DskipDockerBuild`
   The _install_ goal will compile, test, and package your project’s code and then copy it into the local dependency repository.

 * Install Stardog Triplestore (<http://stardog.com/>) and start it in background. Create a database with the name _qanary_. All the triples generated by the components will be stored in the _qanary_ database.

 * Run the pipeline component:
   ```
   cd qanary_pipeline-template/target/
   java -jar target/qa.pipeline-<version>.jar
   ```
 * After `maven build` jar files will be generated in the corresponding folders of the Qanary components. For example, to start the Alchemy API components:
   ```
   cd qanary_component-Alchemy-NERD
   java -jar target/qa.Alchemy-NERD-0.1.0.jar
   ```
 
 * After running corresponding jar files, you can see Springboot application running on <http://localhost:8080/#/overview> that will tell the status of currently running components.

 * Now your pipeline is ready to use. Go to <http://localhost:8080/startquestionansweringwithtextquestion>. Here you can find a User Interface to interact for adding question via web interface, and then select the components you need to include in the pipeline via checking a checkbox for each component. Press the start button and you are ready to go!

### With docker

 * Clone the GitHub repository: `git clone https://github.com/WDAqua/Qanary`

 * Install Java 8 (see <http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html> for details)

 * Install maven (see <https://maven.apache.org/install.html> for details)
 
 * Install docker (see <https://docs.docker.com/engine/installation/> for details)
 
 * Start docker service (see <https://docs.docker.com/engine/admin/> for details)

 * Compile and package your project using maven: `mvn clean install`
   The _install_ goal will compile, test, and package your project’s code and then copy it into the local dependency repository. Additionally, it will generate docker images for each component that will be stored in your local repository.

 * Configure the script `start.sh` according to the services you want to start. Each service runs inside a docker instance. At least the docker containers `stardog`, `pipeline` and one qanary component have to be up and running.
 Afterwards, run the script `initdb.sh` that creates the database _qanary_ in the stardog triple store.
 
 * After executing the run script, you can see Springboot application running on <http://localhost:8080/#/overview> that will tell the status of currently running components.

 * Now your pipeline is ready to use. Go to <http://localhost:8080/startquestionansweringwithtextquestion>. Here you can find a User Interface to interact for adding question via web interface, and then select the components you need to include in the pipeline via checking a checkbox for each component. Press the start button and you are ready to go!
