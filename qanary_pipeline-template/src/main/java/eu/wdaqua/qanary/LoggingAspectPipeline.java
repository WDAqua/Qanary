package eu.wdaqua.qanary;

import eu.wdaqua.qanary.explainability.annotations.LoggingAspect;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnProperty(name = "pipeline.as.component", havingValue = "false", matchIfMissing = true)
public class LoggingAspectPipeline extends LoggingAspect {

/*
    @Autowired
    public void setTriplestoreConnector(QanaryTripleStoreProxy qanaryTripleStoreConnector) {
        this.qanaryTripleStoreConnector = qanaryTripleStoreConnector;
    }

    @Pointcut("execution(* eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestion(..))")
    public void startOfQaProcessForPipeline() {

    }

    @Pointcut("execution(* eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.executeComponentList(..))")
    public void executedComponentList() {}

    @Pointcut("execution(* eu.wdaqua.qanary.QanaryPipeline..*(..))")
    public void allExecutionsInPipeline() {}

    @AfterReturning(value = "allExecutionsInPipeline()", returning = "result")
    public void logExecutedMethodsInPipeline(JoinPoint joinPoint, Object result) {
        this.getLogger().info("Log method");
        addMethodToList(joinPoint, result);
    }

    @AfterReturning(value = "executedComponentList()", returning = "result")
    public void setGraph(Object result) {
        QanaryQuestionAnsweringRun qanaryQuestionAnsweringRun = (QanaryQuestionAnsweringRun) result;
        this.currentProcessGraph = qanaryQuestionAnsweringRun.getInGraph();
        this.getLogger().info("Found and using graph: {}", this.currentProcessGraph);
    }

    @Before("startOfQaProcessForPipeline()")
    public void startContext() {
        this.getLogger().info("Start pipeline logging context");
        this.methodList = new ArrayList<>();
    }

    @AfterReturning(value = "startOfQaProcessForPipeline()", returning = "result")
    public void endContext() throws IOException, SparqlQueryFailed {
        this.getLogger().info("End pipeline logging context and store methods");
        this.logMethods();
    }

*/

}
