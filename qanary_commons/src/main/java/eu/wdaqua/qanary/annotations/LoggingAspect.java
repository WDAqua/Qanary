package eu.wdaqua.qanary.annotations;

import eu.wdaqua.qanary.commons.QanaryMessage;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("execution(* eu.wdaqua.qanary..*(..))")
    public void allExecutions() {

    }

    @Pointcut("execution(public eu.wdaqua.qanary.commons.QanaryMessage process(eu.wdaqua.qanary.    commons.QanaryMessage))")
    public void processExecution() {
    }

    private URI currentProcessGraph;

    @Around(value = "allExecutions()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();
        logger.info(">> {}() - {}", methodName, Arrays.toString(args));
        logger.info("<< {}() - {}", "a");
        Object result = joinPoint.proceed();
        return result;
    }

    @Around(value = "processExecution()" )
    public Object logAroundProcess(ProceedingJoinPoint joinPoint) throws Throwable {
        QanaryMessage qanaryMessage = (QanaryMessage)  joinPoint.getArgs()[0];
        this.currentProcessGraph = qanaryMessage.getInGraph();
        logger.info(">>> Method: {}", joinPoint.toLongString());
        return joinPoint.proceed();
    }








}
