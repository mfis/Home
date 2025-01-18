package de.fimatas.home.controller.api;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Component
@CommonsLog
public class ExternalServiceHttpAPI {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${application.externalServicesEnabled:false}")
    private boolean externalServicesEnabled;

    private final Map<String, LocalDateTime> lastUrlRequestCall = new HashMap<>();

    @Scheduled(cron = "0 0 0 * * *")
    protected void resetLastUrlRequestCall() {
        lastUrlRequestCall.clear();
    }

    public ResponseEntity<String> getForEntity(String url, Map<String, ?> uriVariables)
            throws RestClientException {
        checkServiceEnabledAndFrequency(url, uriVariables, "GET");
        return restTemplate.getForEntity(url, String.class, uriVariables);
    }

    public ResponseEntity<String> postForEntity(String url, @Nullable Object request,
                                                Map<String, ?> uriVariables) throws RestClientException {
        checkServiceEnabledAndFrequency(url, uriVariables, "POST");
        return restTemplate.postForEntity(url, request, String.class, uriVariables);
    }

    private void checkServiceEnabledAndFrequency(String url, Map<String, ?> uriVariables, String method) {

        var host = StringUtils.substringBefore(url, "?");

        if(!externalServicesEnabled) {
            throw new RestClientException("External services are disabled: " + host);
        }

        var lastCallKey = method + " " + url + "#" + generateHash(uriVariables);
        var value = lastUrlRequestCall.get(lastCallKey);
        if(value != null && value.plusSeconds(58).isAfter(LocalDateTime.now())){
            throw new RestClientException("Too many calls to url: " + host);
        }

        lastUrlRequestCall.put(lastCallKey, LocalDateTime.now());
        log.info("CALLING external service (" + method + "): " + host);
    }

    private String generateHash(Map<String, ?> map) {

        try {
            Map<String, ?> sortedMap = new TreeMap<>(map);
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, ?> entry : sortedMap.entrySet()) {
                if (!builder.isEmpty()) {
                    builder.append("&");
                }
                builder.append(entry.getKey()).append("=").append(entry.getValue());
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 Algorithm not available", e);
        }
    }

}
