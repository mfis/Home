package de.fimatas.home.controller.api;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.fimatas.home.library.util.HomeAppConstants;

@Component
public class HueAPI {

    @Autowired
    private Environment env;

    @Autowired
    @Qualifier("restTemplate")
    private RestTemplate restTemplateHue;

    private String host;

    private String hueUser;

    private long resourceNotAvailableCounter;

    //

    private JsonNode currentObject = null;

    private LocalDateTime currentObjectTimestamp;

    //

    private static final ObjectMapper jsonObjectMapper = new ObjectMapper();

    private static final String PATH_API = "/api/"; // NOSONAR

    private static final Log LOG = LogFactory.getLog(HueAPI.class);

    @PostConstruct
    public void init() {

        hueUser = env.getProperty("hue.authuser");
        host = env.getProperty("hue.hostName");

        Assert.notNull(hueUser, "ccuUser not set");
        Assert.notNull(host, "host not set");
    }

    public synchronized boolean refresh() {

        String path = host + PATH_API + hueUser;

        JsonNode response = callHueAPI(path, Optional.empty(), true);
        return response != null;
    }

    public JsonNode getRootNode() {
        return currentObject;
    }

    private HttpHeaders createHeaders(int contentLength) {

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CACHE_CONTROL, "no-cache");
        httpHeaders.setAccept(Arrays.asList(MediaType.ALL));
        httpHeaders.setAcceptCharset(List.of(StandardCharsets.UTF_8));
        httpHeaders.setCacheControl(CacheControl.noCache());
        httpHeaders.setContentLength(contentLength);
        return httpHeaders;
    }

    private JsonNode callHueAPI(String path, Optional<String> body, boolean refresh) {

        ResponseEntity<String> responseEntity = null;
        try {
            if (body.isPresent()) {
                HttpHeaders headers = createHeaders(body.get().length());
                responseEntity = restTemplateHue.postForEntity(path, new HttpEntity<>(body, headers), String.class);
            } else {
                responseEntity = restTemplateHue.getForEntity(path, String.class);
            }
            resourceNotAvailableCounter = 0;
        } catch (Exception e) {
            if (handleRequestException()) {
                throw e;
            }
            return null;
        }

        HttpStatus statusCode = responseEntity.getStatusCode();
        if (!statusCode.is2xxSuccessful()) {
            LOG.error("Could not successful call hue API. RC=" + statusCode.value());
        }
        Assert.notNull(responseEntity.getBody(), "hue response is empty!");
        if (LOG.isTraceEnabled()) {
            LOG.trace("REQUEST:\n" + body + "\nRESPONSE:\n" + responseEntity.getBody());
        }

        try {
            JsonNode jsonTree = jsonObjectMapper.readTree(responseEntity.getBody());
            normalizeTimestamps(jsonTree);

            if (ignoreEqualResponse(refresh, jsonTree)) {
                return null;
            }

            return jsonTree;
        } catch (Exception e) {
            throw new IllegalStateException("Error parsing object", e);
        }
    }

    private void normalizeTimestamps(JsonNode jsonTree) {

        ObjectNode config = (ObjectNode) jsonTree.path("config");
        config.put("UTC", StringUtils.EMPTY);
        config.put("localtime", StringUtils.EMPTY);
        config.put("localtime", StringUtils.EMPTY);

        JsonNode whitelist = config.path("whitelist");
        for (JsonNode user : whitelist) {
            ((ObjectNode) user).put("last use date", StringUtils.EMPTY);
        }
    }

    private boolean ignoreEqualResponse(boolean refresh, JsonNode newObject) {

        if (!refresh) {
            return false;
        }

        if (newObject.equals(currentObject)) {
            if (ChronoUnit.SECONDS.between(currentObjectTimestamp,
                LocalDateTime.now()) < HomeAppConstants.MODEL_MAX_UPDATE_INTERVAL_SECONDS) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Response is equal to previous response AND model is still actual. -> NOT returning response.");
                }
                return true;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Response is equal to previous response BUT model is outdated. -> Returning response.");
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Response is NOT equal to previous response. -> Returning response.");
            }
        }

        currentObject = newObject;
        currentObjectTimestamp = LocalDateTime.now();
        return false;
    }

    public boolean handleRequestException() {
        resourceNotAvailableCounter++;
        return resourceNotAvailableCounter > 2;
    }

}
