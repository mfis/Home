package de.fimatas.home.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@ComponentScan(basePackages = {"de.fimatas.home", "mfi.files"})

@PropertySource(value = "classpath:application.properties")
@PropertySource(value = "file:/Users/mfi/documents/config/homeapp.properties", ignoreResourceNotFound = true)
@PropertySource(value = "file:/home/homeapp/documents/config/homeapp.properties", ignoreResourceNotFound = true)
public class HomeClientApplication { // NOSONAR

    public static void main(String[] args) {
        SpringApplication.run(HomeClientApplication.class, args);
    }
}
