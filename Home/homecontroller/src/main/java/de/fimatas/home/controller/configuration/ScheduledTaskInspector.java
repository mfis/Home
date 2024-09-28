package de.fimatas.home.controller.configuration;

import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@CommonsLog
@Component
public class ScheduledTaskInspector {

    @Autowired
    private ScheduledTaskAspect scheduledTaskAspect;

    @Autowired
    private ApplicationContextProvider applicationContextProvider;

    public List<ScheduledTaskInfo> getScheduledTasks() {

        List<ScheduledTaskInfo> taskInfos = new ArrayList<>();
        Map<String, Object> beans = applicationContextProvider.getApplicationContext().getBeansWithAnnotation(Component.class);

        beans.forEach((name, bean) -> {
            Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
            Method[] methods = targetClass.getDeclaredMethods();
            Arrays.stream(methods).forEach(method -> {
                Scheduled scheduled = method.getAnnotation(Scheduled.class);
                if (scheduled != null) {
                    ScheduledTaskInfo taskInfo = new ScheduledTaskInfo();
                    taskInfo.setClassName(targetClass.getSimpleName());
                    taskInfo.setMethodName(method.getName());
                    if(StringUtils.isNotBlank(scheduled.cron())){
                        taskInfo.setType("cron");
                        taskInfo.setValue(scheduled.cron());
                    } else if(scheduled.fixedRate() > 0){
                        taskInfo.setType("rate");
                        taskInfo.setValue((scheduled.fixedRate()) + "ms");
                    } else if(scheduled.fixedDelay() > 0){
                        taskInfo.setType("delay");
                        taskInfo.setValue((scheduled.fixedDelay()) + "ms");
                    }else {
                        taskInfo.setType("unknown");
                        taskInfo.setValue("unknown");
                    }
                    String taskName = targetClass.getSimpleName() + "." + method.getName();
                    taskInfo.setLastExecutionTime(scheduledTaskAspect.getTaskExecutionTimes().get(taskName));

                    taskInfos.add(taskInfo);
                }
            });
        });

        return taskInfos;
    }

    @Data
    public static class ScheduledTaskInfo {
        private String className;
        private String methodName;
        private String type;
        private String value;
        private Long lastExecutionTime;
    }
}
