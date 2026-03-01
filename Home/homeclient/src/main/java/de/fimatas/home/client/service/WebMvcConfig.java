package de.fimatas.home.client.service;

import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${appdistribution.file.path}")
    private String appdistributionFilePath;

    @Value("${appdistribution.web.url}")
    private String appdistributionWebUrl;


    @Bean
    public ByteArrayHttpMessageConverter byteArrayHttpMessageConverter() {
        ByteArrayHttpMessageConverter arrayHttpMessageConverter = new ByteArrayHttpMessageConverter();
        arrayHttpMessageConverter.setSupportedMediaTypes(getSupportedMediaTypesForByteArrayHttpMessageConverter());
        return arrayHttpMessageConverter;
    }

    private List<MediaType> getSupportedMediaTypesForByteArrayHttpMessageConverter() {
        List<MediaType> list = new ArrayList<>();
        list.add(MediaType.IMAGE_JPEG);
        list.add(MediaType.IMAGE_PNG);
        list.add(MediaType.APPLICATION_OCTET_STREAM);
        return list;
    }

   @Override
    public void addResourceHandlers(@Nonnull ResourceHandlerRegistry registry) {

        URL appdistributionUrl;
        try {
            appdistributionUrl = new URL(appdistributionWebUrl);
        } catch (MalformedURLException e) {
           throw new IllegalArgumentException("Malformed 'appdistribution.web.url' URL!");
        }

        registry.addResourceHandler(appdistributionUrl.getPath() + "**")
                .addResourceLocations("file:" + appdistributionFilePath)
                .setCacheControl(CacheControl.noCache());
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.connectTimeout(Duration.ofSeconds(8)).readTimeout(Duration.ofSeconds(8)).build();
    }

    @Bean
    public LoginInterceptor pagePopulationInterceptor() {
        return new LoginInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(pagePopulationInterceptor());
    }
}
