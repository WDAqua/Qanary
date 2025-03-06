package eu.wdaqua.qanary.explainability.annotations;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorQanaryInternal;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.rdf.model.ResourceFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

// Potentially outsource duplicate code from LoggingAspectComponent, when known how abstract classes can be used in AspectJ.
@Aspect
public class LoggingAspectPipeline {

    private static Stack<UUID> methodStack = new Stack<>();
    private final Logger logger = LoggerFactory.getLogger(LoggingAspectPipeline.class);
    private final String EMPTY_STACK_ITEM = "init";
    private final String applicationName = "QanaryPipeline";
    private final String MAP_IS_NULL_ERROR = "Method map is null";
    private final String LOGGING_QUERY = "/queries/logging/insert_method_data.rq";
    /// /////////////// CONFIG FOR PIPELINE
    /*
     * START: /questionanswering from controller
     * END: RETURN /questionanswering from controller
     * GRAPH: Fetched from
     * QanaryTripleStoreEndpoint: Fetched from
     */

    private URI processGraph;
    private QanaryTripleStoreConnector qanaryTripleStoreConnector;
    private Map<UUID, MethodObject> methodObjectMap = new HashMap<>();

    public static Stack<UUID> getMethodStack() {
        return methodStack;
    }

    public void setMethodStack(Stack<UUID> methodStack) {
        this.methodStack = methodStack;
    }

    public URI getProcessGraph() {
        return processGraph;
    }

    public void setProcessGraph(URI processGraph) {
        this.processGraph = processGraph;
    }

    public QanaryTripleStoreConnector getQanaryTripleStoreConnector() {
        return qanaryTripleStoreConnector;
    }

    public void setQanaryTripleStoreConnector(QanaryTripleStoreConnector qanaryTripleStoreConnector) {
        this.qanaryTripleStoreConnector = qanaryTripleStoreConnector;
    }

    public Map<UUID, MethodObject> getMethodObjectMap() {
        return methodObjectMap;
    }

    public void setMethodObjectMap(Map<UUID, MethodObject> methodObjectMap) {
        this.methodObjectMap = methodObjectMap;
    }

    @Pointcut(
            "execution(public org.springframework.http.ResponseEntity<?> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestion(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestion(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestionThroughJson(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<?> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.createQuestionAnswering(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<?> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.createQuestionAnsweringFull(..))"
    )
    public void startProcessForPipeline() {
    }

    ;

    @Pointcut("execution(* eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.executeComponentList(..))")
    public void setTriplestoreAndGraphForPipeline() {
    }

    ;

    // Any class in the package eu.wdaqua.qanary.web or eu.wdaqua.qanary(Pipeline)
    @Pointcut("(execution(* eu.wdaqua.qanary.web..*(..)) || execution(* eu.wdaqua.qanary.QanaryPipeline..*(..)) || execution(* eu.wdaqua.qanary.QanaryComponentRegistrationChangeNotifier..*(..))) && !execution(* eu.wdaqua.qanary.web.QanarySparqlProtocolController..*(..))")
    public void logMethodCallPointcut() {
    }

    ;

    @Before(value = "setTriplestoreAndGraphForPipeline()")
    public void setTriplestoreAndGraphForPipeline(JoinPoint joinPoint) throws URISyntaxException {
        QanaryMessage qanaryMessage = (QanaryMessage) joinPoint.getArgs()[2];
        this.processGraph = qanaryMessage.getOutGraph();
        if (this.getQanaryTripleStoreConnector() == null) {
            QanaryUtils qanaryUtils = new QanaryUtils(qanaryMessage,
                    new QanaryTripleStoreConnectorQanaryInternal(qanaryMessage.getEndpoint(), this.applicationName));
            this.setQanaryTripleStoreConnector(qanaryUtils.getQanaryTripleStoreConnector());
        }
        logger.info("Initialized triplestore and graph for pipeline: {}, {}", this.processGraph, this.qanaryTripleStoreConnector.getFullEndpointDescription());
    }

    @Before(value = "startProcessForPipeline()")
    public void startProcessForPipelineBefore(JoinPoint joinPoint) {
        this.methodStack.clear();
        this.methodObjectMap.clear();

    }

    @Before(value = "logMethodCallPointcut()")
    public void logMethodCallBefore(JoinPoint joinPoint) {
        // Get caller and self-push to stack
        String caller = checkAndGetFromStack();
        UUID uuid = UUID.randomUUID();
        this.methodStack.push(uuid);

        // Get required data
        String method = joinPoint.getSignature().getName();
        Object[] input = joinPoint.getArgs();

        this.methodObjectMap.put(
                uuid,
                new MethodObject(caller, method, input, this.applicationName));
    }

    @AfterReturning(value = "logMethodCallPointcut()", returning = "result")
    public void logMethodCallAfter(JoinPoint joinPoint, Object result) {
        UUID currentMethodUuid = this.methodStack.peek();
        MethodObject method = null;
        try {
            method = this.methodObjectMap.get(currentMethodUuid);
        } catch (NullPointerException e) {
            logger.error("Method with uuid {} was not logged, check implementation.");
            // Further handling
        }
        method.setOutput(result);
        this.methodObjectMap.replace(currentMethodUuid, method);
        this.methodStack.pop();
        if (this.methodStack.isEmpty()) {
            logMethods(this.methodObjectMap);
            this.setProcessGraph(null); // Process ended
            this.methodObjectMap.clear(); // Clear stored methods
        }
    }

    public String checkAndGetFromStack() {
        try {
            return this.methodStack.peek().toString();
        } catch (EmptyStackException e) {
            return EMPTY_STACK_ITEM;
        }
    }

    /**
     * Logs method by using the defined logging query.
     *
     * @param methodUuid UUID of the method to be logged
     * @param method     Oject that contains all details needed to get logged
     * @throws IOException       During read-process of logging-query
     * @throws SparqlQueryFailed During update-process with the final query
     */
    public void logMethodData(String methodUuid, MethodObject method)
            throws IOException, SparqlQueryFailed, NullPointerException {
        String query = QanaryTripleStoreConnector.readFileFromResources(LOGGING_QUERY);
        query = query.replace("?graph", "<" + this.getProcessGraph().toASCIIString() + ">");
        query = query.replace("?annotatedBy", "<urn:qanary:" + this.applicationName + ">");
        query = query.replace("?a", "<" + methodUuid + ">");
        query = query.replace("?caller", "<" + method.getCaller() + ">");
        query = query.replace("?method", "'" + method.getMethod() + "'");
        query = query.replace("?output", generateOutputDataRepresentation(method.getOutput()));
        query = query.replace("?input", generateInputDataRepresentation(method.getInput()));
        logger.info("Method-log query: {}", query);
        try {
            this.qanaryTripleStoreConnector.update(query);
        } catch (HttpException e) {
            logger.error("Logging failed due to an HTTP exception with the error message: {}. If this isn't a test, check implementation", e.getMessage());
        }

    }

    /**
     * Transforms object list to SPARQL-conform representation to store data
     *
     * @param inputData
     * @return
     */
    public String generateInputDataRepresentation(Object[] inputData) {
        String representation = "(";
        if (inputData.length == 0 || inputData == null)
            return "()";
        for (int i = 0; i < inputData.length; i++) {
            Object var = inputData[i];
            String varRepresentation = "[ " + "rdf:type \"" + var.getClass() + "\" ;" + "rdf:value \""
                    + ResourceFactory.createPlainLiteral(var.toString().replace("\n", " ").replace("\\", "")) + "\" ]";
            representation += varRepresentation;
        }
        ;
        representation += ")";
        return representation;
    }

    /**
     * Creates SPARQL-conform representation for the output data
     *
     * @param outputData
     * @return
     */
    public String generateOutputDataRepresentation(Object outputData) {
        if (outputData.equals(null))
            return "[]";
        return "[ rdf:type \"" +
                ResourceFactory.createPlainLiteral(outputData.getClass().toString()) +
                "\" ; rdf:value \""
                + ResourceFactory.createPlainLiteral(outputData.toString().replace("\n", " ").replace("\\", "'")) +
                "\"]";
    }

    public void logMethods(Map<UUID, MethodObject> methodMap) throws RuntimeException {
        // k = UUID of the actual method; v = Method details
        try {
            methodMap.forEach((k, v) -> {
                try {
                    this.logMethodData(k.toString(), v);
                } catch (IOException | SparqlQueryFailed | NullPointerException e) {
                    logger.error("Method with uuid {} was not logged due to an exception with the error message: {}", k,
                            e.getMessage());
                }
            });
        } catch (NullPointerException e) {
            logger.error(MAP_IS_NULL_ERROR);
        }
    }

    /*
     * Returns whether the method call is expected in terms of the call hierarchy, i.e. the call is within the expected flow
     * Needs to know the caller for a JoinPoint
     * Approaches to solve:
     * * Exclude all external methods calls (what with methods that are called from those excluded methods?)
     * * For research purposes: Use a stack to keep track of the method calls (best case w/o parallel calls)
     */
    public boolean isMethodCallExpected() {
        return false;
    }


}
