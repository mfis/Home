package de.fimatas.home.controller.api;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fimatas.home.controller.model.HeatpumpRoofProgram;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    public TasmotaAPI(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();

        this.retryTemplate = RetryTemplate.builder()
                .maxAttempts(2)
                .fixedBackoff(1000)
                .retryOn(NoRouteToHostException.class)
                .build();
    }

    public Map<String, Boolean> call(Map<String, HeatpumpRoofProgram> programs) {

        var responses = new HashMap<String, Boolean>();
        programs.forEach((roomname, program) -> {
            boolean ok;
            try {
                var request = new IrHvac();
                request.setVendor(vendor);
                request.setPower(program.isExpectedOnOffState() ? "On" : "Off");
                request.setMode(program.getExpectedMode() == null ? null : program.getExpectedMode().getValue());
                request.setTemp(program.getExpectedTemperature() == null? null : program.getExpectedTemperature());
                request.setFanSpeed(program.getFanSpeed() == null ? null : program.getFanSpeed().getValue());

                var response = retryTemplate.execute(context -> sendAcCommand(request, roomname));
                ok = response != null && response.getIrHvac() != null && response.getIrHvac().equals(request);
                if(!ok) {
                    log.warn("HeatpumpRoof " + roomname + " command failed: request = " + request + " respronse = " + response);
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