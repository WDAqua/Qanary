    package eu.wdaqua.qanary.explainability.annotations;

    import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
    import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
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
    import java.util.*;
    import java.util.stream.Collectors;

    @Aspect
    @Service
    public class LoggingAspect {

        private Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
        protected URI currentProcessGraph;
        protected Map<UUID, MethodObject> methodList = new HashMap<>();
        private final String LOGGING_QUERY = "/queries/logging/insert_method_data.rq";
        protected QanaryTripleStoreConnector qanaryTripleStoreConnector;
        private Stack callStack = new Stack<UUID>();
        @Value("${spring.application.name}")
        private String applicationName;

        public Logger getLogger() {
            return logger;
        }

        public void setQanaryTripleStoreConnector(QanaryTripleStoreConnector qanaryTripleStoreConnector) {
            if(qanaryTripleStoreConnector != null) {
                this.qanaryTripleStoreConnector = qanaryTripleStoreConnector;
            }
            // else: it's a component; set QTSC when process executed
        }

        public QanaryTripleStoreConnector getQanaryTripleStoreConnector() {
            return qanaryTripleStoreConnector;
        }
/*
        @Pointcut("execution(* eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestion(..))")
        public void startOfQaProcessForPipeline() {

        }

        @Pointcut("execution(* eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.executeComponentList(..))")
        public void executionOfComponentList() {

        }

        @Pointcut("execution(* eu.wdaqua.qanary.QanaryPipeline.*(..))")
        public void executedPipelineMethods() {


        }


 */
        /* TODO: Need for implementation?
        @Pointcut("execution(* eu.wdaqua.qanary.web..startquestionansweringwithtextquestion(..))")
        public void executedPipelineMethodsFromController() {

        }
        */

/*
        @Before(value = "executionOfComponentList()")
        public void storeGraphForPipeline(JoinPoint joinPoint) {
            QanaryMessage qanaryMessage = (QanaryMessage) joinPoint.getArgs()[2];
            this.currentProcessGraph = qanaryMessage.getInGraph();
        }

        @Before(value = "startOfQaProcessForPipeline()")
        public void initPipelineQaProcess() {
            this.methodList = new ArrayList<>();
        }

        @AfterReturning(value = "executedPipelineMethods()", returning = "result")
        public void logPipelineMethods(JoinPoint joinPoint, Object result) {
            addMethodToList(joinPoint, result);
        }

        @AfterReturning(value = "startOfQaProcessForPipeline()")
        public void storeLoggedData() throws IOException, SparqlQueryFailed {
            for(MethodObject method : this.methodList) {
                this.logMethodData(
                        method.getInput(),
                        method.getOutput(),
                        method.getMethodName(),
                        method.getClassName()
                );
            }
        }

        // Target all methods within any Qanary component
        @Pointcut("execution(* eu.wdaqua.qanary.component..*(..)) || execution(* eu.wdaqua.qanary.QanaryPipelineComponent.*(..))")
        public void allExecutionsWithinAComponentOrPac() {

        }


 */

        public void logMethodData(UUID methodUuid, MethodObject method) throws IOException, SparqlQueryFailed {
            String query = QanaryTripleStoreConnector.readFileFromResources(LOGGING_QUERY);
            query = query.replace("?graph", "<" + this.currentProcessGraph.toASCIIString() + ">");
            query = query.replace("?a", "<" + methodUuid.toString() + ">");
            query = query.replace("?caller", "<" + method.getCaller().toString() + ">");
            query = query.replace("?method", "'" + method.getMethod() + "'");
            query = query.replace("?annotatedBy", "<urn:qanary:" + this.applicationName + ">");
            if(method.getInput() != null) {
                query = query.replace("?input", "");
            } else {
                query = query.replace("?input", "");
            }
            if(method.getOutput() != null) { // TODOs as shortcuts for IDE's?
                query = query.replace("?output", "");
            } else {
                query = query.replace("?output", "");
            }
            query = query.replace("?explanationType", "temp").replace("?explanationValue", "tempValue");
            logger.info("Query: {}", query);
            this.qanaryTripleStoreConnector.update(query);
        }

            public void logMethods() throws RuntimeException{
                // k = UUID of the actual method; v = Method details
            this.methodList.forEach((k,v) -> {
                try {
                    this.logMethodData(k, v);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (SparqlQueryFailed e) {
                    throw new RuntimeException(e);
                }
            });
        }


        ///////////////////////////////////////////////////////

        // TODO: When should the logging start? At Controller call or process-call?

        @Pointcut("execution(* annotatequestion(..))")
        public void startByAnnotateQuestion() {};

        @Pointcut("execution(* eu.wdaqua.qanary.component..*(..))") // || execution(* eu.wdaqua.qanary.QanaryPipelineComponent.*(..))
        public void storeMethodExecutionInComponent() {};
        // TODO: CHeck different paths for all components

        @AfterReturning(value = "storeMethodExecutionInComponent()", returning = "result")
        public void implementationStoreMethodExecutionInComponentAfter(JoinPoint joinPoint, Object result) throws IOException, SparqlQueryFailed {
            UUID currentMethodUuid = (UUID) this.callStack.peek();
            MethodObject method = this.methodList.get(currentMethodUuid);
            method.setOutput(result);
            this.methodList.replace(currentMethodUuid, method);
            this.callStack.pop();
            if(this.callStack.isEmpty()) {
                logMethods();
            }
        }

        @Before(value = "storeMethodExecutionInComponent()")
        public void implementationStoreMethodExecutionInComponentBefore(JoinPoint joinPoint) {
            // Get caller and self-push to stack
            UUID caller = checkAndGetFromStack();
            UUID uuid = UUID.randomUUID();
            this.callStack.push(uuid);

            // Get required data
            String method = joinPoint.getSignature().getName();
            Object[] input = joinPoint.getArgs();

            this.methodList.put(
                    uuid,
                    new MethodObject(caller, method, input, this.applicationName) // Caller == null IF first tracked method call
            );
            logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Method call to method {}", method);

        }

        public UUID checkAndGetFromStack() {
            try {
                return (UUID) this.callStack.peek();
            } catch(EmptyStackException e) {
                return null;
            }
        }

        /* COMPONENT POINTCUTS
        * Execution begins on annotatequestion
        * Caller = Item on Stack
        *
        *
        */
    }
