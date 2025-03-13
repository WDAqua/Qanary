package eu.wdaqua.qanary.explainability.annotations;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorQanaryInternal;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
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

@Aspect
public class AspectGeneral {

    private static final Logger logger = LoggerFactory.getLogger(AspectGeneral.class);
    private static Stack<String> callStack = new Stack<>();
    private static QanaryTripleStoreConnector qanaryTripleStoreConnector;
    private static URI processGraph = null;
    private static Map<String, MethodObject> methodList = new HashMap<>();
    private static boolean activeTracing = false;
    private static String crossComponentProcessId;
    private final String LOGGING_QUERY = "/queries/logging/insert_method_data.rq";
    private String applicationName = null;

    public AspectGeneral() {
        logger.info("Initializing AspectGeneral");
    }

    // Pointcut for all methods outside (QanaryPipeline, not) and QanaryComponent as they are
    // handled separately

    @Pointcut(value = "" +
            "(execution(* * (..)) || execution(private * *(..)) || execution(protected * *(..))) && " +
            "!execution(* eu.wdaqua.qanary.component..* (..)) && " +
            "!execution(* eu.wdaqua.qanary.explainability..*(..))")
    public void anyExecution() {
    }

    /**
     * Sets the returned object for each stored method
     *
     * @param joinPoint JoinPoint
     * @param result    Returned object
     */
    @AfterReturning(value = "anyExecution()", returning = "result")
    public void implementationStoreMethodExecutionInComponentAfter(JoinPoint joinPoint, Object result) {
        if (this.activeTracing) {
            String currentMethodUuid = this.callStack.peek();
            MethodObject method = this.methodList.get(currentMethodUuid);
            method.setOutput(result);
            this.methodList.replace(currentMethodUuid, method);
            this.callStack.pop();
            // Log all stack items
            if (this.callStack.isEmpty()) {
                this.activeTracing = false;
                logMethods(methodList);
                processGraph = null; // Process ended
                this.methodList.clear(); // Clear stored methods
            }
        }
    }

    /**
     * Creates a UUID for the designated method and puts it on the execution-stack.
     *
     * @param joinPoint JoinPoint
     */
    @Before(value = "anyExecution()")
    public void implementationStoreMethodExecutionInComponentBefore(JoinPoint joinPoint) {
        if (!this.activeTracing) {
            // TODO
        }
        if (this.activeTracing) {
            // Get caller and self-push to stack
            String caller = checkAndGetFromStack();
            String uuid = UUID.randomUUID().toString();
            this.callStack.push(uuid);
            // Get required data
            String method = joinPoint.getSignature().getName();
            Object[] input = joinPoint.getArgs();

            this.methodList.put(
                    uuid,
                    new MethodObject(caller, method, input, this.applicationName == null ? "QanaryPipeline" : this.applicationName));
        }
    }

    /**
     * Helper function to get the caller method's uuid. If the stack is empty at the
     * beginning, it returns "init"
     *
     * @return UUID for the latest stack item; "" if stack is empty (first method
     * call)
     */
    public String checkAndGetFromStack() {
        try {
            return this.callStack.peek().toString();
        } catch (EmptyStackException e) {
            return crossComponentProcessId == null
                    ? "init"
                    : crossComponentProcessId;
        }
    }

    // APPLICATION NAME RELATED

    @Pointcut("execution(* getApplicationName())")
    public void applicationNamePointcut() {
    }

    @AfterReturning(value = "applicationNamePointcut()", returning = "result")
    public void getApplicationNameAfterReturning(JoinPoint joinPoint, Object result) {
        this.applicationName = (String) result;
        logger.info("Application name set to: {}", this.applicationName);
    }

    public void logMethods(Map<String, MethodObject> methodMap) throws RuntimeException {
        // k = UUID of the actual method; v = Method details
        try {
            methodMap.forEach((k, v) -> {
                try {
                    this.logMethodData(k, v);
                } catch (IOException | SparqlQueryFailed | NullPointerException e) {
                    logger.error("Method with uuid {} was not logged due to an exception with the error message: {}", k,
                            e.getMessage());
                }
            });
        } catch (NullPointerException e) {
            logger.error("Error");
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
        logger.info("Logging method {}", method.getMethod());
        String query = QanaryTripleStoreConnector.readFileFromResources(LOGGING_QUERY);
        query = query.replace("?graph", "<" + this.processGraph.toASCIIString() + ">");
        query = query.replace("?annotatedBy", "<urn:qanary:" + method.getAnnotatedBy() + ">");
        query = query.replace("?a", "<" + methodUuid + ">");
        query = query.replace("?caller", "<" + method.getCaller() + ">");
        query = query.replace("?method", "'" + method.getMethod() + "'");
        try {
            query = query.replace("?output", generateOutputDataRepresentation(method.getOutput()));
        } catch (NullPointerException e) {
            query = query.replace("?output", generateOutputDataRepresentation(method.getOutput().getClass()));
        }
        query = query.replace("?input", generateInputDataRepresentation(method.getInput()));
        logger.info("Method-log query: {}", query);
        this.qanaryTripleStoreConnector.update(query);
    }

    /**
     * Creates SPARQL-conform representation for the output data
     *
     * @param outputData
     * @return
     */
    public String generateOutputDataRepresentation(Object outputData) {
        if (outputData == null)
            return "[]";
        return "[ rdf:type \"" +
                ResourceFactory.createPlainLiteral(outputData.getClass().toString()) +
                "\" ; rdf:value \""
                + ResourceFactory.createPlainLiteral(outputData.toString().replace("\n", " ").replace("\\", "'")) +
                "\"]";
    }

    /**
     * Transforms object list to SPARQL-conform representation to store data
     *
     * @param inputData
     * @return
     */
    // TODO: FOr I + O, implement a robust solution to handle exceptions during toString().
    public String generateInputDataRepresentation(Object[] inputData) {
        String representation = "(";
        if (inputData.length == 0 || inputData == null)
            return "()";
        logger.info("{} variables", inputData.length);
        for (int i = 0; i < inputData.length; i++) {
            Object var = inputData[i];
            String type;
            String value;
            try {
                type = "\"" + var.getClass() + "\"";
                value = "\"" + (var.toString().isEmpty()
                        ? "null\""
                        : (var.toString()
                        .replace("\n", " ")
                        .replace("\"", "'")
                        + "\""));
            } catch (NullPointerException e) {
                type = "\"null\"";
                value = "\"null\"";
            }
            String varRepresentation = "[ " + "rdf:type " + type + ";" + "rdf:value " + value + " ]";
            representation += varRepresentation;
        }
        representation += ")";
        return representation;
    }


    // PIPELINE SPECIFIC

    @Pointcut(
            "execution(public org.springframework.http.ResponseEntity<?> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestion(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestion(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestionThroughJson(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<?> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.createQuestionAnswering(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<?> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.createQuestionAnsweringFull(..))"
    )
    public void startProcessForPipeline() {
    }

    @Before(value = "startProcessForPipeline()")
    public void startProcessForPipelineBefore(JoinPoint joinPoint) {
        logger.info("Starting process for pipeline");
        callStack.clear();
        methodList.clear();
        activeTracing = true;
        implementationStoreMethodExecutionInComponentBefore(joinPoint);
    }

    @AfterReturning(value = "startProcessForPipeline()", returning = "result")
    public void endProcessForPipeline(JoinPoint joinPoint, Object result) {
        implementationStoreMethodExecutionInComponentAfter(joinPoint, result);
    }

    @Pointcut("execution(* eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.executeComponentList(..))")
    public void setTriplestoreAndGraphForPipeline() {
    }

    @Before(value = "setTriplestoreAndGraphForPipeline()")
    public void setTriplestoreAndGraphForPipeline(JoinPoint joinPoint) throws URISyntaxException {
        QanaryMessage qanaryMessage = (QanaryMessage) joinPoint.getArgs()[2];
        this.processGraph = qanaryMessage.getOutGraph();
        if (qanaryTripleStoreConnector == null) {
            QanaryUtils qanaryUtils = new QanaryUtils(qanaryMessage,
                    new QanaryTripleStoreConnectorQanaryInternal(qanaryMessage.getEndpoint(), this.applicationName == null ? "QanaryPipeline" : applicationName));
            qanaryTripleStoreConnector = qanaryUtils.getQanaryTripleStoreConnector();
        }
        logger.info("Initialized triplestore and graph for pipeline: {}, {}", this.processGraph, this.qanaryTripleStoreConnector.getFullEndpointDescription());
    }

}
