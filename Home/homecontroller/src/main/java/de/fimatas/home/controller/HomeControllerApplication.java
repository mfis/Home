package de.fimatas.home.controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
@ComponentScan(basePackages = {"de.fimatas.home", "mfi.files"})

@PropertySource(value = "classpath:application.properties", encoding = "UTF-8")
@PropertySource(value = "file:/Users/mfi/documents/config/homecontroller/homecontroller.properties", encoding = "UTF-8", ignoreResourceNotFound = true)
@PropertySource(value = "file:/opt/homecontroller/homecontroller.properties", encoding = "UTF-8", ignoreResourceNotFound = true)
@PropertySource(value = "file:/Users/mfi/documents/config/homecontroller/homecontrollercredentials.properties", encoding = "UTF-8", ignoreResourceNotFound = true)
@PropertySource(value = "file:/opt/homecontroller/homecontrollercredentials.properties", encoding = "UTF-8", ignoreResourceNotFound = true)
public class HomeControllerApplication { // NOSONAR
    public static void main(String[] args) {
        SpringApplication.run(HomeControllerApplication.class, args);
    }
}
