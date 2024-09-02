package com.dimon.catanbackend.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("execution(* com.dimon.catanbackend.controller.*.*(..))")
    private void forController() {}

    @Before("forController()")
    public void logBefore(JoinPoint theJoinPoint) {
        logger.info("Entering method: {} with arguments: {}", theJoinPoint.getSignature().toShortString(), theJoinPoint.getArgs());
    }

    @AfterReturning(pointcut = "forController()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        logger.info("Exiting method: {} with result: {}", joinPoint.getSignature().toShortString(), result);
    }

    @AfterThrowing(pointcut = "forController()", throwing = "error")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable error) {
        logger.error("Exception in method: {} with cause: {}", joinPoint.getSignature().toShortString(), error.getMessage());
    }
}
