package de.fimatas.home.controller.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

@Component
@CommonsLog
public class BrightSkyAPI {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Environment env;

    private static final ObjectMapper jsonObjectMapper = new ObjectMapper();

    private Map<LocalDate, List<JsonNode>> cacheFurtherDays = null;

    public List<JsonNode> callTwoDays(){

        List<JsonNode> forecasts = new LinkedList<>();
        LocalDate now = LocalDate.now();
        callForDate(now, forecasts);
        callForDate(now.plusDays(1L), forecasts);
        return forecasts;
    }

    public Map<LocalDate, List<JsonNode>> getCachedFurtherDays(){
        if(cacheFurtherDays == null){
            cachingCallForFurtherDays();
        }
        return cacheFurtherDays;
    }

    public void cachingCallForFurtherDays(){

        Map<LocalDate, List<JsonNode>> allDays = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();

        for(int i = 2 ; i < 10 ; i++){
            List<JsonNode> singleDay = new LinkedList<>();
            callForDate(now.plusDays(i), singleDay);
            allDays.put(now.plusDays(i), singleDay);
        }
        cacheFurtherDays = allDays;
    }

    @SneakyThrows
    private void callForDate(LocalDate localDate, List<JsonNode> forecasts) {

        Map<String, String> uri = new HashMap<>();
        uri.put("max_dist", env.getProperty("weatherForecast.maxDist"));
        uri.put("tz", env.getProperty("weatherForecast.timezone"));
        uri.put("units", "dwd");
        uri.put("lat", env.getProperty("weatherForecast.lat"));
        uri.put("lon", env.getProperty("weatherForecast.lon"));
        uri.put("date", localDate.format(ISO_LOCAL_DATE));

        String url = env.getProperty("weatherForecast.brightskyEndpoint")
                + "/weather?max_dist={max_dist}&tz={tz}&units={units}&lat={lat}&lon={lon}&date={date}";

        ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class, uri);

        JsonNode jsonTree = jsonObjectMapper.readTree(responseEntity.getBody());

        if(responseEntity.getStatusCode() != HttpStatus.OK){
            log.error("RC=" + responseEntity.getStatusCode());
        }

        jsonTree.path("weather").forEach(node -> {
            if(localDate.equals(LocalDateTime.parse(node.get("timestamp").asText(), ISO_OFFSET_DATE_TIME).toLocalDate())){
                forecasts.add(node);
            }
        });
    }

}
