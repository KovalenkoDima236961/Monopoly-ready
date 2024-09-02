package com.dimon.catanbackend.aspects;

import com.dimon.catanbackend.utils.api.ApiAnalyticsService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ApiAnalyticsAspect {

    @Autowired
    private ApiAnalyticsService apiAnalyticsService;

    /**
     * This method is an around advice that intercepts the execution of any method
     * within the `com.dimon.catanbackend.controller` package and its subpackages.
     * It logs analytics data such as the time taken to execute the method and whether
     * the method completed successfully.
     *
     * @param joinPoint Provides reflective access to the method being intercepted.
     * @return The result of the method execution.
     * @throws Throwable if the intercepted method throws an exception.
     */
    @Around("execution(* com.dimon.catanbackend.controller..*(..))")
    public Object logAnalytics(ProceedingJoinPoint joinPoint) throws Throwable {

        long startTime = System.currentTimeMillis();
        boolean success = false;

        try {
            Object result = joinPoint.proceed();
            success = true;
            return result;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String endpoint = joinPoint.getSignature().toShortString();
            apiAnalyticsService.recordApiCall(endpoint, duration, success);
        }
    }
}
