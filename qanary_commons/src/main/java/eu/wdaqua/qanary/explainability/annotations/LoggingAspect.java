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
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.stereotype.Service;

    import java.io.IOException;
    import java.net.URI;
    import java.net.URISyntaxException;
    import java.util.*;

    @Service
    @Aspect
    public class LoggingAspect {

        private Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
        private URI currentProcessGraph;
        private Map<String, MethodObject> methodList = new HashMap<>();
        private final String LOGGING_QUERY = "/queries/logging/insert_method_data.rq";
        private QanaryTripleStoreConnector qanaryTripleStoreConnector;
        private Stack callStack = new Stack<String>();
        @Value("${spring.application.name}")
        private String applicationName;
        public static String MAP_IS_NULL_ERROR = "Passed map is null; No method logged";

        public QanaryTripleStoreConnector getQanaryTripleStoreConnector() {
            return qanaryTripleStoreConnector;
        }

        public URI getCurrentProcessGraph() {
            return currentProcessGraph;
        }

        /**
         * Logs method by using the defined logging query.
         * @param methodUuid UUID of the method to be logged
         * @param method Oject that contains all details needed to get logged
         * @throws IOException During read-process of logging-query
         * @throws SparqlQueryFailed During update-process with the final query
         */
        public void logMethodData(String methodUuid, MethodObject method) throws IOException, SparqlQueryFailed {
            String query = QanaryTripleStoreConnector.readFileFromResources(LOGGING_QUERY);
            query = query.replace("?graph", "<" + this.currentProcessGraph.toASCIIString() + ">");
            query = query.replace("?annotatedBy", "<urn:qanary:" + this.applicationName + ">");
            query = query.replace("?a", "<" + methodUuid + ">");
            query = query.replace("?caller", "<" + method.getCaller() + ">");
            query = query.replace("?method", "'" + method.getMethod() + "'");
            query = query.replace("?output", generateOutputDataRepresentation(method.getOutput()));
            query = query.replace("?input", generateInputDataRepresentation(method.getInput()));
            //query = query.replace("?explanationType", "'temp'").replace("?explanationValue", "'tempValue'").replace("?explanation_generator", "'generator'").replace("?explanationScore","1.0");
            logger.info("Method-log query: {}", query);
            this.qanaryTripleStoreConnector.update(query);
        }

        /**
         * Transforms object list to SPARQL-conform representation to store data
         * @param inputData
         * @return
         */
        public String generateInputDataRepresentation(Object[] inputData) {
            String representation = "(";
            if(inputData.length == 0 || inputData == null)
                return "()";
            for(int i = 0; i < inputData.length; i++) {
                Object var = inputData[i]; //TODO: What for difficult string representations?
                String varRepresentation = "[ " + "rdf:type \"" + var.getClass() + "\" ;" + "rdf:value \"" + ResourceFactory.createPlainLiteral(var.toString()) + "\" ]";
                representation += varRepresentation;
            };
            representation += ")";
            return representation;
        }

        /**
         * Creates SPARQL-conform representation for the output data
         * @param outputData
         * @return
         */
        public String generateOutputDataRepresentation(Object outputData) {
            if(outputData == null)
                return "[]";
            return "[ rdf:type \"" +
                    ResourceFactory.createPlainLiteral(outputData.getClass().toString()) +
                    "\" ; rdf:value \"" + ResourceFactory.createPlainLiteral(outputData.toString()) +
                    "\"]";
        }

        public void logMethods(Map<String,MethodObject> methodMap) throws RuntimeException{
            // k = UUID of the actual method; v = Method details
            try {
                methodMap.forEach((k,v) -> {
                    try {
                        this.logMethodData(k, v);
                    } catch (IOException | SparqlQueryFailed e) {
                        logger.error("Method with uuid {} was not logged due to an exception with the error message: {}", k, e.getMessage());
                    }
                });
                } catch (NullPointerException e) {
                    logger.error(MAP_IS_NULL_ERROR);
                }
        }

        @Pointcut("execution(* eu.wdaqua.qanary.component..*(..)) || execution(* eu.wdaqua.qanary.QanaryPipelineComponent..*(..))") // || execution(* eu.wdaqua.qanary.QanaryPipelineComponent.*(..))
        public void storeMethodExecutionInComponent() {};

        @Pointcut("execution(* process(eu.wdaqua.qanary.commons.QanaryMessage))")
        public void processExecution() {};

        /**
         * Sets the graph from the executed process context (i.e. the process(QanaryMessage message) method)
         * @param joinPoint JoinPoint
         * @throws URISyntaxException Get the endpoint from the QanaryMessage
         */
        @Before(value = "processExecution()")
        public void setGraphFromProcessExecution(JoinPoint joinPoint) throws URISyntaxException {
            QanaryMessage qanaryMessage = (QanaryMessage) joinPoint.getArgs()[0];
            this.currentProcessGraph = qanaryMessage.getInGraph();
            if(this.qanaryTripleStoreConnector == null) {
                QanaryUtils qanaryUtils = new QanaryUtils(qanaryMessage, new QanaryTripleStoreConnectorQanaryInternal(qanaryMessage.getEndpoint(), "null"));
                this.qanaryTripleStoreConnector = qanaryUtils.getQanaryTripleStoreConnector();
            }
        }

        /**
         * Sets the returned object for each stored method
         * @param joinPoint JoinPoint
         * @param result Returned object
         */
        @AfterReturning(value = "storeMethodExecutionInComponent()", returning = "result")
        public void implementationStoreMethodExecutionInComponentAfter(JoinPoint joinPoint, Object result) {
            String currentMethodUuid = (String) this.callStack.peek();
            MethodObject method = this.methodList.get(currentMethodUuid);
            method.setOutput(result);
            this.methodList.replace(currentMethodUuid, method);
            this.callStack.pop();
            if(this.callStack.isEmpty()) {
                if(this.currentProcessGraph == null) {
                    logger.warn("Skipped logging as no processing graph was found");
                }
                else {
                    logMethods(this.methodList);
                    this.currentProcessGraph = null;
                }
            }
        }

        /**
         * Creates a UUID for the designated method and puts it on the execution-stack.
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
                    new MethodObject(caller, method, input, this.applicationName) // Caller == null IF first tracked method call
            );
        }

        /**
         * Helper function to get the caller method's uuid. If the stack is empty at the beginning, it returns "init"
         * @return UUID for the latest stack item; "init" if stack is empty (first method call)
         */
        public String checkAndGetFromStack() {
            try {
                return (String) this.callStack.peek();
            } catch(EmptyStackException e) {
                return "init";
            }
        }

    }
