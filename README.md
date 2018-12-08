![](https://raw.githubusercontent.com/WDAqua/Qanary/master/doc/logo-qanary_s.png)

# A Reference Implementation for Creating Question Answering Systems following the Qanary Methodology
## Qanary in a Nutshell

Qanary is a methodology for creating Question Answering Systems it is part of the [WDAqua project](http://wdaqua.eu) where question answering systems are researched and developed. For all the publications related to Qanary please see the section [publications](#qanarypublications). W.r.t. questions, ideas or any feedback related to Qanary please do not hesitate to [contact the core developers](https://github.com/WDAqua/Qanary/wiki/Who-do-I-talk-to%3F). However, if you like to see a QA system built using the Qanary framework, one of our core developers has build a complete end-to-end QA system which allows to query several RDF data stores: http://wdaqua.eu/qa.

Please go to the [GitHub Wiki page](https://github.com/WDAqua/Qanary/wiki) of this repository to get more insights on how to use this framework, how to add new component etc.

Here, we provide our key contributions on-top of the [RDF vocabulary qa](https://github.com/WDAqua/QAOntology): the reference implementation of the Qanary methodology. This repository contributes several sub-resources for the Question Answering Community to build knowledge-driven QA systems incorporating a standard [RDF vocabulary named "qa"](https://github.com/WDAqua/QAOntology). 

All the resources are reusable. For detailed description of individual resources, kindly refer to Wiki section of this repository. The [Qanary Question Answering components](https://github.com/WDAqua/Qanary-question-answering-components) maintained by the core developers are available [here](https://github.com/WDAqua/Qanary-question-answering-components) (in a separated Git repository). They require to first clone this repository (of the Qanary framework) and execute ``mvn install``.
However, this is just the beginning, many more components will soon be published.

The following sub-project are part of the Qanary core frameworks:

 * [**Qanary Pipeline**](#qanarypipeline) implementation: a central component where components for question answering systems are connected automatically and can be called by Web UIs
 * Qanary component implementations: components providing wrappers to existing functionality or implement new question answering approaches
    * a [**Qanary component template**](#qanarycomponenttemplate) implementation: use this to build you own component ([howto]()) as it provides [several features]()
 * the additional resource [**QALD evaluator**](#qaldevaluator): a client for the Qanary Pipeline evaluating the capabilities w.r.t. named entity recognition and disambiguation of a given Qanary Pipeline configuration with the [QALD benchmark](http://qald.sebastianwalter.org/) (Question Answering over Linked Data) data
    * [**QALD annotated with named entities**](#qaldnerddataset): questions of [QALD](http://qald.sebastianwalter.org/) annotated with named entities containing

<a name="qanarypipeline"></a>
## Qanary Pipeline

[source](https://github.com/WDAqua/Qanary/tree/master/qanary_pipeline-template)

More details follow soon.


<a name="qanarycomponenttemplate"></a>
## Qanary component template
[source](https://github.com/WDAqua/Qanary/tree/master/qanary_component-template)

More details follow soon.



## Additional Resource

<a name="qaldevaluator"></a>
### QALD evaluator
[source](https://github.com/WDAqua/Qanary/tree/master/qald-evaluator)

More details follow soon.


<a name="qaldnerddataset"></a>
### QALD annotated with named entities
[source](https://github.com/WDAqua/Qanary/tree/master/ISWC-results)

More details follow soon.

<a name="qanarypublications"></a>
## Publications / References

If you want to inform yourself about the Qanary methodology in general, please use this publication:  *Andreas Both, Dennis Diefenbach, Kuldeep Singh, Saedeeh Shekarpour, Didier Cherix and Christoph Lange: Qanary - A Methodology for Vocabulary-driven Open Question Answering Systems* appearing in [13th Extended Semantic Web Conference](http://2016.eswc-conferences.org), 2016.

For additional publication we suggest to [follow this list of publications](https://scholar.google.de/scholar?q=%22qanary%22+question+%22answering%22)


## Stuff used to make this:

 * [Spring Boot](http://projects.spring.io/spring-boot/) project

## How to run the code

### Without docker

 * Clone the GitHub repository: `git clone https://github.com/WDAqua/Qanary`

 * Install Java 8 (see <http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html> for details)

 * Install maven (see <https://maven.apache.org/install.html> for details)

 * Compile and package your project using maven: `mvn clean install -DskipDockerBuild`
   The _install_ goal will compile, test, and package your project’s code and then copy it into the local dependency repository.

 * Install Stardog Triplestore (<http://stardog.com/>) and start it in background. Create a database with the name _qanary_. All the triples generated by the components will be stored in the _qanary_ database. We use version 4.1.3 of Stardog.

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
 
 ### Using Qanary for your work
 
 * Our Wiki page contains all the information about how to integrate a new component and also about the easy usability of Qanary framework. We have illustrated inclusion of a new component with example in Wiki page. Please refer to it.
