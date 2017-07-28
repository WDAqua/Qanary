package eu.wdaqua.qanary.agdistis;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * represents a wrapper of the Stanford NER tool used here as a spotter
 *
 * @author Dennis Diefenbach
 */

@Component
public class Agdistis extends QanaryComponent {
    private final String agdistisService = "http://139.18.2.164:8080/AGDISTIS";
    private static final Logger logger = LoggerFactory.getLogger(Agdistis.class);

    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        try {
            long startTime = System.currentTimeMillis();
            logger.info("process: {}", myQanaryMessage);
            //STEP 1: Retrive the information needed for the question

            // the class QanaryUtils provides some helpers for standard tasks
            QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
            QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);

            // Retrives the question string
            String myQuestion = myQanaryQuestion.getTextualRepresentation();

            // Retrieves the spots from the knowledge graph
            String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
                    + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
                    + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
                    + "SELECT ?start ?end "
                    + "FROM <" + myQanaryMessage.getInGraph() + "> "
                    + "WHERE { "
                    + "?a a qa:AnnotationOfSpotInstance . "
                    + "?a oa:hasTarget [ "
                    + "		a    oa:SpecificResource; "
                    + "		oa:hasSource    ?q; "
                    + "		oa:hasSelector  [ "
                    + "			a oa:TextPositionSelector ; "
                    + "			oa:start ?start ; "
                    + "			oa:end  ?end "
                    + "		] "
                    + "] ; "
                    + "} "
                    + "ORDER BY ?start ";
            ResultSet r = myQanaryUtils.selectFromTripleStore(sparql, myQanaryMessage.getEndpoint().toString());
            ArrayList<Spot> spots = new ArrayList<Spot>();
            while (r.hasNext()) {
                QuerySolution s = r.next();
                Spot spot = new Spot();
                spot.begin = s.getLiteral("start").getInt();
                spot.end = s.getLiteral("end").getInt();
                logger.info("Spot: {}-{}", spot.begin, spot.end);
                spots.add(spot);
            }

            // Step 2: Call the AGDISTIS service
            // Informations about the AGDISTIS API can be found here: https://github.com/AKSW/AGDISTIS/wiki/2-Asking-the-webservice
            // curl --data-urlencode "text='The <entity>University of Leipzig</entity> in <entity>Barack Obama</entity>.'" -d type='agdistis' http://139.18.2.164:8080/AGDISTIS
            // Match the format "The <entity>University of Leipzig</entity> in <entity>Barack Obama</entity>."
            String input = myQuestion;
            Integer offset = 0;
            for (Spot spot : spots) {
                input = input.substring(0, spot.begin + offset) + "<entity>"
                        + input.substring(spot.begin + offset, spot.end + offset)
                        + "</entity>"
                        + input.substring(spot.end + offset, input.length());
                offset += "<entity>".length() + "</entity>".length();
            }
            // String input="The <entity>University of Leipzig</entity> in <entity>Barack Obama</entity>.";
            logger.info("Input to Agdistis: " + input);
            UriComponentsBuilder service = UriComponentsBuilder.fromHttpUrl(agdistisService);
            logger.info("Service request " + service);
            String body = "type=agdistis&" + "text='" + URLEncoder.encode(input, "UTF-8") + "'";
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.postForObject(service.build().encode().toUri(), body, String.class);
            logger.info("JSON document from Agdistis api {}", response);
            // Extract entities
            ArrayList<Link> links = new ArrayList<Link>();
            JSONArray arr = new JSONArray(response);
            for (int i = 0; i < arr.length(); i++) {
                if (!arr.getJSONObject(i).isNull("disambiguatedURL")) {
                    Link l = new Link();
                    l.link = arr.getJSONObject(i).getString("disambiguatedURL");
                    l.begin = arr.getJSONObject(i).getInt("start") - 1;
                    l.end = arr.getJSONObject(i).getInt("start") - 1 + arr.getJSONObject(i).getInt("offset");
                    links.add(l);
                }
            }

            //STEP4: Push the result of the component to the triplestore
            logger.info("Apply commons alignment on outgraph");
            for (Link l : links) {
                sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
                        + "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
                        + "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
                        + "INSERT { "
                        + "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { "
                        + "  ?a a qa:AnnotationOfInstance . "
                        + "  ?a oa:hasTarget [ "
                        + "           a    oa:SpecificResource; "
                        + "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; "
                        + "           oa:hasSelector  [ "
                        + "                    a oa:TextPositionSelector ; "
                        + "                    oa:start \"" + l.begin + "\"^^xsd:nonNegativeInteger ; "
                        + "                    oa:end  \"" + l.end + "\"^^xsd:nonNegativeInteger  "
                        + "           ] "
                        + "  ] . "
                        + "  ?a oa:hasBody <" + l.link + "> ;"
                        + "     oa:annotatedBy <http://agdistis.aksw.org> ; "
                        + "	    oa:AnnotatedAt ?time  "
                        + "}} "
                        + "WHERE { "
                        + "BIND (IRI(str(RAND())) AS ?a) ."
                        + "BIND (now() as ?time) "
                        + "}";
                logger.info("Sparql query {}", sparql);
                myQanaryUtils.updateTripleStore(sparql, myQanaryQuestion.getEndpoint().toString());
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            logger.info("Time {}", estimatedTime);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return myQanaryMessage;
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
