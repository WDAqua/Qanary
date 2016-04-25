package eu.wdaqua.qanary.qald.evaluator.qaldreader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementAssign;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementDataset;
import org.apache.jena.sparql.syntax.ElementExists;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementMinus;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementNotExists;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementService;
import org.apache.jena.sparql.syntax.ElementSubQuery;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.apache.jena.sparql.syntax.ElementVisitor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class FileReader {

	public void readFile() throws UnsupportedEncodingException, IOException {
		Reader reader = new InputStreamReader(FileReader.class.getResourceAsStream("/qald-6-test-multilingual.json"),
				"UTF-8");
		Gson gson = new GsonBuilder().create();
		JsonObject json = gson.fromJson(reader, JsonObject.class);

		JsonArray questions = json.get("questions").getAsJsonArray();
		// System.out.println(questions);
		System.out.println("size: " + questions.size());

		JsonObject question;
		JsonArray questiondata;
		String language;
		String questionstring;
		JsonObject query;
		String sparqlquery;


		for (int i = 0; i < questions.size(); i++) {
			question = questions.get(i).getAsJsonObject();
			// System.out.println(question.getAsJsonArray("question"));

			questiondata = question.getAsJsonArray("question");

			for (int j = 0; j < questiondata.size(); j++) {
				language = questiondata.get(j).getAsJsonObject().get("language").getAsString();
				if (language.compareTo("en") == 0) {
					URIDetector uriDetector = new URIDetector();
					questionstring = questiondata.get(j).getAsJsonObject().get("string").getAsString();
					query = question.get("query").getAsJsonObject();
					if (query.isJsonObject() && !query.isJsonNull() && query.has("sparql")) {
						sparqlquery = question.get("query").getAsJsonObject().get("sparql").getAsString();

						Query myquery = QueryFactory.create(sparqlquery);
						if (myquery.isSelectType() && myquery.isQueryResultStar()) { // of
																						// the
																						// form
																						// SELECT
																						// *?
							myquery.getDatasetDescription(); // FROM / FROM
																// NAMED
																// bits
							Op op = Algebra.compile(myquery); // Get the algebra
																// for the query

							Element querypattern = myquery.getQueryPattern(); // The
																				// meat
																				// of
																				// the
																				// query,

							// the WHERE bit
							querypattern.visit(uriDetector);

						}
					} else {
						sparqlquery = null;
					}

					// TODO: expected output: List of subjects, List of
					// predicate, List of objects u
					uriDetector.getSubjects();
					uriDetector.getPredicates();
					uriDetector.getObjects();
					System.out
							.println(question.get("id").getAsInt() + ": " + questionstring + " SPARQL: " + sparqlquery);
					break;
				}
			}

		}

	}
}
