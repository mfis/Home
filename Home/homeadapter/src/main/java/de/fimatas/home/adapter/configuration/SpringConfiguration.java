package de.fimatas.home.adapter.configuration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class SpringConfiguration {

    @Bean(name = "restTemplateFirewallConfigurationCheck")
    public RestTemplate restTemplateFirewallConfigurationCheck(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.setConnectTimeout(Duration.ofMillis(1500)).setReadTimeout(Duration.ofMillis(1500)).build();
    }
}
