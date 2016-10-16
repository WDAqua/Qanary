package eu.wdaqua.qanary.spotlightNED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import eu.wdaqua.qanary.component.QanaryQuestion;
import eu.wdaqua.qanary.component.QanaryUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;

/**
 * represents a wrapper of the DBpedia Spotlight as NED
 *
 * @author Kuldeep Singh, Dennis Diefenbach
 */

@Component
public class DBpediaSpotlightNED extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(DBpediaSpotlightNED.class);
    private String service = "http://spotlight.sztaki.hu:2222/rest/disambiguate/";

    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        logger.info("process: {}", myQanaryMessage);


        long startTime = System.currentTimeMillis();
        //STEP 1: Retrive the information needed for the computations

        // retrive the question
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
        String myQuestion = myQanaryQuestion.getTextualRepresentation();
        logger.info("Question: {}", myQuestion);

        // Retrieves the spots from the knowledge graph
        String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
                + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
                + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
                + "SELECT ?start ?end " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
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

        ResultSet r = myQanaryUtils.selectFromTripleStore(sparql);
        ArrayList<Link> links = new ArrayList<Link>();
        while (r.hasNext()) {
            QuerySolution s = r.next();
            Link link = new Link();
            link.begin = s.getLiteral("start").getInt();
            link.end = s.getLiteral("end").getInt();
            logger.info("Spot start {}, end {}", link.begin, link.end);
            links.add(link);
        }

        // STEP2: Call the DBpedia NED service

        // it will create XML content, which needs to be input in DBpedia
        // NED with curl command
        String content = getXmlFromQuestion(myQuestion, links);

        RestTemplate myRestTemplate = new RestTemplate();
        //Set header
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        //Set Body
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("text", content);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        String response = myRestTemplate.postForObject(service, request, String.class);

        // Now the output of DBPediaNED, which is JSON, is parsed below to
        // fetch the corresponding URIs
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(response);
        JSONArray arr = (JSONArray) json.get("Resources");

        int cnt = 0;
        if (arr != null) {
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
            logger.warn("nothing recognized for \"{}\": {}", myQuestion, json);
        } else {
            logger.info("recognized {} entities: {}", cnt, json);
        }

        logger.debug("Apply vocabulary alignment on outgraph.");

        // STEP3: Push the result of the component to the triplestore
        // long startTime = System.currentTimeMillis();

        // TODO: prevent that duplicate entries are created within the
        // triplestore, here the same data is added as already exit (see
        // previous SELECT query)
        for (Link l : links) {
            sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
                    + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
                    + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
                    + "INSERT { " + "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { " //
                    + "  ?a a qa:AnnotationOfInstance . " //
                    + "  ?a oa:hasTarget [ " //
                    + "           a    oa:SpecificResource; " //
                    + "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; " //
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
            myQanaryUtils.updateTripleStore(sparql);
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        logger.info("Time {}", estimatedTime);

        return myQanaryMessage;
    }

    private String getXmlFromQuestion(String question, ArrayList<Link> offsets) {
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

    class Link {
        public int begin;
        public int end;
        public String link;
    }

}
