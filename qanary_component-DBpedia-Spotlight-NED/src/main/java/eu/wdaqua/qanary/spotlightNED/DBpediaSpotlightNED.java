package eu.wdaqua.qanary.spotlightNED;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;

/**
 * represents a wrapper of the DBpedia Spotlight as NED
 * 
 * @author Kuldeep Singh
 *
 */

@Component
public class DBpediaSpotlightNED extends QanaryComponent {
	// private String agdistisService="http://139.18.2.164:8080/AGDISTIS";
	private static final Logger logger = LoggerFactory.getLogger(DBpediaSpotlightNED.class);

	/**
	 * default processor of a QanaryMessage
	 */

	public String runCurl1(String question) {

		String xmlResp = "";
		try {
			URL url = new URL("http://spotlight.sztaki.hu:2222/rest/disambiguate/");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			// String temp = "text=<?xml version=\"1.0\"
			// encoding=\"UTF-8\"?><annotation
			// text=\""+question+"\"><surfaceForm name=\"published\"
			// offset=\"23\" /><surfaceForm name=\"Heart\" offset=\"63\"
			// /></annotation>";
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());

			wr.write(("text=" + question).getBytes("UTF-8"));
			wr.flush();
			wr.close();

			// InputStreamReader ir = new
			// InputStreamReader(connection.getInputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			xmlResp = response.toString();
			logger.info("Response spotlight service {}", xmlResp);
		} catch (Exception e) {
		}
		return (xmlResp);

	}

	public String getXmlFromQuestion(String question, ArrayList<Link> offsets) {
		String xmlFileContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><annotation text=\"" + question + "\">";

		for (Link sel : offsets) {
			int begin = sel.begin;
			int end = sel.end;
			String surNam = question.substring(begin, end);
			xmlFileContent += "<surfaceForm name=\"" + surNam + "\" offset=\"" + begin + "\"/>";
		}
		xmlFileContent += "</annotation>";

		return xmlFileContent;
	}

	public QanaryMessage process(QanaryMessage QanaryMessage) {
		logger.info("process: {}", QanaryMessage);

		try {
			long startTime = System.currentTimeMillis();
			logger.info("process: {}", QanaryMessage);
			// STEP1: Retrieve the named graph and the endpoint
			String endpoint = QanaryMessage.getEndpoint().toASCIIString();
			String namedGraph = QanaryMessage.getInGraph().toASCIIString();
			logger.info("Endpoint: {}", endpoint);
			logger.info("InGraph: {}", namedGraph);

			// STEP2: Retrieve information that are needed for the computations
			String sparql = "PREFIX qa:<http://www.wdaqua.eu/qa#> " + "SELECT ?questionuri " + "FROM <" + namedGraph
					+ "> " + "WHERE {?questionuri a qa:Question}";
			ResultSet result = selectTripleStore(sparql, endpoint);
			String uriQuestion = result.next().getResource("questionuri").toString();
			logger.info("Uri of the question: {}", uriQuestion);
			// Retrieve the question itself
			RestTemplate restTemplate = new RestTemplate();
			// TODO: pay attention to "/raw" maybe change that
			ResponseEntity<String> responseEntity = restTemplate.getForEntity(uriQuestion + "/raw", String.class);
			String question = responseEntity.getBody();
			logger.info("Question: {}", question);
			// Retrieves the spots from the knowledge graph
			sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
					+ "SELECT ?start ?end " + "FROM <" + namedGraph + "> " //
					+ "WHERE { " //
					+ "    ?a a qa:AnnotationOfSpotInstance . " + "?a oa:hasTarget [ "
					+ "		     a               oa:SpecificResource; " //
					+ "		     oa:hasSource    ?q; " //
					+ "	         oa:hasSelector  [ " //
					+ "			         a        oa:TextPositionSelector ; " //
					+ "			         oa:start ?start ; " //
					+ "			         oa:end   ?end " //
					+ "		     ] " //
					+ "    ] ; " //
					+ "    oa:annotatedBy ?annotator " //
					+ "} " //
					+ "ORDER BY ?start ";

			ResultSet r = selectTripleStore(sparql, endpoint);
			ArrayList<Link> links = new ArrayList<Link>();
			while (r.hasNext()) {
				QuerySolution s = r.next();
				Link link = new Link();
				link.begin = s.getLiteral("start").getInt();
				link.end = s.getLiteral("end").getInt();
				links.add(link);
			}

			// STEP3: Call the DBpedia NED service

			// it will create XML content, which needs to be input in DBpedia
			// NED with curl command
			String content = getXmlFromQuestion(question, links);

			// storing the output of DBpediaNED, which is a JSON
			String response = runCurl1(content);

			// Now the output of DBPediaNED, which is JSON, is parsed below to
			// fetch the corresponding URIs
			JSONParser parser = new JSONParser();
			JSONObject json = (JSONObject) parser.parse(response);
			JSONArray arr = (JSONArray) json.get("Resources");
			
			int cnt = 0;
			if (arr!=null){
				Iterator i = arr.iterator();
				while (i.hasNext()) {
					JSONObject obj = (JSONObject) i.next();
					String uri = (String) obj.get("@URI");
					links.get(cnt).link = uri;
					logger.info("recognized: {} at ({},{})", uri, links.get(cnt).begin, links.get(cnt).end);
					cnt++;
				}
			}
			
			if (cnt == 0) {
				logger.warn("nothing recognized for \"{}\": {}", question, json);
			} else {
				logger.info("recognized {} entities: {}", cnt, json);
			}

			logger.debug("Apply vocabulary alignment on outgraph.");

			// STEP4: Push the result of the component to the triplestore
			// long startTime = System.currentTimeMillis();

			// TODO: prevent that duplicate entries are created within the
			// triplestore, here the same data is added as already exit (see
			// previous SELECT query)
			for (Link l : links) {
				sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
						+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
						+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
						+ "INSERT { " + "GRAPH <" + namedGraph + "> { " //
						+ "  ?a a qa:AnnotationOfInstance . " //
						+ "  ?a oa:hasTarget [ " //
						+ "           a    oa:SpecificResource; " //
						+ "           oa:hasSource    <" + uriQuestion + ">; " //
						+ "           oa:hasSelector  [ " //
						+ "                    a oa:TextPositionSelector ; " //
						+ "                    oa:start \"" + l.begin + "\"^^xsd:nonNegativeInteger ; " //
						+ "                    oa:end  \"" + l.end + "\"^^xsd:nonNegativeInteger  " //
						+ "           ] " //
						+ "  ] . " //
						+ "  ?a oa:hasBody <" + l.link + "> ;" //
						+ "     oa:annotatedBy <https://github.com/dbpedia-spotlight/dbpedia-spotlight> ; " //
						+ "	    oa:AnnotatedAt ?time  " + "}} " //
						+ "WHERE { " //
						+ "  BIND (IRI(str(RAND())) AS ?a) ."//
						+ "  BIND (now() as ?time) " //
						+ "}";
				logger.debug("Sparql query: {}", sparql);
				loadTripleStore(sparql, endpoint);
			}
			long estimatedTime = System.currentTimeMillis() - startTime;
			logger.info("Time {}", estimatedTime);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return QanaryMessage;
	}

	public void loadTripleStore(String sparqlQuery, String endpoint) {
		UpdateRequest request = UpdateFactory.create(sparqlQuery);
		UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint);
		proc.execute();
	}

	public ResultSet selectTripleStore(String sparqlQuery, String endpoint) {
		Query query = QueryFactory.create(sparqlQuery);
		QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
		return qExe.execSelect();
	}

	class Spot {
		public int begin;
		public int end;
	}

	class Link {
		public int begin;
		public int end;
		public String link;
	}

}
