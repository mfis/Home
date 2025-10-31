package de.fimatas.home.controller.service;

import de.fimatas.heatpump.basement.driver.api.*;
import de.fimatas.home.controller.api.ExternalServiceHttpAPI;
import de.fimatas.home.controller.api.HomematicAPI;
import de.fimatas.home.controller.command.HomematicCommand;
import de.fimatas.home.controller.command.HomematicCommandBuilder;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.HeatpumpBasementDatapoint;
import de.fimatas.home.library.domain.model.HeatpumpBasementModel;
import de.fimatas.home.library.domain.model.ValueWithTendency;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.model.ConditionColor;
import de.fimatas.home.library.util.HomeUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private HomematicAPI hmApi;

    @Autowired
    private HomematicCommandBuilder homematicCommandBuilder;

    @Autowired
    private Environment env;

    @Value("${application.externalServicesEnabled:false}")
    private boolean externalServicesEnabled;

    @Value("${application.heatpumpRefreshEnabled:false}")
    private boolean heatpumpRefreshEnabled;

    private boolean isCallError = false; // prevent continous error calls

    private final Map<Device, Integer> lastValuesWrote = new HashMap<>();

    private static final long REFRESH_DELAY_MS = 1000L * 60L * 30L;

    @PostConstruct
    public void init() {
        scheduledRefreshFromDriverCache();
    }

    @Scheduled(cron = "0 0 2,14 * * *")
    public void resetCallErrorFlag() {
        isCallError = false;
    }

    public void scheduledRefreshFromDriverCache() {
        try {
            refreshHeatpumpModel(true, false);
        } catch (Exception e) {
            handleException(e, "Could not call heatpump basement service (with-cache)");
        }
    }

    @Scheduled(initialDelay = REFRESH_DELAY_MS, fixedDelay = REFRESH_DELAY_MS)
    public void scheduledRefreshFromDriverNoCache() {
        int stunde = LocalTime.now().getHour();
        if (stunde >= 5 && stunde <= 22) {
            callWithoutCache(false);
        }
    }

    @Async
    public void readFromClientRequest() {
        var model = ModelObjectDAO.getInstance().readHeatpumpBasementModel();
        if(model != null && model.getApiReadTimestamp() > 0){
            return;
        }
        callWithoutCache(true);
    }

    private void callWithoutCache(boolean manual) {
        try {
            refreshHeatpumpModel(false, manual);
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

    private synchronized void refreshHeatpumpModel(boolean cachedData, boolean manual) {

        if(!externalServicesEnabled || !heatpumpRefreshEnabled){
            return;
        }

        if(!cachedData && isCallError){
            return;
        }

        if(!cachedData && manual){
            switchModelToBusy();
        }

        var sshUser = env.getProperty("heatpump.basement.driver.sshUser");
        var sshPass = env.getProperty("heatpump.basement.driver.sshPass");
        var apiUser = env.getProperty("heatpump.basement.driver.apiUser");
        var apiPass =  env.getProperty("heatpump.basement.driver.apiPass");

        Request request = new Request();
        request.setCredentials(new Credentials(sshUser, sshPass, apiUser, apiPass));
        request.setReadFromCache(cachedData);

        Response response = callDriver(request);
        handleResponse(request, response);
    }

    private void switchModelToBusy() {

        var model = ModelObjectDAO.getInstance().readHeatpumpBasementModel();
        if(model == null){
            model = emptyModel();
        }
        model.setBusy(true);
        model.setTimestamp(model.getTimestamp() + 1);
        ModelObjectDAO.getInstance().write(model);
        uploadService.uploadToClient(model);
    }

    private void handleResponse(Request request, Response response) {

        if(responseHasError(response)){
            try {
                var html = StringUtils.substringBetween(response.getErrorMessage(), "<!DOCTYPE html>", "</html>");
                log.warn("Error calling heatpump basement driver: " + StringUtils.remove(response.getErrorMessage(), html));
            } catch (Exception e) {
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

        if(!request.isReadFromCache()){
            updateHomematicSysVars(newModel);
        }

        ModelObjectDAO.getInstance().write(newModel);
        uploadService.uploadToClient(newModel);
    }

    private void updateHomematicSysVars(HeatpumpBasementModel newModel) {

        if (newModel == null || newModel.isOffline()) {
            return;
        }

        List<HomematicCommand> commands = new ArrayList<>();
        createHomematicSysVar(newModel, HeatpumpBasementDatapoints.VERBRAUCH_AKTUELLES_JAHR, Device.ELECTRIC_POWER_CONSUMPTION_COUNTER_HEATPUMP_BASEMENT, commands);
        createHomematicSysVar(newModel, HeatpumpBasementDatapoints.ERZEUGTE_WAERME_AKTUELLES_JAHR, Device.WARMTH_POWER_PRODUCTION_COUNTER_HEATPUMP_BASEMENT, commands);

        if(!commands.isEmpty()){
            hmApi.executeCommand(commands.toArray(new HomematicCommand[]{}));
        }
    }

    private void createHomematicSysVar(HeatpumpBasementModel newModel, HeatpumpBasementDatapoints datapoint, Device device, List<HomematicCommand> commands) {

        var datapointValue = newModel.getDatapoints().stream().filter(dp -> dp.getId().equals(datapoint.getId())).findFirst();

        if (datapointValue.isPresent() && datapointValue.get().getValueWithTendency() != null) {
            var valueToWrite = datapointValue.get().getValueWithTendency().getValue();
            var lastValueWrote = lastValuesWrote.get(device);
            if (lastValueWrote != null && lastValueWrote == valueToWrite.intValue()) {
                log.debug("SAME VALUE " + datapoint.getId() + ": " + valueToWrite);
            }else{
                log.debug("WRITE VALUE " + datapoint.getId() + ": " + valueToWrite);
                var command = homematicCommandBuilder.write(device,
                        de.fimatas.home.library.homematic.model.Datapoint.SYSVAR_DUMMY,
                        valueToWrite.toString());
                commands.add(command);
                lastValuesWrote.put(device, valueToWrite.intValue());
            }
        }
    }

    private void mapResponseToModel(Response response, HeatpumpBasementModel newModel) {

        newModel.setApiReadTimestamp(response.getTimestampResponse());
        newModel.setOffline(response.getExitCode() != null && response.getExitCode() == Response.RC_OFFLINE);

        Map<String, Datapoint> idMap = response.getDatapointList().stream()
                .collect(Collectors.toMap(Datapoint::id, Function.identity()));

        var oldModel = ModelObjectDAO.getInstance().readHeatpumpBasementModel();

        Arrays.stream(HeatpumpBasementDatapoints.values()).toList().forEach(enumDp -> {
            var apiDp = idMap.get(enumDp.getId());
            var modelDp = new HeatpumpBasementDatapoint();
            modelDp.setId(enumDp.getId());
            modelDp.setName(enumDp.getAlternateLabel());
            modelDp.setGroup(enumDp.getGroup());
            modelDp.setDescription("");
            if(apiDp==null){
                modelDp.setValueFormattedLong("Unbekannt");
                modelDp.setValueFormattedLong("?");
                modelDp.setConditionColor(ConditionColor.RED);
            }else{
                modelDp.setValueFormattedLong(enumDp.getFormattedValueLong().apply(apiDp.value()));
                modelDp.setValueFormattedShort(enumDp.getFormattedValueShort().apply(apiDp.value()));
                if(enumDp.getTendencyThreshold() != null){
                    Optional<HeatpumpBasementDatapoint> oldValue = oldModel == null ? Optional.empty() : oldModel.getDatapoints().stream().filter(dp -> dp.getId().equals(enumDp.getId())).findFirst();
                    var valueBD = HeatpumpBasementDatapoints.valueAsBigDecimal(enumDp.getFormattedValueLong().apply(apiDp.value()));
                    var valueWithTenency = new ValueWithTendency<>(valueBD);
                    HomeUtils.calculateTendency(response.getTimestampResponse(), oldValue.map(HeatpumpBasementDatapoint::getValueWithTendency).orElse(null), valueWithTenency, enumDp.getTendencyThreshold());
                    modelDp.setValueWithTendency(valueWithTenency);
                }
                modelDp.setConditionColor(ConditionColor.valueOf(enumDp.getStateColorBasedByValue().apply(apiDp.value()).name()));
                modelDp.setStateOff(HeatpumpBasementDatapoints.isValueOff(apiDp.value()));
            }
            newModel.getDatapoints().add(modelDp);
        });

        if(newModel.isOffline()){
            newModel.setConditionColor(ConditionColor.RED);
        }else{
            newModel.setConditionColor(
                    newModel.getDatapoints().stream().map(HeatpumpBasementDatapoint::getConditionColor)
                            .min(Comparator.comparingInt(Enum::ordinal)).orElseThrow());
        }
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

        log.info("call HeatpumpBasement " + (request.isReadFromCache() ? "cache" : "LIVE !"));
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
