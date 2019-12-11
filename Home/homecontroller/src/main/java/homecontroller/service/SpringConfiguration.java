package homecontroller.service;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import homecontroller.util.HomeAppConstants;

@Configuration
public class SpringConfiguration implements WebMvcConfigurer {

	@Bean
	public ByteArrayHttpMessageConverter byteArrayHttpMessageConverter() {
		return new ByteArrayHttpMessageConverter();
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(10))
				.setReadTimeout(Duration.ofSeconds(10)).build();
	}

	@Bean(name = "restTemplateBinaryResponse")
	public RestTemplate restTemplateBinaryResponse(RestTemplateBuilder restTemplateBuilder,
			List<HttpMessageConverter<?>> messageConverters) {
		return restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(5))
				.setReadTimeout(Duration.ofSeconds(20)).additionalMessageConverters(messageConverters)
				.build();
	}

	@Bean(name = "restTemplateLowTimeout")
	public RestTemplate restTemplateLowTimeout(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.setConnectTimeout(Duration.ofMillis(500))
				.setReadTimeout(Duration.ofMillis(500)).build();
	}

	@Bean(name = "restTemplateLongPolling")
	public RestTemplate restTemplateLongPolling(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(10)).setReadTimeout(Duration
				.ofSeconds(HomeAppConstants.CONTROLLER_CLIENT_LONGPOLLING_REQUEST_TIMEOUT_SECONDS * 2L))
				.build();
	}

}
