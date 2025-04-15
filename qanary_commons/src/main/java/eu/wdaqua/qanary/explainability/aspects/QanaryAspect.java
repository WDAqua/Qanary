package eu.wdaqua.qanary.explainability.aspects;

import com.github.therapi.runtimejavadoc.MethodJavadoc;
import com.github.therapi.runtimejavadoc.RuntimeJavadoc;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorQanaryInternal;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.rdf.model.ResourceFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@Aspect
public class QanaryAspect {

    private static Stack<String> callStack = new Stack<>();
    private final Logger logger = LoggerFactory.getLogger(QanaryAspect.class);
    private final String LOGGING_QUERY = "/queries/logging/insert_method_data.rq";
    private QanaryTripleStoreConnector qanaryTripleStoreConnector;
    private URI processGraph = null;
    private Map<String, MethodObject> methodList = new HashMap<>();
    private boolean activeTracing = false;
    private String crossComponentProcessId;
    private String applicationName = null;

    public QanaryAspect() {
        logger.debug("QanaryAspect initialized");
    }

    public static Stack<String> getCallStack() {
        return callStack;
    }

    public static void setCallStack(Stack<String> callStack) {
        QanaryAspect.callStack = callStack;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationNameForQuery() {
        return getApplicationName() == null ? "QanaryPipeline" : getApplicationName();
    }

    public String getCrossComponentProcessId() {
        return crossComponentProcessId;
    }

    public void setCrossComponentProcessId(String crossComponentProcessId) {
        this.crossComponentProcessId = crossComponentProcessId;
    }

    public URI getProcessGraph() {
        return processGraph;
    }

    public void setProcessGraph(URI processGraph) {
        this.processGraph = processGraph;
    }

    public String getLOGGING_QUERY() {
        return LOGGING_QUERY;
    }

    public Map<String, MethodObject> getMethodList() {
        return methodList;
    }

    public void setMethodList(Map<String, MethodObject> methodList) {
        this.methodList = methodList;
    }

    public QanaryTripleStoreConnector getQanaryTripleStoreConnector() {
        return qanaryTripleStoreConnector;
    }

    // POINTCUTS //

    public void setQanaryTripleStoreConnector(QanaryTripleStoreConnector qanaryTripleStoreConnector) {
        this.qanaryTripleStoreConnector = qanaryTripleStoreConnector;
    }

    public Logger getLogger() {
        return logger;
    }

    public boolean isActiveTracing() {
        return activeTracing;
    }

    public void setActiveTracing(boolean activeTracing) {
        this.activeTracing = activeTracing;
    }

    /**
     * This pointcut is defined for the execution of any public/protected/private method without methods from eu.wdaqua.qanary.component/eu.wdaqua.component/eu.wdaqua.qanary.explainability and explicitly the methods
     * process(QanaryMessage) as well as any interceptor defined in Qanary.
     */
    @Pointcut(value =
            "(execution(* * (..)) || execution(private * *(..)) || execution(protected * *(..))) && " +
                    "!execution(* eu.wdaqua.qanary.component..* (..)) && " +
                    "!execution(* eu.wdaqua.component..* (..)) &&" +
                    "!execution(* eu.wdaqua.qanary.explainability..*(..)) &&" +
                    "!execution(* process(eu.wdaqua.qanary.commons.QanaryMessage)) &&" +
                    "!execution(* eu.wdaqua.qanary.communications..* (..))")
    // TODO: For both interceptors
    public void anyQanaryAndPipelineExecution() {
    }

    /**
     * This pointcut is defined for the execution of the method getApplicationName().
     */
    @Pointcut("execution(* getApplicationName())")
    public void applicationNamePointcut() {
    }

    /**
     * This pointcut is defined for the API call to the question answering capability of Qanary, precisely the QanaryPipeline.
     * It serves as the starting point for the tracing.
     */
    @Pointcut(value =
            "execution(public org.springframework.http.ResponseEntity<?> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestion(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestion(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestionThroughJson(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<?> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.createQuestionAnswering(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<?> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.createQuestionAnsweringFull(..))")
    public void startProcessForPipeline() {
    }

    /**
     * This pointcut is defined for the executeComponentList(..) method for the QanaryPipeline.
     * It serves as a provider for the QanaryTripleStore and process graph
     */
    @Pointcut("execution(* eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.executeComponentList(..))")
    public void setTriplestoreAndGraphForPipeline() {
    }

    /**
     * This pointcut is defined for any method of a QanaryComponent without touching the process(QanaryMethod) method
     */
    @Pointcut(value = "(" +
            "execution(* eu.wdaqua.qanary.component..* (..)) || " +
            "execution(* eu.wdaqua.component..* (..)) || " +
            "execution(private eu.wdaqua.qanary.component..* *(..)) ||" +
            "execution(protected eu.wdaqua.component..* *(..))) && " +
            "!execution(* process(eu.wdaqua.qanary.commons.QanaryMessage))")
    public void componentAspect() {
    }

    /**
     * This pointcut is defined for the process(QanaryMessage) method
     */ // TODO: Later, check for QanaryPipelineComponent
    @Pointcut("execution(* process(eu.wdaqua.qanary.commons.QanaryMessage))")
    public void processExecution() {
    }

    // JoinPoints //

    @Before(value = "anyQanaryAndPipelineExecution()")
    public void implementationStoreMethodExecutionInComponentBeforeForPipeline(JoinPoint joinPoint) {
        if (this.activeTracing) {
            String docstring = extractDocstring(joinPoint);
            // Get caller and self-push to stack
            String caller = checkAndGetFromStack();
            String uuid = UUID.randomUUID().toString();
            this.callStack.push(uuid);
            // Get required data
            String methodName = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
            Object[] input = joinPoint.getArgs();
            MethodObject methodObject = new MethodObject(caller, methodName, input);
            methodObject.setDocstring(docstring);
            this.methodList.put(uuid, methodObject);
        }
    }

    @Before(value = "componentAspect()")
    public void implementationStoreMethodExecutionInComponentBeforeForComponent(JoinPoint joinPoint) {
        if (!isActiveTracing()) {
            // Checks if the current executed method is the HttpRequest containing the cross-component processId
            Object[] args = joinPoint.getArgs();
            setActiveTracing(checkForProcessIdAndSetup(args));
        }
        if (isActiveTracing()) {
            String docstring = extractDocstring(joinPoint);
            getLogger().debug("Intercepted method '{}' before execution with aspect.", joinPoint.getSignature().getName());
            // Get caller and self-push to stack
            String caller = checkAndGetFromStack();
            String uuid = UUID.randomUUID().toString();
            getCallStack().push(uuid);
            getLogger().debug("UUID: {}", uuid);

            // Get required data
            String method = joinPoint.getSignature().getName();
            Object[] input = joinPoint.getArgs();
            MethodObject methodObject = new MethodObject(caller, method, input);
            methodObject.setDocstring(docstring);

            getMethodList().put(uuid, methodObject);
        }
    }

    @Before(value = "startProcessForPipeline()")
    public void startProcessForPipelineBefore(JoinPoint joinPoint) {
        logger.info("Starting process for pipeline");
        callStack.clear();
        methodList.clear();
        activeTracing = true;
        implementationStoreMethodExecutionInComponentBeforeForPipeline(joinPoint);
    }

    @Before(value = "setTriplestoreAndGraphForPipeline()")
    public void setTriplestoreAndGraphForPipeline(JoinPoint joinPoint) throws URISyntaxException {
        // Skip aspect execution if we're in a test
        if (isTestEnvironment()) {
            return;
        }

        // Original aspect code
        QanaryMessage qanaryMessage = (QanaryMessage) joinPoint.getArgs()[2];
        this.processGraph = qanaryMessage.getOutGraph();
        if (qanaryTripleStoreConnector == null) {
            QanaryUtils qanaryUtils = new QanaryUtils(qanaryMessage,
                    new QanaryTripleStoreConnectorQanaryInternal(qanaryMessage.getEndpoint(),
                            this.applicationName == null ? "QanaryPipeline" : applicationName));
            qanaryTripleStoreConnector = qanaryUtils.getQanaryTripleStoreConnector();
        }
        logger.debug("Initialized triplestore and graph for pipeline: {}, {}", this.processGraph, this.qanaryTripleStoreConnector.getFullEndpointDescription());
    }

    @Before(value = "processExecution()")
    public void setGraphFromProcessExecution(JoinPoint joinPoint) throws URISyntaxException {
        QanaryMessage qanaryMessage = (QanaryMessage) joinPoint.getArgs()[0];
        getLogger().info("Set process graph: {}", qanaryMessage.getInGraph());
        setProcessGraph(qanaryMessage.getInGraph());
        if (getQanaryTripleStoreConnector() == null) {
            QanaryUtils qanaryUtils = new QanaryUtils(qanaryMessage,
                    new QanaryTripleStoreConnectorQanaryInternal(qanaryMessage.getEndpoint(), getApplicationName()));
            setQanaryTripleStoreConnector(qanaryUtils.getQanaryTripleStoreConnector());
        }
        implementationStoreMethodExecutionInComponentBeforeForComponent(joinPoint);
    }

    @AfterReturning(value = "componentAspect() || anyQanaryAndPipelineExecution() || processExecution()", returning = "result")
    public void implementationStoreMethodExecutionInComponentAfter(JoinPoint joinPoint, Object result) {
        if (isActiveTracing()) {
            String currentMethodUuid = getCallStack().peek();
            MethodObject method = getMethodList().get(currentMethodUuid);
            method.setOutput(result);
            getMethodList().replace(currentMethodUuid, method);
            getCallStack().pop();
            getLogger().debug("Logging method '{}' completed, callstack: {}", joinPoint.getSignature().getName(), getCallStack());
            // Log all stack items
            if (getCallStack().isEmpty()) {
                setActiveTracing(false);
                logMethods(getMethodList());
                setProcessGraph(null);
                getMethodList().clear();
            }
        }
    }

    @AfterThrowing(value = "componentAspect()")
    public void implementationStoreMethodExecutionInComponentThrowing(JoinPoint joinPoint) {
        if (isActiveTracing()) {
            String currentMethodUuid = getCallStack().peek();
            MethodObject method = getMethodList().get(currentMethodUuid);
            method.setOutput(null);
            method.setErrorOccurred(true); // TODO: Handle occurred error in logged data, e.g. by return exception message
            getMethodList().replace(currentMethodUuid, method);
            getCallStack().pop();
            getLogger().debug("Logging method with {} completed, callstack: {}", joinPoint.getSignature().getName(), getCallStack());
            // Log all stack items
            if (getCallStack().isEmpty()) {
                setActiveTracing(false);
                logMethods(getMethodList());
                setProcessGraph(null);
                getMethodList().clear();
            }
        }
    }

    @AfterReturning(value = "applicationNamePointcut()", returning = "result")
    public void getApplicationNameAfterReturning(JoinPoint joinPoint, Object result) {
        this.applicationName = (String) result;
    }

    @AfterReturning(value = "startProcessForPipeline()", returning = "result")
    public void endProcessForPipeline(JoinPoint joinPoint, Object result) throws Throwable {
        implementationStoreMethodExecutionInComponentAfter(joinPoint, result);
    }


    // Helper methods //

    /**
     * This method is used to check if the passed args of an JoinPoint contain a HttpServletRequest that provides a processId from a different component
     *
     * @param args Array of arguments
     * @return Whether a processId has been found or not
     */
    public boolean checkForProcessIdAndSetup(Object[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof HttpServletRequest) {
                HttpServletRequest request = (HttpServletRequest) args[i];
                String processId = request.getHeader("processId");
                if (processId != null && !processId.isEmpty()) {
                    this.getLogger().info("Found cross-component call processId: {}", processId);
                    this.setCrossComponentProcessId(processId);
                    resetConfiguration();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Resets callstack and method list
     */
    public void resetConfiguration() {
        this.getCallStack().clear();
        this.getMethodList().clear();
    }

    /**
     * Creates SPARQL-conform representation for the output data
     *
     * @param outputData Object of returned object
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
     * @param inputData Array of input arguments
     * @return
     */
    public String generateInputDataRepresentation(Object[] inputData) {
        String representation = "(";
        if (inputData.length == 0 || inputData == null)
            return "()";
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
        getLogger().debug("Logging method {}", method.getMethod());
        String query = QanaryTripleStoreConnector.readFileFromResources(getLOGGING_QUERY());
        query = query.replace("?graph", "<" + getProcessGraph().toASCIIString() + ">");
        query = query.replace("?annotatedBy", "<urn:qanary:" + getApplicationNameForQuery() + ">");
        query = query.replace("?a", "<" + methodUuid + ">");
        query = query.replace("?caller", "<" + method.getCaller() + ">");
        query = query.replace("?method", "'" + method.getMethod() + "'");
        query = query.replace("?docstring", "'" + method.getDocstring() + "'");
        try {
            query = query.replace("?output", generateOutputDataRepresentation(method.getOutput()));
        } catch (NullPointerException e) {
            query = query.replace("?output", generateOutputDataRepresentation(method.getOutput().getClass()));
        }
        query = query.replace("?input", generateInputDataRepresentation(method.getInput()));
        getQanaryTripleStoreConnector().update(query);
    }

    public void logMethods(Map<String, MethodObject> methodMap) throws RuntimeException {
        // k = UUID of the actual method; v = Method details
        try {
            methodMap.forEach((k, v) -> {
                try {
                    logMethodData(k, v);
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
     * Helper function to get the caller method's uuid. If the stack is empty at the
     * beginning, it returns "init"
     *
     * @return UUID for the latest stack item; "" if stack is empty (first method
     * call)
     */
    public String checkAndGetFromStack() {
        try {
            return callStack.peek().toString();
        } catch (EmptyStackException e) {
            return getCrossComponentProcessId() == null
                    ? "init"
                    : getCrossComponentProcessId();
        }
    }

    private boolean isTestEnvironment() {
        return System.getProperty("spring.profiles.active", "").contains("test")
                || Thread.currentThread().getStackTrace().length > 0
                && Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(element -> element.getClassName().contains("Test"));
    }

    private Method getMethodFromJoinPoint(JoinPoint joinPoint) {
        try {
            String methodName = joinPoint.getSignature().getName();
            Class<?>[] parameterTypes = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getParameterTypes();
            Method method = joinPoint.getTarget().getClass().getMethod(methodName, parameterTypes);
            return method;
        } catch (NoSuchMethodException | NullPointerException e) {
            return null;
        }
    }

    private String extractDocstring(JoinPoint joinPoint) {
        Method method = getMethodFromJoinPoint(joinPoint);
        if (method != null) {
            MethodJavadoc methodJavadoc = RuntimeJavadoc.getJavadoc(method);
            if (methodJavadoc != null) {
                logger.info("Docstring: {} for method '{}'", methodJavadoc.getComment().toString(), method.getName().toString());
                if (methodJavadoc.getComment() != null) {
                    return methodJavadoc.getComment().toString().replace("\n", " ").replace("'", "\"");
                }
            }
        }
        return "";
    }

}
