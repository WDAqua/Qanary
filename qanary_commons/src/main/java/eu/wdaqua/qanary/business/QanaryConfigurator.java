package eu.wdaqua.qanary.business;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import eu.wdaqua.qanary.exceptions.QanaryExceptionServiceCallNotOk;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringFinished;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * represents the configuration of the currently running service
 * <p>
 * Created by didier on 27.03.16.
 */
public class QanaryConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(QanaryConfigurator.class);
    private final RestTemplate restTemplate;
    private final int port;
    private final String host;
    private final URI endpoint;
    private final QanaryTripleStoreProxy myQanaryTripleStoreConnector;
    private final URI qanaryOntology;
    // default configuration defined in the currently used application
    // context (e.g., defined in application.properties)
    private List<String> defaultComponentNames;


    public QanaryConfigurator( //
                               RestTemplate restTemplate, //
                               List<String> defaultComponents, //
                               String serverhost, //
                               int serverport, //
                               URI qanaryOntology, //
                               URI triplestoreendpoint, // TODO: remove?
                               QanaryTripleStoreProxy myQanaryTripleStoreConnector //
    ) {
        this.restTemplate = restTemplate;
        this.setDefaultComponentNames(defaultComponents);
        this.port = serverport;
        this.host = serverhost;
        this.endpoint = triplestoreendpoint;
        this.myQanaryTripleStoreConnector = myQanaryTripleStoreConnector;
        this.qanaryOntology = qanaryOntology;

        logger.warn("make sure the triplestore is available at {}", triplestoreendpoint);
    }

    public QanaryTripleStoreProxy getQanaryTripleStoreConnector() {
        return this.myQanaryTripleStoreConnector;
    }

    /**
     * call the provided components sequentially, as demanded by the provided
     * QanaryMessage
     */
    public QanaryQuestionAnsweringFinished callServices( //
                                                         List<QanaryComponent> myComponents, //
                                                         QanaryMessage message //
    ) throws QanaryExceptionServiceCallNotOk {
        QanaryQuestionAnsweringFinished result = new QanaryQuestionAnsweringFinished(message);
        result.startQuestionAnswering();

        logger.info("QanaryMessage for current process: {} (components={})", message.asJsonString(), myComponents);

        if (myComponents.isEmpty()) {
            logger.warn("callServices with empty component list is recognized: {}", myComponents);
        }

        // run the process for all demanded components
        for (QanaryComponent component : myComponents) {

            URI myURI;
            try {
                myURI = new URI((component.getUrl() + "/annotatequestion").replace("//annotatequestion", "/annotatequestion"));
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return result;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(message.asJsonString(), headers);

            logger.debug("POST request will be performed to {} with {}", myURI, message.asJsonString());

            long start = QanaryUtils.getTime();
            try {
                long startTimeOfComponent = QanaryUtils.getTime();
                ResponseEntity<QanaryMessage> responseEntity = restTemplate.exchange( //
                        myURI, HttpMethod.POST, request, QanaryMessage.class);
                long duration = QanaryUtils.getTime() - startTimeOfComponent;

                result.appendProtocol(component, responseEntity.getStatusCode(), duration);
                if (responseEntity.getStatusCode() == HttpStatus.OK) {
                    message = responseEntity.getBody();
                    logger.debug("received: {}", message);
                } else {
                    logger.error("call to \"{}\" return HTTP {}", component.getName(), responseEntity.getStatusCode());
                    throw new QanaryExceptionServiceCallNotOk(component.getName(), QanaryUtils.getTime() - start,
                            responseEntity.getStatusCode());
                }
            } catch (QanaryExceptionServiceCallNotOk e) {
                logger.error("QanaryExceptionServiceCallNotOk: called \"{}\" catched '{}'", component.getName(), e.getMessage());
                throw e;
            } catch (Exception e) {
                logger.error("Exception during call to component {}: catched '{}' (using URI {}) -> throws '{}'", //
                        component.getName(), //
                        e.getMessage(), //
                        myURI, //
                        ExceptionUtils.getStackTrace(e) //
                );
                throw new QanaryExceptionServiceCallNotOk(
                        component.getName(), //
                        QanaryUtils.getTime() - start, //
                        e.getMessage(),  //
                        ExceptionUtils.getStackTrace(e) //
                );
            }

        }
        result.endQuestionAnswering();
        logger.info("callServices finished: {}", result);
        return result;
    }

    public URI getQanaryOntology() {
        return this.qanaryOntology;
    }

    public int getPort() {
        return this.port;
    }

    public String getHost() {
        return this.host;
    }

    public URI getEndpoint() {
        return this.endpoint;
    }

    /**
     * get the predefined component names
     *
     * @return
     */
    public List<String> getDefaultComponentNames() {
        return defaultComponentNames;
    }

    public void setDefaultComponentNames(List<String> defaultComponentNames) {
        this.defaultComponentNames = defaultComponentNames;
    }

    /**
     * get the list of predefined components as comma-separated list
     *
     * @return
     */
    public String getDefaultComponentNamesAsString() {
        return StringUtils.join(this.getDefaultComponentNames(), ",");
    }

}
