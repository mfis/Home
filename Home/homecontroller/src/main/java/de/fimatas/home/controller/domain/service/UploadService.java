package de.fimatas.home.controller.domain.service;

import java.util.Arrays;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import de.fimatas.home.library.util.HomeAppConstants;

@Component
public class UploadService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Environment env;

    private static Log log = LogFactory.getLog(UploadService.class);

    private long resourceNotAvailableCounter;

    public void upload(Object object) {
        String host = env.getProperty("client.hostName");
        uploadBinary(host + "/upload" + object.getClass().getSimpleName(), object.getClass(), object);
    }

    private <T> T uploadBinary(String url, T t, Object instance) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cache-Control", "no-cache");
        headers.set(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN,
            env.getProperty(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN));

        String plainClientCredentials = env.getProperty("client.auth.user") + ":" + env.getProperty("client.auth.pass");
        String base64ClientCredentials = new String(Base64.encodeBase64(plainClientCredentials.getBytes()));
        headers.set("Authorization", "Basic " + base64ClientCredentials);

        try {
            @SuppressWarnings("unchecked")
            HttpEntity<T> request = new HttpEntity<>((T) instance, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            HttpStatus statusCode = response.getStatusCode();

            connectionEstablishedLogging();
            if (!statusCode.is2xxSuccessful()) {
                LogFactory.getLog(UploadService.class).error("Could not successful upload data. RC=" + statusCode.value());
            }

        } catch (ResourceAccessException | HttpServerErrorException e) {
            connectionNotEstablishedLogging(e);
        }
        return t;
    }

    private void connectionNotEstablishedLogging(RestClientException e) {
        resourceNotAvailableCounter++;
        String suppressLogEntries = resourceNotAvailableCounter == 5 ? " NO FURTHER LOG ENTRIES WILL BE WRITTEN." : "";
        if (resourceNotAvailableCounter < 4) {
            log.warn("Could not upload state (#" + resourceNotAvailableCounter + "). "
                + (e.getMessage() != null ? e.getMessage().replace('\r', ' ').replace('\n', ' ') : "") + suppressLogEntries); // NOSONAR
        }
    }

    private void connectionEstablishedLogging() {
        if (resourceNotAvailableCounter > 0) {
            log.warn("upload state successful after " + resourceNotAvailableCounter + " times.");
            resourceNotAvailableCounter = 0;
        }
    }

}
