package de.fimatas.home.controller.configuration;

import lombok.Getter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Aspect
@Component
public class ScheduledTaskAspect {

    private final Map<String, Long> taskExecutionTimes = new HashMap<>();

    @Around("@annotation(scheduled)")
    public Object measureExecutionTime(ProceedingJoinPoint pjp, @SuppressWarnings("unused") Scheduled scheduled) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result;
        try {
            result = pjp.proceed();
        } finally {
            long endTime = System.currentTimeMillis();
            String className = pjp.getTarget().getClass().getSimpleName();
            String methodName = pjp.getSignature().getName();
            long executionTime = endTime - startTime;
            String taskName = className + "." + methodName;
            taskExecutionTimes.put(taskName, executionTime);
        }
        return result;
    }
}
