package homecontroller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@PropertySources({ @PropertySource(value = "classpath:application.properties"),
		@PropertySource(value = "file:/Users/mfi/documents/config/homecontroller.properties", ignoreResourceNotFound = true),
		@PropertySource(value = "file:/home/homecontroller/homecontroller.properties", ignoreResourceNotFound = true) })
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
