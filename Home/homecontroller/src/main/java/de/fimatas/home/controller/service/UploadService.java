package de.fimatas.home.controller.service;

import de.fimatas.home.library.util.HomeAppConstants;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class UploadService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Environment env;

    @Value("${application.homeAdapterEnabled:false}")
    private boolean homeAdapterEnabled;

    private final static Log log = LogFactory.getLog(UploadService.class);

    private final Map<String, Long> resourceNotAvailableCounter = new HashMap<>();

    @PostConstruct
    public void init() {
        log.info("homeAdapterEnabled=" + homeAdapterEnabled);
    }

    public void uploadToClient(Object object) {
        String host = env.getProperty("client.hostName");
        uploadBinaryToClient(host + "/upload" + object.getClass().getSimpleName(), object, true);
    }

    @Async
    public void uploadToAdapter(Object object) {
        if(homeAdapterEnabled){
            uploadBinaryToClient("http://localhost:8097/upload" + object.getClass().getSimpleName(), object, false);
        }
    }

    private <T> void uploadBinaryToClient(String url, Object instance, boolean credentials) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cache-Control", "no-cache");

        if(credentials) {
            headers.set(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN,
                    env.getProperty(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN));
            String plainClientCredentials = env.getProperty("client.auth.user") + ":" + env.getProperty("client.auth.pass");
            String base64ClientCredentials = new String(Base64.encodeBase64(plainClientCredentials.getBytes()));
            headers.set("Authorization", "Basic " + base64ClientCredentials);
        }

        try {
            @SuppressWarnings("unchecked")
            HttpEntity<T> request = new HttpEntity<>((T) instance, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            HttpStatus statusCode = response.getStatusCode();

            connectionEstablishedLogging(url);
            if (!statusCode.is2xxSuccessful()) {
                LogFactory.getLog(UploadService.class).error("Could not successful upload data. RC=" + statusCode.value());
            }

        } catch (ResourceAccessException | HttpServerErrorException | HttpClientErrorException e) {
            connectionNotEstablishedLogging(url, e);
        }
    }

    private void connectionNotEstablishedLogging(String url, RestClientException e) {
        resourceNotAvailableCounter.putIfAbsent(url, 0L);
        resourceNotAvailableCounter.put(url, resourceNotAvailableCounter.get(url) + 1);
        String suppressLogEntries = resourceNotAvailableCounter.get(url) == 5 ? " NO FURTHER LOG ENTRIES WILL BE WRITTEN." : "";
        if (resourceNotAvailableCounter.get(url) < 4) {
            log.warn("Could not upload state (#" + resourceNotAvailableCounter.get(url) + "). "
                + (e.getMessage() != null ? e.getMessage().replace('\r', ' ').replace('\n', ' ') : "") + suppressLogEntries); // NOSONAR
        }
    }

    private void connectionEstablishedLogging(String url) {
        if (resourceNotAvailableCounter.containsKey(url) && resourceNotAvailableCounter.get(url) > 0) {
            log.warn("upload state successful after " + resourceNotAvailableCounter.get(url) + " times.");
            resourceNotAvailableCounter.put(url, 0L);
        }
    }

}
