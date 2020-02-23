package homecontroller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@PropertySource(value = "classpath:application.properties", encoding = "UTF-8")
@PropertySource(value = "file:/Users/mfi/documents/config/homecontroller.properties", encoding = "UTF-8", ignoreResourceNotFound = true)
@PropertySource(value = "file:/home/homecontroller/homecontroller.properties", encoding = "UTF-8", ignoreResourceNotFound = true)
@PropertySource(value = "file:///C:/Users/Matthias/home/homecontroller/application.properties", encoding = "UTF-8", ignoreResourceNotFound = true)
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
