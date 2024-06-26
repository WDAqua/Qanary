package eu.wdaqua.qanary.component;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;

@Controller
// Used by default, but excluded when a pipeline doesn't act as component
@ConditionalOnProperty(name = "pipeline.as.component", matchIfMissing = true, havingValue = "true")
public class QanaryServiceController {

    public final static String filenameOnlyPostInteractionAllowed = "only-post-is-allowed.html";
    private static final Logger logger = LoggerFactory.getLogger(QanaryServiceController.class);
    @Value("${spring.boot.admin.client.url}")
    private String qanaryHost;

    private QanaryComponent qanaryComponent;

    @Inject
    public QanaryServiceController(QanaryComponent qanaryComponent) {
        this.qanaryComponent = qanaryComponent;
        logger.info("qanaryComponent: {}", this.qanaryComponent);
    }

    /**
     * example:
     * <pre>
     * curl -X POST -d 'message={"http://qanary/#endpoint":"http://x.y"}' http://localhost:8080/annotatequestion | python -m json.tool
     * </pre>
     */
    @PostMapping(value = {QanaryConfiguration.annotatequestion, "/" + QanaryConfiguration.annotatequestion}, consumes = "application/json", produces = "application/json")
    @ResponseBody
    @Operation(
            summary = "Each Qanary process will implement this endpoint as it is required ", //
            operationId = "showDescriptionOnGetRequestOnRoot", //
            description = "for showing information in a Web browser" //
    )
    public ResponseEntity<?> annotatequestion(HttpServletRequest request, @RequestBody String message) throws Exception {
        logger.info("annotatequestion: {}", message);
        long start = QanaryUtils.getTime();

        QanaryConfiguration.setServiceUri(new URI(String.format("%s://%s:%d/" + QanaryConfiguration.annotatequestion,
                request.getScheme(), request.getServerName(), request.getServerPort())));
        QanaryConfiguration.setServiceUri(new URI(qanaryHost));

        QanaryMessage myQanaryMessage = new QanaryMessage(message);

        this.qanaryComponent.setQanaryMessage(myQanaryMessage);
        this.qanaryComponent.setUtils(myQanaryMessage);

        try {
            this.qanaryComponent.process(myQanaryMessage);
        } catch (Exception e) {
            logger.error("Something went wrong while executing the 'process' method: {}", e.getMessage());
            return new ResponseEntity<String>(e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }


        logger.debug("processing took: {} ms", QanaryUtils.getTime() - start);

        return new ResponseEntity<QanaryMessage>(myQanaryMessage, HttpStatus.OK);
    }

    /**
     * not intended -> fallback: showing information to the user
     *
     * @param response
     * @return
     * @throws Exception
     */
    @GetMapping(value = {QanaryConfiguration.annotatequestion, "/" + QanaryConfiguration.annotatequestion})
    public String showDescriptionOnGetRequest(HttpServletResponse response) throws Exception {
        return filenameOnlyPostInteractionAllowed;
    }
}
