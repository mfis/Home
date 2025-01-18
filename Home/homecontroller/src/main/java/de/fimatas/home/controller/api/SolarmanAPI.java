package de.fimatas.home.controller.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.fimatas.home.controller.dao.TokenDAO;
import de.fimatas.home.controller.model.IllegalPvCollectionTimeException;
import de.fimatas.home.library.util.HomeAppConstants;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Component
@CommonsLog
public class SolarmanAPI {

    @Autowired
    private ExternalServiceHttpAPI externalServiceHttpAPI;

    @Autowired
    private Environment env;

    private static final ObjectMapper jsonObjectMapper = new ObjectMapper();

    private static final String PATH_LOGIN = "/account/v1.0/token";

    private static final String PATH_CURRENT_DATA = "/device/v1.0/currentData";

    private static int accessFailureCounter = 0;

    private static int loginCounter = 0;

    @CircuitBreaker(name = "solarman", fallbackMethod = "fallbackResponse")
    public JsonNode callForCurrentData(){

        if(accessFailureCounter > 1 || loginCounter > 1 || !Boolean.parseBoolean(env.getProperty("solarman.enabled"))){
            return null;
        }

        // first try to load current data
        try{
            return currentData();
        } catch (IllegalPvCollectionTimeException ipcte){
            throw ipcte;
        } catch (IllegalStateException | IllegalAccessException e) {
            log.warn("first call (access): " + e.getMessage());
        } catch (Exception e){
            log.error("first call (other): " + e.getMessage());
            throw new RuntimeException(e);
        }

        // if login failure occured, try to login
        try{
            loginCounter++;
            log.warn("solarman login");
            login();
        } catch (IllegalStateException | IllegalAccessException e){
            log.error("login call (access): " + e.getMessage());
            accessFailureCounter++;
            throw new RuntimeException(e);
        } catch (Exception e){
            log.error("login call (other): " + e.getMessage());
            throw new RuntimeException(e);
        }

        // then try to load current data second time
        try{
            return currentData();
        } catch (IllegalPvCollectionTimeException ipcte){
            throw ipcte;
        } catch (IllegalStateException | IllegalAccessException e){
            log.error("second call (access): " + e.getMessage());
            throw new RuntimeException(e);
        } catch (Exception e){
            log.error("second call (other): " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused") // used by resilience4j
    public JsonNode fallbackResponse(Throwable t) {
        return null;
    }

    private void login() throws Exception {

        Map<String, String> uri = new HashMap<>();
        uri.put("appId", env.getProperty("solarman.appId"));
        String url = env.getProperty("solarman.hostname") + PATH_LOGIN +  "?appId={appId}";
        ObjectNode requestJsonNode = jsonObjectMapper.createObjectNode();
        requestJsonNode.put("appSecret", env.getProperty("solarman.appSecret"));
        requestJsonNode.put("email", env.getProperty("solarman.email"));
        requestJsonNode.put("password", env.getProperty("solarman.password"));
        String requestJsonString = jsonObjectMapper.writeValueAsString(requestJsonNode);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(requestJsonString.length());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestJsonString, headers);

        ResponseEntity<String> responseEntity = externalServiceHttpAPI.postForEntity(url, requestEntity, uri);
        final JsonNode response = handleResponse(responseEntity, false);
        TokenDAO.getInstance().write("solarman", "bearer", response.get("access_token").asText());
        TokenDAO.getInstance().persist();
    }

    private JsonNode currentData() throws Exception {

        String bearer = TokenDAO.getInstance().read("solarman", "bearer");
        if(StringUtils.isBlank(bearer)){
            throw new IllegalStateException("no bearer token set");
        }

        Map<String, String> uri = new HashMap<>();
        uri.put("language", "en");
        String url = env.getProperty("solarman.hostname") + PATH_CURRENT_DATA +  "?language={language}";
        ObjectNode requestJsonNode = jsonObjectMapper.createObjectNode();
        requestJsonNode.put("deviceSn", env.getProperty("solarman.device"));
        String requestJsonString = jsonObjectMapper.writeValueAsString(requestJsonNode);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(requestJsonString.length());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestJsonString, headers);

        ResponseEntity<String> responseEntity = externalServiceHttpAPI.postForEntity(url, requestEntity, uri);
        return handleResponse(responseEntity, true);
    }

    private static JsonNode handleResponse(ResponseEntity<String> responseEntity, boolean checkCollectionTime) throws IllegalAccessException, JsonProcessingException {

        if(responseEntity.getStatusCode() == FORBIDDEN || responseEntity.getStatusCode() == UNAUTHORIZED){
            throw new IllegalAccessException("response code " + responseEntity.getStatusCode());
        }
        final JsonNode tree = jsonObjectMapper.readTree(responseEntity.getBody());
        if(tree.get("success").asText().equalsIgnoreCase("false")){
            throw new IllegalStateException("call not successful: " + tree.get("msg").asText());
        }

        if(checkCollectionTime){
            var collectionTime = Long.parseLong(tree.get("collectionTime").asText()) * 1000L;
            Instant collectionTimeInstant = Instant.ofEpochMilli(collectionTime);
            var collectionTimeDurationSeconds = Duration.between(collectionTimeInstant, Instant.now()).toSeconds();
            if(collectionTimeDurationSeconds > HomeAppConstants.MODEL_PV_OUTDATED_SECONDS){
                throw new IllegalPvCollectionTimeException("pv data too old: " + (collectionTimeDurationSeconds / 60) + " minutes");
            }
        }

        return tree;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void resetCounter() {
        accessFailureCounter = 0;
        loginCounter = 0;
    }

}
