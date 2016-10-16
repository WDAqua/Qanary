package eu.wdaqua.qanary.FOX;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;

import eu.wdaqua.qanary.component.QanaryQuestion;
import eu.wdaqua.qanary.component.QanaryUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.util.UUID;

/**
 * represents a wrapper of the Stanford NER tool used here as a spotter
 *
 * @author Dennis Diefenbach
 */

@Component
public class FOXComponent extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(FOXComponent.class);
    private static final String foxService = "http://fox-demo.aksw.org/api";

    /**
     * default processor of a QanaryMessage
     */
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        long startTime = System.currentTimeMillis();
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
        logger.info("Qanary Message: {}", myQanaryMessage);

        // STEP1: Retrieve the named graph and the endpoint
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
        String myQuestion = myQanaryQuestion.getTextualRepresentation();

        logger.info("Question: {}", myQuestion);

        // STEP3: Pass the information to the component and execute it
        //curl -d type=text -d task=NER -d output=N-Triples --data-urlencode "input=The foundation of the University of Leipzig in 1409 initiated the city's development into a centre of German law and the publishing industry, and towards being a location of the Reichsgericht (High Court), and the German National Library (founded in 1912). The philosopher and mathematician Gottfried Leibniz was born in Leipzig in 1646, and attended the university from 1661-1666." -H "Content-Type: application/x-www-form-urlencoded" http://fox-demo.aksw.org/api
        //Create body
        MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<String, String>();
        bodyMap.add("type", "text");
        bodyMap.add("task", "NER");
        bodyMap.add("output", "N-Triples");
        bodyMap.add("lang", "en");
        bodyMap.add("input", myQuestion);
        //Set Header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        //Set request
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(bodyMap, headers);
        //Execute service
        RestTemplate myRestTemplate = new RestTemplate();
        ResponseEntity<String> model = myRestTemplate.exchange(foxService, HttpMethod.POST, request, String.class);
        String response = model.getBody();
        logger.info("Response from FOX API" + response);

        // STEP4: Vocabulary alignment
        logger.info("Apply vocabulary alignment on outgraph");
        //Retrieve the triples from FOX
        JSONObject obj = new JSONObject(response);
        String triples = URLDecoder.decode(obj.getString("output"));

        //Create a new temporary named graph
        final UUID runID = UUID.randomUUID();
        String namedGraphTemp = "urn:graph:" + runID.toString();

        //Insert data into temporary graph
        String sparql = "INSERT DATA { GRAPH <" + namedGraphTemp + "> {" + triples + "}}";
        logger.info(sparql);
        myQanaryUtils.updateTripleStore(sparql);

        //Align to QANARY vocabulary
        sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
                + "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
                + "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
                + "INSERT { "
                + "GRAPH <" + myQanaryMessage.getOutGraph() + "> { "
                + "  ?a a qa:AnnotationOfSpotInstance . "
                + "  ?a oa:hasTarget [ "
                + "           a    oa:SpecificResource; "
                + "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; "
                + "           oa:hasSelector  [ "
                + "                    a oa:TextPositionSelector ; "
                + "                    oa:start ?begin ; "
                + "                    oa:end  ?end "
                + "           ] "
                + "  ] ; "
                + "     oa:annotatedBy <http://fox-demo.aksw.org> ; "
                + "	    oa:AnnotatedAt ?time  "
                + "}} "
                + "WHERE { "
                + "	SELECT ?a ?s ?begin ?end ?time "
                + "	WHERE { "
                + "		graph <" + namedGraphTemp + "> { "
                + "			?s	<http://ns.aksw.org/scms/beginIndex> ?begin . "
                + "			?s  <http://ns.aksw.org/scms/endIndex> ?end . "
                + "			BIND (IRI(str(RAND())) AS ?a) ."
                + "			BIND (now() as ?time) "
                + "		} "
                + "	} "
                + "}";
        myQanaryUtils.updateTripleStore(sparql);

        //Drop the temporary graph
        sparql = "DROP SILENT GRAPH <" + namedGraphTemp + ">";
        myQanaryUtils.updateTripleStore(sparql);

        long estimatedTime = System.currentTimeMillis() - startTime;
        logger.info("Time: {}", estimatedTime);

        return myQanaryMessage;
    }
}
