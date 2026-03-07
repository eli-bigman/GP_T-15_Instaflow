package com.insightflow.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /** Monitors all controller methods — logs entry, exit, and execution time. */
    @Around("execution(* com.insightflow.controller..*(..))")
    public Object monitorController(ProceedingJoinPoint joinPoint) throws Throwable {
        String cls = joinPoint.getTarget().getClass().getSimpleName();
        String method = joinPoint.getSignature().getName();
        long start = System.currentTimeMillis();

        log.info("[REQUEST]  {}.{}()", cls, method);
        try {
            Object result = joinPoint.proceed();
            log.info("[RESPONSE] {}.{}() completed in {}ms", cls, method, elapsed(start));
            return result;
        } catch (Throwable ex) {
            log.error("[FAILED]   {}.{}() threw {} after {}ms — {}",
                    cls, method, ex.getClass().getSimpleName(), elapsed(start), ex.getMessage());
            throw ex;
        }
    }

    /** Monitors all service methods — logs execution time at DEBUG level. */
    @Around("execution(* com.insightflow.service..*(..))")
    public Object monitorService(ProceedingJoinPoint joinPoint) throws Throwable {
        String cls = joinPoint.getTarget().getClass().getSimpleName();
        String method = joinPoint.getSignature().getName();
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            log.debug("[SERVICE]  {}.{}() — {}ms", cls, method, elapsed(start));
            return result;
        } catch (Throwable ex) {
            log.error("[SERVICE]  {}.{}() threw {} — {}ms",
                    cls, method, ex.getClass().getSimpleName(), elapsed(start));
            throw ex;
        }
    }

    private long elapsed(long startMs) {
        return System.currentTimeMillis() - startMs;
    }
}
