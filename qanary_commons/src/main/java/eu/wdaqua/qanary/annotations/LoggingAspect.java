    package eu.wdaqua.qanary.annotations;

    import eu.wdaqua.qanary.commons.QanaryMessage;
    import eu.wdaqua.qanary.commons.QanaryUtils;
    import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
    import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorQanaryInternal;
    import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
    import org.apache.jena.query.QuerySolutionMap;
    import org.apache.jena.rdf.model.ResourceFactory;
    import org.aspectj.lang.JoinPoint;
    import org.aspectj.lang.annotation.AfterReturning;
    import org.aspectj.lang.annotation.Aspect;
    import org.aspectj.lang.annotation.Before;
    import org.aspectj.lang.annotation.Pointcut;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.stereotype.Component;

    import java.io.IOException;
    import java.net.URI;
    import java.util.ArrayList;
    import java.util.Arrays;
    import java.util.List;
    import java.util.stream.Collectors;

    @Aspect
    @Component
    public class LoggingAspect {

        private Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
        private URI currentProcessGraph;
        private List<MethodObject> methodList = new ArrayList<>();
        private final String LOGGING_QUERY = "/queries/logging/insert_method_data.rq";
        private QanaryTripleStoreConnector qanaryTripleStoreConnector;
        @Value("${spring.application.name}")
        private String applicationName;

        public void setQanaryTripleStoreConnector(QanaryTripleStoreConnector qanaryTripleStoreConnector) {
            if(qanaryTripleStoreConnector != null) {
                this.qanaryTripleStoreConnector = qanaryTripleStoreConnector;
            }
            // else: it's a component; set QTSC when process executed
        }


        @Pointcut("execution(* eu.wdaqua.qanary.component..*(..))")
        public void allExecutionsInComponent() {

        }

        @Pointcut("execution(* process(eu.wdaqua.qanary.commons.QanaryMessage))")
        public void processExecution() {

        }

        @Before(value = "processExecution()")
        public void logGraphFromProcess(JoinPoint joinPoint) throws Throwable {
            QanaryMessage qanaryMessage = (QanaryMessage) Arrays.asList(joinPoint.getArgs()).get(0);
            if(this.qanaryTripleStoreConnector == null) {
                QanaryUtils qanaryUtils = new QanaryUtils(qanaryMessage, new QanaryTripleStoreConnectorQanaryInternal(qanaryMessage.getEndpoint(), "null"));
                this.qanaryTripleStoreConnector = qanaryUtils.getQanaryTripleStoreConnector();
            }
            this.currentProcessGraph = qanaryMessage.getInGraph();
            logger.info(">>>>>>>>>>>>>>>>>>>>>> Graph found: {}", this.currentProcessGraph.toASCIIString());
        }

        public void logMethodData(Object[] args, Object result, String methodName, String className) throws IOException, SparqlQueryFailed {
            QuerySolutionMap qsm = new QuerySolutionMap();
            qsm.add("graph", ResourceFactory.createResource(this.currentProcessGraph.toASCIIString()));
            qsm.add("class", ResourceFactory.createPlainLiteral(className));
            qsm.add("method", ResourceFactory.createPlainLiteral(methodName));
            qsm.add("input", ResourceFactory.createPlainLiteral(Arrays.stream(args).map(Object::toString).collect(Collectors.joining(", "))));
            qsm.add("output", ResourceFactory.createPlainLiteral(result == null ? "void" : result.toString()));
            qsm.add("annotatedBy", ResourceFactory.createResource(this.applicationName));
            String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(LOGGING_QUERY, qsm);
            this.qanaryTripleStoreConnector.update(query);
        }

        @Pointcut("execution(* annotatequestion(..))") // Is this enough?
        public void componentContextIdentifier() {}

        /**
         * Resets the context properties
         * @return
         */
        @Before(value = "componentContextIdentifier()")
        public void startComponentContext() {
            logger.info(">>>>>>>>>>>>>>>>>>>>>>> START CONTEXT");
            this.methodList = new ArrayList<>();
            this.currentProcessGraph = null;
        }

        /**
         * First: Stores the methods to the triplestore
         */
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

        class MethodObject {
            private String className;
            private String methodName;
            private Object[] input;
            private Object output;

            public Object getOutput() {
                return output;
            }

            public Object[] getInput() {
                return input;
            }

            public String getClassName() {
                return className;
            }

            public String getMethodName() {
                return methodName;
            }

            public void setClassName(String className) {
                this.className = className;
            }

            public void setInput(Object[] input) {
                this.input = input;
            }

            public void setMethodName(String methodName) {
                this.methodName = methodName;
            }

            public void setOutput(Object output) {
                this.output = output;
            }
        }

        /**
         * Store method as soon as it returned value
         * @param joinPoint JoinPoint
         * @param result Result of any data type (It is required to provide an informative toString() method)
         */
        @AfterReturning(value = "allExecutionsInComponent()", returning = "result")
        public void addResultToMethodFromComponentContext(JoinPoint joinPoint, Object result) {
            MethodObject method = new MethodObject();
            method.setInput(joinPoint.getArgs());
            method.setMethodName(joinPoint.getSignature().getName());
            method.setClassName(joinPoint.getTarget().getClass().getName());
            method.setOutput(result);
            this.methodList.add(method);
        }

    }
