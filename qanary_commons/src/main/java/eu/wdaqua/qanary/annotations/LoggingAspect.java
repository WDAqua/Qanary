    package eu.wdaqua.qanary.annotations;

    import eu.wdaqua.qanary.commons.QanaryMessage;
    import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
    import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
    import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
    import org.apache.jena.query.QuerySolution;
    import org.apache.jena.query.QuerySolutionMap;
    import org.apache.jena.rdf.model.ResourceFactory;
    import org.aspectj.lang.JoinPoint;
    import org.aspectj.lang.ProceedingJoinPoint;
    import org.aspectj.lang.annotation.*;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Component;

    import java.io.IOException;
    import java.net.URI;
    import java.util.Arrays;

    @Aspect
    @Component
    public class LoggingAspect {

        private Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
        private URI currentProcessGraph;
        private final String LOGGING_QUERY = "/queries/logging/insert_method_data.rq";
        private QanaryTripleStoreConnector qanaryTripleStoreConnector;

        @Pointcut("execution(* eu.wdaqua.qanary.component..*(..))")
        public void allExecutionsInComponent() {

        }

        @Autowired
        public void setupTriplestore(QanaryTripleStoreConnector qanaryTripleStoreProxy) {
            this.qanaryTripleStoreConnector = qanaryTripleStoreProxy;
        }

        @Around(value = "allExecutionsInComponent()")
        public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
            Object[] args = joinPoint.getArgs();
            String methodName = joinPoint.getSignature().getName();
            logger.info(">> {}() - {}", methodName, Arrays.toString(args));
            logger.info("<< {}() - {}", "a");
            Object result = joinPoint.proceed();
            return result;
        }

        @AfterReturning(value = "execution(* *(..)) && @within(LogExecution) && !execution(* qanaryTripleStoreProxy(..))", returning = "result")
        public void logAroundWithAnnotation(JoinPoint joinPoint, Object result) throws Throwable {
            Object[] args = joinPoint.getArgs();
            String methodName = joinPoint.getSignature().getName();
            logMethodData(args, result, methodName);
        }

        public void logMethodData(Object[] args, Object result, String methodName) throws IOException, SparqlQueryFailed {
            QuerySolutionMap qsm = new QuerySolutionMap();
            qsm.add("method", ResourceFactory.createPlainLiteral(methodName));
            qsm.add("input", ResourceFactory.createPlainLiteral(args.toString()));
            qsm.add("output", ResourceFactory.createPlainLiteral(result.toString()));
            String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(LOGGING_QUERY, qsm);
            this.qanaryTripleStoreConnector.update(query);
        }

    }
