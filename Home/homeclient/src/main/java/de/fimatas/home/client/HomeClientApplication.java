package de.fimatas.home.client;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@ComponentScan(basePackages = {"de.fimatas.home", "de.fimatas.users"})
@CommonsLog
@PropertySource(value = "classpath:application.properties")
@PropertySource(value = "file:/Users/mfi/documents/config/homeapp.properties", ignoreResourceNotFound = true)
@PropertySource(value = "file:/home/homeapp/documents/config/homeapp.properties", ignoreResourceNotFound = true)
public class HomeClientApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(HomeClientApplication.class);
        app.addListeners((ApplicationListener<ApplicationStartedEvent>) event -> {
            log.info("Spring Boot Version: " + SpringBootVersion.getVersion());
        });
        app.run(args);
    }
}
