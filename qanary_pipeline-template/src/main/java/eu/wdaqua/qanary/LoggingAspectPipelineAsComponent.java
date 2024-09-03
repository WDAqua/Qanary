package eu.wdaqua.qanary;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.explainability.annotations.LoggingAspect;
import eu.wdaqua.qanary.explainability.annotations.MethodObject;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

@Aspect
@Controller
@ConditionalOnProperty(name = "pipeline.as.component", havingValue = "true")
public class LoggingAspectPipelineAsComponent extends LoggingAspect {




    @Autowired
    public void setTriplestoreConnector(QanaryTripleStoreProxy qanaryTripleStoreConnector) {
        this.qanaryTripleStoreConnector = qanaryTripleStoreConnector;
    }

    @Pointcut("execution(* eu.wdaqua.qanary.QanaryPipelineComponent..*(..))")
    public void allExecutionsWithinPac() {}

    @Pointcut("execution(* process(eu.wdaqua.qanary.commons.QanaryMessage))")
    public void processExecution() {

    }

    @Before(value = "processExecution()")
    public void logGraphFromProcess(JoinPoint joinPoint) {
        QanaryMessage qanaryMessage = (QanaryMessage) Arrays.asList(joinPoint.getArgs()).get(0);
        this.currentProcessGraph = qanaryMessage.getInGraph();
    }

    /**
     * Context begin
     */
    @Pointcut("execution(* annotatequestion(..))")
    public void componentContextIdentifier() {}

    @Before(value = "componentContextIdentifier()")
    public void startComponentContext() {
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
                    method.getClassName(),
                    method.getInputTypes(),
                    method.getOutputType()
            );
        }
    }

    /**
     * Store method as soon as it returned value
     * @param joinPoint JoinPoint
     * @param result Result of any data type (It is required to provide an informative toString() method)
     */
    @AfterReturning(value = "allExecutionsWithinPac()", returning = "result")
    public void addResultToMethodFromComponentContext(JoinPoint joinPoint, Object result) {
        addMethodToList(joinPoint, result);
    }


}
