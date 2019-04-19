package homecontroller.service;

import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SpringConfiguration {

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(10))
				.setReadTimeout(Duration.ofSeconds(10)).build();
	}

	@Bean
	public RestTemplate restTemplateLowTimeout(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.setConnectTimeout(Duration.ofMillis(300))
				.setReadTimeout(Duration.ofMillis(300)).build();
	}
}
