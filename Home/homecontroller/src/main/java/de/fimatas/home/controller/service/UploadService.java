package de.fimatas.home.controller.service;

import de.fimatas.home.library.util.HomeAppConstants;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.PostConstruct;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.*;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@CommonsLog
public class UploadService {

    @Autowired
    @Qualifier("restTemplateModelUpload")
    private RestTemplate restTemplateModelUpload;

    private final Environment env;

    @Value("${application.homeClientEnabled:false}")
    private boolean homeClientEnabled;

    private final Map<String, Long> resourceNotAvailableCounter = new HashMap<>();

    private final RestClient backupFileRestClient;

    public UploadService(RestClient.Builder builder, Environment env) {

        this.env = env;

        var requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofMinutes(5));

        this.backupFileRestClient = builder
                .baseUrl(Objects.requireNonNull(env.getProperty("client.hostName")))
                .requestFactory(requestFactory)
                .build();

        log.info("homeClientEnabled=" + homeClientEnabled + ", client.hostName=" + env.getProperty("client.hostName"));
    }

    @PostConstruct
    public void init() {

    }

    @CircuitBreaker(name = "upload", fallbackMethod = "fallbackResponse")
    public void uploadToClient(Object object) {
        if(homeClientEnabled){
            String host = env.getProperty("client.hostName");
            uploadBinaryToClient(host + "/upload" + object.getClass().getSimpleName(), object, true);
        }
    }

    public void uploadBackupFile(File file) {

        var body = new LinkedMultiValueMap<String, Object>();
        body.add("file", new FileSystemResource(file));

        backupFileRestClient.post()
                .uri("/uploadBackupFileMultipart")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN, env.getProperty(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN))
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unused") // used by resilience4j
    public void fallbackResponse(Throwable t) {
        // noop
    }

    private <T> void uploadBinaryToClient(String url, Object instance, boolean credentials) {

        String modelName =  instance.getClass().getSimpleName();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cache-Control", "no-cache");

        if(credentials) {
            headers.set(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN,
                    env.getProperty(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN));
        }

        try {
            long l1 = System.nanoTime();
            @SuppressWarnings("unchecked")
            HttpEntity<T> request = new HttpEntity<>((T) instance, headers);
            ResponseEntity<String> response = restTemplateModelUpload.postForEntity(url, request, String.class);
            HttpStatusCode statusCode = response.getStatusCode();

            connectionEstablishedLogging(modelName);
            if (!statusCode.is2xxSuccessful()) {
                LogFactory.getLog(UploadService.class).error("Could not successful upload data. RC=" + statusCode.value());
            }

            long l2 = System.nanoTime();
            long ldiff = (l2 - l1) / 1000000; // ms
            if(ldiff > 3000){
                log.warn("uploadBinaryToClient " + modelName + ": " + ldiff + " ms!");
            }

        } catch (ResourceAccessException | HttpServerErrorException | HttpClientErrorException e) {
            connectionNotEstablishedLogging(modelName, e);
        }
    }

    private void connectionNotEstablishedLogging(String name, RestClientException e) {
        resourceNotAvailableCounter.putIfAbsent(name, 0L);
        resourceNotAvailableCounter.put(name, resourceNotAvailableCounter.get(name) + 1);
        String suppressLogEntries = resourceNotAvailableCounter.get(name) == 5 ? " NO FURTHER LOG ENTRIES WILL BE WRITTEN." : "";
        if (resourceNotAvailableCounter.get(name) < 4) {
            log.warn("Could not upload state (#" + resourceNotAvailableCounter.get(name) + "). " + name + " - "
                + (e.getMessage() != null ? e.getMessage().replace('\r', ' ').replace('\n', ' ') : "") + suppressLogEntries); // NOSONAR
        }
    }

    private void connectionEstablishedLogging(String name) {
        if (resourceNotAvailableCounter.containsKey(name) && resourceNotAvailableCounter.get(name) > 0) {
            log.warn("upload state successful after " + resourceNotAvailableCounter.get(name) + " times. " + name);
            resourceNotAvailableCounter.put(name, 0L);
        }
    }

}
