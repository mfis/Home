package de.fimatas.home.controller.configuration;

import jakarta.annotation.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
        context = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return context;
    }
}
