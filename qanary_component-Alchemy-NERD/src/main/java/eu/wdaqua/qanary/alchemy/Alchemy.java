package eu.wdaqua.qanary.alchemy;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;
import eu.wdaqua.qanary.component.QanaryQuestion;
import eu.wdaqua.qanary.component.QanaryUtils;
import eu.wdaqua.qanary.component.ontology.TextPositionSelector;

/**
 * represents a wrapper of the Alchemy API as a Entity Linking Tool
 *
 * @author Dennis Diefenbach, AnBo
 */
@SpringBootApplication
@ComponentScan("eu.wdaqua.qanary.component")
public class Alchemy extends QanaryComponent {
    private final String alchemyKey = "7fdef5a245edb49cfc711e80217667be512869b9";
    private final String alchemyService = "http://access.alchemyapi.com/calls/text/TextGetRankedNamedEntities";
    private static final Logger logger = LoggerFactory.getLogger(Alchemy.class);

    /**
     * processor of a QanaryMessage, (1.) executes a service call to the Alchemy service, (2.) parse
     * the data and (3.) sends the retrieved data to the SPARQL endpoint defined in the Qanary
     * message received
     */
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

        // the class QanaryUtils provides some helpers for standard tasks
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);

        // question string is required as input for the service call
        String myQuestion = myQanaryQuestion.getTextualRepresentation();

        // STEP 1: execute a service call to the Alchemy service
        String xmlRawData = this.getDataFromAlchemyService(myQuestion);

        // STEP 2: parse the Alchemy service response and create corresponding
        // object related to the qa vocabulary
        Collection<TextPositionSelector> discoveredNamedEntities = this.parseXmlInput(xmlRawData, myQuestion);

        // STEP 3: save the text selectors with their disambiguations as
        // annotations of the current question to the triplestore
        myQanaryQuestion.addAnnotations(discoveredNamedEntities);

        return myQanaryMessage;
    }

    /**
     * call Alchemy web service and retrieve data in XML format
     */
    private String getDataFromAlchemyService(String myQuestion) {
        // call the Alchemy service, see also
        // http://www.alchemyapi.com/api/entity/proc.html
        RestTemplate myRestTemplate = new RestTemplate();
        UriComponentsBuilder myServiceCall = UriComponentsBuilder.fromHttpUrl(alchemyService)
                .queryParam("apikey", alchemyKey).queryParam("text", myQuestion);
        logger.info("Service request: {} ", myServiceCall);
        ResponseEntity<String> alchemyResponse = myRestTemplate.exchange(myServiceCall.build().encode().toUri(),
                HttpMethod.GET, null, String.class);
        return alchemyResponse.getBody();
    }

    /**
     * parse the XML return of the Alchemy service to return a list of text selectors
     */
    private Collection<TextPositionSelector> parseXmlInput(String xmlInput, String myQuestion)
            throws DocumentException {
        List<Node> myXmlNodes = DocumentHelper.parseText(xmlInput).selectNodes("/results/entities/entity");

        List<TextPositionSelector> discoveredNamedEntities = new LinkedList<>();

        String resourceUri;
        int begin;
        int end;
        int score;
        String markedText;

        for (Node myXmlNode : myXmlNodes) {
            try {
                markedText = myXmlNode.selectSingleNode("text").getText();
                // DBpedia URI disambiguating the marked text
                resourceUri = myXmlNode.selectSingleNode("disambiguated").selectSingleNode("dbpedia").getText();
                // indexes of the (first) occurrence of the marked text in the
                // question
                begin = myQuestion.indexOf(markedText);
                end = begin + markedText.length();
                // a flaw of the Alchemy service is that the marked text is not
                // unambiguously, hence we have to reduce the score if the
                // marked text occurs several times in the question
                score = 1 / StringUtils.countOccurrencesOf(myQuestion, markedText);

                discoveredNamedEntities.add(new TextPositionSelector(begin, end, resourceUri, score));
            } catch (Exception e) {
                logger.warn("could not process Alchemy answer item: {}\n{}", myXmlNode.asXML(), e.getMessage());
            }
        }

        return discoveredNamedEntities;
    }

    /**
     * a bean is required to enable the automatic integration
     */
    @Bean
    public QanaryComponent qanaryComponent() {
        return new Alchemy();
    }

    /**
     * main method starting the spring application
     */
    public static void main(String[] args) {
        SpringApplication.run(Alchemy.class, args);
    }

}
