package de.fimatas.home.adapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource(value = "classpath:application.properties", encoding = "UTF-8")
@PropertySource(value = "file:/Users/mfi/documents/config/homeadapter.properties", encoding = "UTF-8", ignoreResourceNotFound = true)
@PropertySource(value = "file:/home/homeadapter/homeadapter.properties", encoding = "UTF-8", ignoreResourceNotFound = true)
public class HomeAdapterApplication {
    public static void main(String[] args) {
        SpringApplication.run(HomeAdapterApplication.class, args);
    }
}
