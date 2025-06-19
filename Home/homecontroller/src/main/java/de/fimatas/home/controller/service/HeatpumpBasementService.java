package de.fimatas.home.controller.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fimatas.heatpump.roof.driver.api.*;
import de.fimatas.home.controller.api.ExternalServiceHttpAPI;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
@CommonsLog
public class HeatpumpBasementService {

    @Autowired
    private UploadService uploadService;

    @Autowired
    private PushService pushService;

    @Autowired
    private ExternalServiceHttpAPI externalServiceHttpAPI;

    @Autowired
    private Environment env;

    @Value("${application.externalServicesEnabled:false}")
    private boolean externalServicesEnabled;

    @Value("${application.heatpumpRefreshEnabled:false}")
    private boolean heatpumpRefreshEnabled;

    private boolean isCallError = false; // prevent continous error calls

    @PostConstruct
    public void init() {

    }

    public void scheduledRefreshFromDriverCache() {
        try {
            //refreshHeatpumpModel(true);
        } catch (Exception e) {
            handleException(e, "Could not call heatpumpBasement service (with-cache)");
        }
    }

    //@Scheduled(cron = "44 01 6-23 * * *")
    public void scheduledRefreshFromDriverNoCache() {
        isCallError = false;
        try {
            //refreshHeatpumpModel(false);
        } catch (Exception e) {
            handleException(e, "Could not call heatpumpBasement service (no-cache!)");
        }
    }

    private static void handleException(Exception e, String msg) {
        if(e instanceof RestClientException && e.getMessage().startsWith(ExternalServiceHttpAPI.MESSAGE_TOO_MANY_CALLS)){
            log.warn(msg + " - " + e.getMessage());
            return;
        }
        log.error(msg, e);
    }

    private synchronized void refreshHeatpumpModel(boolean cachedData) {

        if(!externalServicesEnabled || !heatpumpRefreshEnabled){
            return;
        }

        if(!cachedData && isCallError){
            return;
        }

        HeatpumpRequest request = new HeatpumpRequest();
        request.setHeatpumpUsername(env.getProperty("heatpump.basement.driver.user"));
        request.setHeatpumpPassword(env.getProperty("heatpump.basement.driver.pass"));
        request.setReadFromCache(cachedData);

        HeatpumpResponse response = callDriver(request);
        handleResponse(request, response);
    }

    private void handleResponse(HeatpumpRequest request, HeatpumpResponse response) {

        if(responseHasError(response)){
            try {
                log.warn("Error calling heatpump driver: " + new ObjectMapper().writeValueAsString(response));
            } catch (JsonProcessingException e) {
                log.warn("Error calling heatpump driver....");
            }
            if(!request.isReadFromCache() || (request.getWriteWithRoomnameAndProgram() != null && !request.getWriteWithRoomnameAndProgram().isEmpty())){
                CompletableFuture.runAsync(() -> pushService.sendErrorMessage("Fehler bei Ansteuerung von Heizung!"));
            }
            switchModelToUnknown();
            return;
        }

        HeatpumpBasementModel newModel = getHeatpumpModelWithUnknownPresets();
        ModelObjectDAO.getInstance().write(newModel);
        uploadService.uploadToClient(newModel);
    }

    private boolean responseHasError(HeatpumpResponse response) {
        return !response.isRemoteConnectionSuccessful() || !response.isDriverRunSuccessful() || StringUtils.isNotBlank(response.getErrorMessage());
    }

    private void switchModelToUnknown() {

        final HeatpumpBasementModel unknownHeatpumpModel = getHeatpumpModelWithUnknownPresets();
        ModelObjectDAO.getInstance().write(unknownHeatpumpModel);
        uploadService.uploadToClient(unknownHeatpumpModel);
    }

    private synchronized HeatpumpResponse callDriver(HeatpumpRequest request){

        try {
            ResponseEntity<HeatpumpResponse> response = externalServiceHttpAPI.postForHeatpumpEntity(
                    Objects.requireNonNull(env.getProperty("heatpump.basement.driver.url")), request);
            HttpStatusCode statusCode = response.getStatusCode();

            if (!statusCode.is2xxSuccessful()) {
                log.error("Could not call heatpump driver. RC=" + statusCode.value());
                isCallError = true;
            }
            final HeatpumpResponse body = response.getBody();
            if(body != null){
                body.setRemoteConnectionSuccessful(true);
            }
            return body;

        } catch (RestClientException e) {
            log.error("Exception calling heatpump driver:" + e.getMessage());
            isCallError = true;
            var response = new HeatpumpResponse();
            response.setRemoteConnectionSuccessful(false);
            response.setErrorMessage("Exception calling heatpump driver:" + e.getMessage());
            return response;
        }
    }

    private HeatpumpBasementModel getHeatpumpModelWithUnknownPresets() {

        final var unknownHeatpumpModel = new HeatpumpBasementModel();
        unknownHeatpumpModel.setTimestamp(System.currentTimeMillis());
        return unknownHeatpumpModel;
    }
}
