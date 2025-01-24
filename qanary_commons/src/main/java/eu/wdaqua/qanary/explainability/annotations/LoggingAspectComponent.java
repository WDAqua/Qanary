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

/**
 * This class' purpose is to log all method executions within the process()-method.
 * It is done by starting the logging process before the process()-method is called and ending it when the method returns.
 */

@Aspect
public class LoggingAspectComponent {

    private URI processGraph;
    private QanaryTripleStoreConnector qanaryTripleStoreConnector;

    public QanaryTripleStoreConnector getQanaryTripleStoreConnector() {
        return qanaryTripleStoreConnector;
    }

    public void setQanaryTripleStoreConnector(QanaryTripleStoreConnector qanaryTripleStoreConnector) {
        this.qanaryTripleStoreConnector = qanaryTripleStoreConnector;
    }

    public URI getProcessGraph() {
        return processGraph;
    }

    public void setProcessGraph(URI processGraph) {
        this.processGraph = processGraph;
    }

    public QanaryTripleStoreConnectorQanaryInternal createTriplestoreConnectorFromJoinPoint(JoinPoint joinPoint,
            String applicationName) throws URISyntaxException {
        QanaryMessage qanaryMessage = (QanaryMessage) joinPoint.getArgs()[2];
        return new QanaryTripleStoreConnectorQanaryInternal(qanaryMessage.getEndpoint(), applicationName);
    }

    public URI createGraphFromJoinPoint(JoinPoint joinPoint) {
        QanaryMessage qanaryMessage = (QanaryMessage) joinPoint.getArgs()[2];
        return qanaryMessage.getInGraph();
    }

    private Logger logger = LoggerFactory.getLogger(LoggingAspectComponent.class);
    private Map<String, MethodObject> methodList = new HashMap<>();
    private final String LOGGING_QUERY = "/queries/logging/insert_method_data.rq";
    private Stack<String> callStack = new Stack<String>();
    private String applicationName;
    public final String MAP_IS_NULL_ERROR = "Passed map is null; No method logged";
    public final String EMPTY_STACK_ITEM = "init";

    public Stack<String> getCallStack() {
        return callStack;
    }

    public void setCallStack(Stack<String> callStack) {
        this.callStack = callStack;
    }

    public Map<String, MethodObject> getMethodList() {
        return methodList;
    }

    public void setMethodList(Map<String, MethodObject> methodList) {
        this.methodList = methodList;
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
        this.qanaryTripleStoreConnector.update(query);
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
            logger.error(MAP_IS_NULL_ERROR);
        }
    }

    // Separated due to overlapping pointcuts and thus condition race
    @Pointcut("!execution(* process(eu.wdaqua.qanary.commons.QanaryMessage)) && " +
            "(execution(* eu.wdaqua.qanary.component..*(..)) || " +
            "execution(* eu.wdaqua.qanary.QanaryPipelineComponent..*(..)))")
    public void storeMethodExecutionInComponent() {
    };

    @Pointcut("execution(* process(eu.wdaqua.qanary.commons.QanaryMessage))")
    public void processExecution() {
    };

    /**
     * Sets the graph from the executed process context (i.e. the
     * process(QanaryMessage message) method)
     * 
     * @param joinPoint JoinPoint
     * @throws URISyntaxException Get the endpoint from the QanaryMessage
     */
    @Before(value = "processExecution()")
    public void setGraphFromProcessExecution(JoinPoint joinPoint) throws URISyntaxException {
        resetConfiguration();
        QanaryMessage qanaryMessage = (QanaryMessage) joinPoint.getArgs()[0];
        this.setProcessGraph(qanaryMessage.getInGraph());
        if (this.getQanaryTripleStoreConnector() == null) {
            QanaryUtils qanaryUtils = new QanaryUtils(qanaryMessage,
                    new QanaryTripleStoreConnectorQanaryInternal(qanaryMessage.getEndpoint(), this.applicationName));
            this.setQanaryTripleStoreConnector(qanaryUtils.getQanaryTripleStoreConnector());
        }
        implementationStoreMethodExecutionInComponentBefore(joinPoint);
    }

    /**
     * Sets the returned object for each stored method
     * 
     * @param joinPoint JoinPoint
     * @param result    Returned object
     */
    @AfterReturning(value = "storeMethodExecutionInComponent()", returning = "result")
    public void implementationStoreMethodExecutionInComponentAfter(JoinPoint joinPoint, Object result) {
        String currentMethodUuid = (String) this.callStack.peek();
        MethodObject method = this.methodList.get(currentMethodUuid);
        method.setOutput(result);
        this.methodList.replace(currentMethodUuid, method);
        this.callStack.pop();
        if (this.callStack.isEmpty()) {
            logMethods(this.methodList);
            this.setProcessGraph(null); // Process ended
            this.methodList.clear(); // Clear stored methods
        }
    }

    /**
     * Creates a UUID for the designated method and puts it on the execution-stack.
     * 
     * @param joinPoint JoinPoint
     */
    @Before(value = "storeMethodExecutionInComponent()")
    public void implementationStoreMethodExecutionInComponentBefore(JoinPoint joinPoint) {
        // Get caller and self-push to stack
        String caller = checkAndGetFromStack();
        String uuid = UUID.randomUUID().toString();
        this.callStack.push(uuid);

        // Get required data
        String method = joinPoint.getSignature().getName();
        Object[] input = joinPoint.getArgs();

        this.methodList.put(
                uuid,
                new MethodObject(caller, method, input, this.applicationName));
    }

    /**
     * Helper function to get the caller method's uuid. If the stack is empty at the
     * beginning, it returns "init"
     * 
     * @return UUID for the latest stack item; "" if stack is empty (first method
     *         call)
     */
    public String checkAndGetFromStack() {
        try {
            return (String) this.callStack.peek();
        } catch (EmptyStackException e) {
            return EMPTY_STACK_ITEM;
        }
    }

    public void resetConfiguration() {
        this.callStack.clear();
        this.methodList.clear();
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

}
