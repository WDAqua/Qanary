package eu.wdaqua.qanary.component;

import java.net.URI;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.wdaqua.qanary.component.config.QanaryConfiguration;

@Controller
public class QanaryServiceController {

    private static final Logger logger = LoggerFactory.getLogger(QanaryServiceController.class);

    private QanaryComponent qanaryComponent;

    @Inject
    public QanaryServiceController(QanaryComponent qanaryComponent) {
        this.qanaryComponent = qanaryComponent;
        logger.info("qanaryComponent: {}", this.qanaryComponent);
    }

    /**
     * provides a description HTML page of the component, replace description.html to custom page
     */
    @RequestMapping(value = QanaryConfiguration.description, produces = {"text/html"})
    public
    @ResponseBody
    String description() {
        return "description";
    }

    /**
     * example: curl -X POST -d 'message={"http://qanary/#endpoint":"http://x.y"}'
     * http://localhost:8080/annotatequestion | python -m json.tool
     */
    @RequestMapping(value = QanaryConfiguration.annotatequestion, consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
    @ResponseBody
    // public QanaryMessage annotatequestion(@RequestBody QanaryMessage
    // myQanaryMessage) {
    public QanaryMessage annotatequestion(HttpServletRequest request, @RequestBody String message) throws Exception {
        logger.info("annotatequestion: {}", message);
        long start = QanaryUtils.getTime();

        QanaryConfiguration.setServiceUri(new URI(String.format("%s://%s:%d/" + QanaryConfiguration.annotatequestion,
                request.getScheme(), request.getServerName(), request.getServerPort())));

        QanaryMessage myQanaryMessage = new QanaryMessage(message);

        this.qanaryComponent.process(myQanaryMessage);

        logger.debug("processing took: {} ms", QanaryUtils.getTime() - start);

        return myQanaryMessage;
    }

}
