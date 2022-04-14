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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

@Component
@CommonsLog
public class BrightSkyAPI {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Environment env;

    private static final ObjectMapper jsonObjectMapper = new ObjectMapper();

    public List<JsonNode> call(){

        List<JsonNode> forecasts = new LinkedList<>();
        LocalDate now = LocalDate.now();
        callForDate(now, forecasts);
        callForDate(now.plusDays(1L), forecasts);
        return forecasts;
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

        jsonTree.path("weather").forEach(forecasts::add);
    }

}
