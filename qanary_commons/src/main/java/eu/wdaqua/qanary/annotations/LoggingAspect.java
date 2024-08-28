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
    private URI currentProcessGraph;

    @Pointcut("execution(* eu.wdaqua.qanary.component..*(..))")
    public void allExecutionsInComponent() {

    }

    @Around(value = "allExecutions()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();
        logger.info(">> {}() - {}", methodName, Arrays.toString(args));
        logger.info("<< {}() - {}", "a");
        Object result = joinPoint.proceed();
        return result;
    }

    @Around("execution(* *(..)) && @within(LogExecution)")
    public Object logAroundWithAnnotation(ProceedingJoinPoint joinPoint) throws Throwable {

        Object result = joinPoint.proceed();
        return result;
    }

}
