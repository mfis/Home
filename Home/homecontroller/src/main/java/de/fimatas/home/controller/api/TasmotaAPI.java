package de.fimatas.home.controller.api;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.fimatas.home.library.domain.model.HeatpumpRoofPreset;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.NoRouteToHostException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@CommonsLog
public class TasmotaAPI {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final RetryTemplate retryTemplate;

    @Value("${heatpump.roof.tasmota.hostnamePrefix}")
    private String hostnamePrefix;
    @Value("${heatpump.roof.tasmota.vendor}")
    private String vendor;
    @Value("${heatpump.roof.mock:false}")
    private boolean mock;

    public TasmotaAPI(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();

        RetryPolicy retryPolicy = RetryPolicy.builder()
                .maxRetries(1)
                .delay(Duration.ofMillis(1000))
                .includes(NoRouteToHostException.class)
                .build();

        this.retryTemplate = new RetryTemplate(retryPolicy);
    }

    public Map<String, Boolean> call(Map<String, HeatpumpRoofPreset> preset) {

        var responses = new HashMap<String, Boolean>();
        preset.forEach((roomname, program) -> {
            boolean ok;
            try {
                var request = new IrHvac();
                request.setVendor(vendor);
                request.setPower(program.isExpectedOnOffState() ? "On" : "Off");
                request.setMode(program.getExpectedMode() == null ? null : program.getExpectedMode().getValue());
                request.setTemp(program.getExpectedTemperature() == null? null : program.getExpectedTemperature());
                request.setFanSpeed(program.getFanSpeed() == null ? null : program.getFanSpeed().getValue());

                if(mock){
                    log.info("CALL MOCK: " + request);
                    Thread.sleep(2000L);
                    ok = true;
                }else{
                    var response = retryTemplate.execute(() -> sendAcCommand(request, roomname));
                    ok = response != null && response.getIrHvac() != null && response.getIrHvac().equals(request);
                    if(!ok) {
                        log.warn("HeatpumpRoof " + roomname + " command failed: request = " + request + " respronse = " + response);
                    }
                }

            }catch(Exception ex){
                ok = false;
                log.error("error calling tasmota: ", ex);
            }
            responses.put(roomname, ok);
        });
        return responses;
    }

    @SneakyThrows
    private TasmotaResponse sendAcCommand(IrHvac requestDto, String roomname) {

        String jsonPayload = objectMapper.writeValueAsString(requestDto);
        String command = "IRhvac " + jsonPayload;

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host(hostnamePrefix + roomname)
                        .path("/cm")
                        .queryParam("cmnd", "{cmd}")
                        .build(command))
                .retrieve()
                .body(TasmotaResponse.class);
    }

    @Data
    @EqualsAndHashCode
    @ToString
    public static class IrHvac {
        @JsonProperty("Vendor")
        private String vendor;
        @JsonProperty("Power")
        private String power;
        @JsonProperty("Mode")
        private String mode;
        @JsonProperty("Temp")
        private Integer temp;
        @JsonProperty("FanSpeed")
        private String fanSpeed;
    }

    @Data
    public static class TasmotaResponse {
        @JsonProperty("IRHVAC")
        private IrHvac irHvac;
    }
}