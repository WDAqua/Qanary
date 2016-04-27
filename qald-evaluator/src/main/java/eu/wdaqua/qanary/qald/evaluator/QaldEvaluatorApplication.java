package eu.wdaqua.qanary.qald.evaluator;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import eu.wdaqua.qanary.qald.evaluator.qaldreader.FileReader;

/**
 * start the spring application
 * 
 * @author AnBo
 *
 */
// @SpringBootApplication
// @EnableAutoConfiguration
// @ComponentScan("eu.wdaqua.qanary.component")
public class QaldEvaluatorApplication {

	public static void main(String... args) throws UnsupportedEncodingException, IOException {
		// SpringApplication.run(QaldEvaluatorApplication.class, args);
		//
		FileReader filereader = new FileReader();

		filereader.getQuestion(1).getUris();

		// send to pipeline
		System.out.println(filereader.getQuestion(1).getSparqlQuery());

		// retrieve recognized URIs from configured pipeline

		// compare to provided uris

		// String sparql = "SELECT DISTINCT ?uri WHERE {
		// <http://dbpedia.org/resource/Albert_Einstein>
		// <http://dbpedia.org/ontology/doctoralAdvisor> ?uri . }";
		// Query query = QueryFactory.create(sparql);

	}
}
