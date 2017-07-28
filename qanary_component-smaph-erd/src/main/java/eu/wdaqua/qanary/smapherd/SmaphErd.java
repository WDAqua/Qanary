package eu.wdaqua.qanary.smapherd;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.ontology.TextPositionSelector;

@Component
public class SmaphErd extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(SmaphErd.class);
	private String service = "http://localhost:9090/smaph/rest/default";

	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
            QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
            String myQuestion = myQanaryQuestion.getTextualRepresentation();

            // STEP 1: execute a service call to the smaph-erd service
            // STEP 2: parse the smaph-erd service response and create corresponding
            // object related to the qa vocabulary
            Collection<TextPositionSelector> discoveredNamedEntities = this.getSmapherdOutput(myQuestion);

            // STEP 3: save the text selectors with their disambiguations as
            // annotations of the current question to the triplestore
            myQanaryQuestion.addAnnotations(discoveredNamedEntities);

            return myQanaryMessage;
        }

    /**
     * Call smaph-erd web service and return a list of named entities
     */
    private Collection<TextPositionSelector> getSmapherdOutput(String myQuestion) {
        String text;
        int begin;
        int end;
        URI resourceUri;

        List<TextPositionSelector> discoveredNamedEntities = new LinkedList<>();
        RestTemplate myRestTemplate = new RestTemplate();
        UriComponentsBuilder myServiceCall = UriComponentsBuilder.fromHttpUrl(service).queryParam("Text", myQuestion);
        logger.info("Service request: {} ", myServiceCall);
        ResponseEntity<String> response = myRestTemplate.exchange(myServiceCall.build().encode().toUri(),
                HttpMethod.GET, null, String.class);
        org.json.JSONObject jsonObject = new org.json.JSONObject(response.getBody());

        org.json.JSONArray annotations = jsonObject.getJSONArray("annotations");
        for (int i = 0; i < annotations.length(); ++i) {
            org.json.JSONObject annotation = annotations.getJSONObject(i);
            text = annotation.getString("title");
            try {
                resourceUri = new URI(annotation.getString("url"));
                begin = annotation.getInt("start");
                end = begin + annotation.getInt("length");
                logger.info("annotating question {} pos.{} to pos.{} with uri {}", myQuestion, begin, end, resourceUri);
                discoveredNamedEntities.add(new TextPositionSelector(begin, end, resourceUri, 1F));
            } catch (URISyntaxException e) {
                logger.warn("could not process smaph-erd answer item: {}\n{}", annotation.getString("url"), e.getMessage());
            }
        }
        return discoveredNamedEntities;
    }

}
