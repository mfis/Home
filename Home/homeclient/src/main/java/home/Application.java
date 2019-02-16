package home;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@PropertySource(value = "classpath:application.properties")
@PropertySource(value = "file:/Users/mfi/documents/config/homeapp.properties", ignoreResourceNotFound = true)
@PropertySource(value = "file:/home/homeapp/documents/config/homeapp.properties", ignoreResourceNotFound = true)
@PropertySource(value = "file:///C:/Users/Matthias/home/homeclient/application.properties", ignoreResourceNotFound = true)
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
