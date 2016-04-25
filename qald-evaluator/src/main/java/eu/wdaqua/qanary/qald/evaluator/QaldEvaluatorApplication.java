package eu.wdaqua.qanary.qald.evaluator;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.syntax.Element;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import eu.wdaqua.qanary.qald.evaluator.qaldreader.FileReader;

/**
 * start the spring application
 * 
 * @author AnBo
 *
 */
//@SpringBootApplication
//@EnableAutoConfiguration
//@ComponentScan("eu.wdaqua.qanary.component")
public class QaldEvaluatorApplication {

	public static void main(String... args) throws UnsupportedEncodingException, IOException {
//		SpringApplication.run(QaldEvaluatorApplication.class, args);
//
//		FileReader filereader = new FileReader();
//
//		filereader.readFile();

		String sparql = "SELECT DISTINCT ?uri WHERE { <http://dbpedia.org/resource/Albert_Einstein> <http://dbpedia.org/ontology/doctoralAdvisor> ?uri . }";

		Query query = QueryFactory.create(sparql);

		Element pattern = query.getQueryPattern();
	}
}
