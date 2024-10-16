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
            query = query.replace("?class", "'" + className + "'");
            query = query.replace("?method", "'" + methodName + "'");
            query = query.replace("?annotatedBy", "<urn:qanary:" + this.applicationName + ">");
            if(args != null) {
                query = query
                        .replace("?input", "qa:input '" + Arrays.stream(args).map(Object::toString).collect(Collectors.joining(", ")) + "' ; qa:inputTypes '" + argTypes + "' ;");
            } else {
                query = query.replace("?input", "");
            }
            if(result != null) { // TODOs as shortcuts for IDE's?
                query = query // TODO: Replace Lists with Seq?  // TODO: Multiple triples for multiple args
                        .replace("?output", "qa:output '" + result + "' ; qa:outputType '" + resultType + "' ;");
            } else {
                query = query.replace("?output", "");
            }
            logger.info("Query: {}", query);
            this.qanaryTripleStoreConnector.update(query);
        }
/*
        // Starting point for component and PaC
        @Pointcut("execution(* annotatequestion(..))")
        public void componentContextIdentifier() {}

        /**
         * Resets the context properties
         * @return
         */
        /*
        @Before(value = "componentContextIdentifier()")
        public void startComponentContext() {
            logger.info(">>>>>>>>>>>>>>>>>>>>>>> START CONTEXT");
            this.methodList = new ArrayList<>();
            this.currentProcessGraph = null;
        }
        */

        /**
         * First: Stores the methods to the triplestore
         */
        /*
        @AfterReturning(value = "componentContextIdentifier()")
        public void endComponentContext() throws IOException, SparqlQueryFailed {
            for(MethodObject method : this.methodList) {
                this.logMethodData(
                        method.getInput(),
                        method.getOutput(),
                        method.getMethodName(),
                        method.getClassName()
                );
            }
        }

        /**
         * Store method as soon as it returned value
         * @param joinPoint JoinPoint
         * @param result Result of any data type (It is required to provide an informative toString() method)

        @AfterReturning(value = "allExecutionsWithinAComponentOrPac()", returning = "result")
        public void addResultToMethodFromComponentContext(JoinPoint joinPoint, Object result) {
            addMethodToList(joinPoint, result);
        }
        */

        /*
        public void addMethodToList(JoinPoint joinPoint, Object result) {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method_ = methodSignature.getMethod();
            Class<?>[] parameterTypes = method_.getParameterTypes();
            MethodObject method = new MethodObject();
            if(joinPoint.getArgs().length != 0) {
                method.setInput(joinPoint.getArgs());
                method.setInputTypes(Arrays.stream(parameterTypes).map(Class::getName).collect(Collectors.toList()));
            } else {
                method.setInputTypes(null);
                method.setInput(null);
            }
            method.setMethodName(joinPoint.getSignature().getName());
            method.setClassName(joinPoint.getTarget().getClass().getName());
            method.setOutput(result);
            try {
                method.setOutputType(result.getClass().getName());
            } catch(NullPointerException e) {
                this.getLogger().error("Void return type");
                method.setOutputType(null);
                method.setOutput(null); // redundant but explicit
            }
            this.methodList.add(method);
        }

         */

            public void logMethods() throws SparqlQueryFailed, IOException{
            this.methodList.forEach((k,v) -> {
                this.logMethodData(k, v);
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
