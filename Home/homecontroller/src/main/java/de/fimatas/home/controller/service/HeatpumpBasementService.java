package de.fimatas.home.controller.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fimatas.heatpump.basement.driver.api.Credentials;
import de.fimatas.heatpump.basement.driver.api.Request;
import de.fimatas.heatpump.basement.driver.api.Response;
import de.fimatas.home.controller.api.ExternalServiceHttpAPI;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.model.ConditionColor;
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
        scheduledRefreshFromDriverCache();
    }

    public void scheduledRefreshFromDriverCache() {
        try {
            refreshHeatpumpModel(true);
        } catch (Exception e) {
            handleException(e, "Could not call heatpump basement service (with-cache)");
        }
    }

    @Scheduled(cron = "44 01 6-23 * * *")
    public void scheduledRefreshFromDriverNoCache() {
        try {
            refreshHeatpumpModel(false);
        } catch (Exception e) {
            handleException(e, "Could not call heatpump basement service (no-cache!)");
        }
    }

    private void handleException(Exception e, String msg) {
        if(e instanceof RestClientException && e.getMessage().startsWith(ExternalServiceHttpAPI.MESSAGE_TOO_MANY_CALLS)){
            log.warn(msg + " - " + e.getMessage());
            return;
        }
        isCallError = true;
        log.error(msg, e);
    }

    private synchronized void refreshHeatpumpModel(boolean cachedData) {

        if(!externalServicesEnabled || !heatpumpRefreshEnabled){
            return;
        }

        if(!cachedData && isCallError){
            return;
        }

        var sshUser = env.getProperty("heatpump.basement.driver.sshUser");
        var sshPass = env.getProperty("heatpump.basement.driver.sshPass");
        var apiUser = env.getProperty("heatpump.basement.driver.apiUser");
        var apiPass =  env.getProperty("heatpump.basement.driver.apiPass");

        Request request = new Request();
        request.setReadFromCache(cachedData);
        request.setCredentials(new Credentials(sshUser, sshPass, apiUser, apiPass));
        request.setReadFromCache(cachedData);

        Response response = callDriver(request);
        handleResponse(request, response);
    }

    private void handleResponse(Request request, Response response) {

        if(responseHasError(response)){
            try {
                log.warn("Error calling heatpump basement driver: " + new ObjectMapper().writeValueAsString(response));
            } catch (JsonProcessingException e) {
                log.warn("Error calling heatpump basement driver....");
            }
            if(!request.isReadFromCache()){
                CompletableFuture.runAsync(() -> pushService.sendErrorMessage("Fehler bei Ansteuerung von Heizung!"));
            }
            switchModelToUnknown(response.getErrorMessage());
            return;
        }

        HeatpumpBasementModel newModel = emptyModel();
        mapResponseToModel(response, newModel);
        ModelObjectDAO.getInstance().write(newModel);
        uploadService.uploadToClient(newModel);
    }

    private void mapResponseToModel(Response response, HeatpumpBasementModel newModel) {
        response.getDatapointList().forEach(dp -> {
            var datapoint = new HeatpumpBasementDatapoint();
            datapoint.setId(dp.id());
            datapoint.setName(dp.name());
            datapoint.setValue(dp.value());
            datapoint.setConditionColor(newModel.getDatapoints().isEmpty() ? ConditionColor.GREEN : ConditionColor.BLUE);
            newModel.getDatapoints().add(datapoint);
        });
    }

    private boolean responseHasError(Response response) {
        return StringUtils.isNotBlank(response.getErrorMessage());
    }

    private void switchModelToUnknown(String errorMessage) {

        final HeatpumpBasementModel unknownHeatpumpModel = emptyModel();
        ModelObjectDAO.getInstance().write(unknownHeatpumpModel);
        uploadService.uploadToClient(unknownHeatpumpModel);
    }

    private synchronized Response callDriver(Request request){

        try {
            ResponseEntity<Response> response = externalServiceHttpAPI.postForHeatpumpBasementEntity(
                    Objects.requireNonNull(env.getProperty("heatpump.basement.driver.url")), request);
            HttpStatusCode statusCode = response.getStatusCode();

            if (!statusCode.is2xxSuccessful()) {
                log.error("Could not call heatpump basement driver. RC=" + statusCode.value());
                isCallError = true;
            }
            return response.getBody();

        } catch (RestClientException e) {
            log.error("Exception calling heatpump basement driver:" + e.getMessage());
            isCallError = true;
            var response = new Response();
            response.setErrorMessage("Exception calling heatpump basement driver:" + e.getMessage());
            return response;
        }
    }

    private HeatpumpBasementModel emptyModel() {

        final var unknownHeatpumpModel = new HeatpumpBasementModel();
        unknownHeatpumpModel.setTimestamp(System.currentTimeMillis());
        return unknownHeatpumpModel;
    }
}
