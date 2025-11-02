package de.fimatas.home.controller.api;

import de.fimatas.heatpump.basement.driver.api.Request;
import de.fimatas.heatpump.basement.driver.api.Response;
import de.fimatas.heatpump.roof.driver.api.HeatpumpRequest;
import de.fimatas.heatpump.roof.driver.api.HeatpumpResponse;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Component
@CommonsLog
public class ExternalServiceHttpAPI {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("restTemplateHeatpumpDriver")
    private RestTemplate restTemplateHeatpumpDriver;

    @Value("${application.externalServicesEnabled:false}")
    private boolean externalServicesEnabled;

    @Autowired
    private Environment environment;

    private final Map<String, LocalDateTime> lastUrlRequestCall = new HashMap<>();

    public static final String MESSAGE_TOO_MANY_CALLS = "Too many calls to url: ";

    private final Map<String, Duration> MAX_CALL_RATE_MAP = new HashMap<>();
    private final Duration MAX_CALL_RATE_DEFAULT = Duration.ofSeconds(58);

    @PostConstruct
    public void init(){
        MAX_CALL_RATE_MAP.put(hostAndPath(environment.getProperty("heatpump.roof.driver.url")).host(), Duration.ofMinutes(1));
        MAX_CALL_RATE_MAP.put(hostAndPath(environment.getProperty("heatpump.basement.driver.url")).host(), Duration.ofMinutes(14));
        MAX_CALL_RATE_MAP.put(hostAndPath(environment.getProperty("weatherForecast.brightskyEndpoint")).host(), Duration.ofMinutes(55));
        MAX_CALL_RATE_MAP.put(hostAndPath(environment.getProperty("solarman.hostname")).host(), Duration.ofSeconds(58));
    }

    @Scheduled(cron = "0 0 0 * * *")
    protected void resetLastUrlRequestCall() {
        lastUrlRequestCall.clear();
    }

    public ResponseEntity<String> getForEntity(String url, Map<String, ?> uriVariables) {
        checkServiceEnabledAndFrequency(url, uriVariables, "GET");
        return restTemplate.getForEntity(url, String.class, uriVariables);
    }

    public ResponseEntity<String> postForEntity(String url, @Nullable Object request, Map<String, ?> uriVariables) {
        checkServiceEnabledAndFrequency(url, uriVariables, "POST");
        return restTemplate.postForEntity(url, request, String.class, uriVariables);
    }

    public synchronized ResponseEntity<HeatpumpResponse> postForHeatpumpRoofEntity(String url, HeatpumpRequest request) throws RestClientException {
        if(HeatpumpRequest.apiVersion != 4){
            throw new IllegalStateException("Heatpump API version not supported");
        }
        if(!request.isReadFromCache()){
            var map = Map.of("type", (request.getWriteWithRoomnameAndProgram().isEmpty() ? "read" : "write"));
            checkServiceEnabledAndFrequency(url, map, "POST");
        }
        HttpEntity<HeatpumpRequest> httpRequest = new HttpEntity<>(request);
        return restTemplateHeatpumpDriver.postForEntity(url, httpRequest, HeatpumpResponse.class);
    }

    public synchronized ResponseEntity<Response> postForHeatpumpBasementEntity(String url, Request request) throws RestClientException {
        if(!request.isReadFromCache()){
            checkServiceEnabledAndFrequency(url, Map.of("read", "read"), "POST");
        }
        HttpEntity<Request> httpRequest = new HttpEntity<>(request);
        return restTemplateHeatpumpDriver.postForEntity(url, httpRequest, Response.class);
    }

    private void checkServiceEnabledAndFrequency(String url, Map<String, ?> uriVariables, String method) {

        var hostAndPath = hostAndPath(url);

        if(!externalServicesEnabled) {
            throw new RestClientException("External services are disabled: " + hostAndPath.host());
        }

        var lastCallKey = method + " " + url + "#" + generateHash(uriVariables);
        var value = lastUrlRequestCall.get(lastCallKey);

        var maxRate = MAX_CALL_RATE_MAP.getOrDefault(hostAndPath.host(), MAX_CALL_RATE_DEFAULT);
        if(value != null && value.plus(maxRate).isAfter(LocalDateTime.now())){
            throw new RestClientException(MESSAGE_TOO_MANY_CALLS + hostAndPath.host());
        }

        lastUrlRequestCall.put(lastCallKey, LocalDateTime.now());
        log.info("CALLING external service (" + method + "): " + hostAndPath.host() + hostAndPath.path());
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

    @SneakyThrows
    private HostAndPath hostAndPath(String urlString) {
        var url = new URL(urlString);
        return new HostAndPath(url.getHost(), url.getPath());
    }

    private record HostAndPath(String host, String path) {}
}
