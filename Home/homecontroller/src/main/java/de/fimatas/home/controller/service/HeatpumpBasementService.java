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
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.fimatas.home.library.util.HomeAppConstants.MODEL_HEATPUMP_BASEMENT_UPDATE_INTERVAL_SECONDS;

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

    @Value("${heatpump.basement.consumption.id:}")
    private String idConsumption;

    @Value("${heatpump.basement.production.id:}")
    private String idProduction;

    private final CircuitBreaker circuitBreaker;

    private final Map<Device, Integer> lastValuesWrote = new HashMap<>();

    private static final long REFRESH_DELAY_MS = 1000L * MODEL_HEATPUMP_BASEMENT_UPDATE_INTERVAL_SECONDS;

    public HeatpumpBasementService(CircuitBreakerRegistry registry) {
        this.circuitBreaker = registry.circuitBreaker("heatpumpBasement");
    }

    public void scheduledRefreshFromDriverCache() {
        try {
            refreshHeatpumpModel(true, false);
        } catch (Exception e) {
            handleException(e, "Could not call heatpump basement service (with-cache)", true);
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

        var stateBeforeCall = circuitBreaker.getState();
        try {
            refreshHeatpumpModel(false, manual);
        } catch (Exception e) {
            handleException(e, "Could not call heatpump basement service (no-cache!)", false);
        } finally {
            var stateAfterCall = circuitBreaker.getState();
            if(stateBeforeCall != stateAfterCall && stateAfterCall == CircuitBreaker.State.OPEN){
                log.warn("circuit breaker heatpumpBasement now OPEN");
                switchModelToUnknown();
                CompletableFuture.runAsync(() -> pushService.sendErrorMessage("Fehler beim Auslesen der Heizung!"));
            }
            if(manual && ModelObjectDAO.getInstance().readHeatpumpBasementModel() != null && ModelObjectDAO.getInstance().readHeatpumpBasementModel().isBusy()){
                switchModelToUnknown();
            }
        }
    }

    private void handleException(Exception e, String msg, boolean cached) {
        circuitBreakerOnError(cached);
        if(e instanceof RestClientException && e.getMessage().startsWith(ExternalServiceHttpAPI.MESSAGE_TOO_MANY_CALLS)){
            log.warn(msg + " - " + e.getMessage());
            return;
        }
        log.error(msg, e);
    }

    private synchronized void refreshHeatpumpModel(boolean cachedData, boolean manual) {

        if(!externalServicesEnabled || !heatpumpRefreshEnabled){
            return;
        }

        if(!cachedData){
            if (!circuitBreaker.tryAcquirePermission()) {
                return;
            }
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
        model.setApiReadTimestamp(model.getTimestamp());
        ModelObjectDAO.getInstance().write(model);
        uploadService.uploadToClient(model);
    }

    private void handleResponse(Request request, Response response) {

        if(responseHasError(response)){
            try {
                if(response.getErrorMessage().contains("<!DOCTYPE")){
                    log.warn("Error calling heatpump basement driver - version problem detected !!!");
                }else{
                    log.warn("Error calling heatpump basement driver: " + response.getErrorMessage());
                }
            } catch (Exception e) {
                log.warn("Error calling heatpump basement driver....");
            }
            circuitBreakerOnError(request.isReadFromCache());
            return;
        }

        HeatpumpBasementModel newModel = emptyModel();
        newModel.setIdConsumption(StringUtils.trimToNull(idConsumption));
        newModel.setIdProduction(StringUtils.trimToNull(idProduction));
        mapResponseToModel(response, newModel);

        if(!request.isReadFromCache()){
            circuitBreakerOnSuccess();
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
            modelDp.setHide(enumDp.isHidden());
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
        return response.getExitCode() == null || response.getExitCode() == 1 || response.getExitCode() > 2 || StringUtils.isNotBlank(response.getErrorMessage());
    }

    private void switchModelToUnknown() {
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
                circuitBreakerOnError(request.isReadFromCache());
            }
            return response.getBody();

        } catch (RestClientException e) {
            var response = new Response();
            response.setErrorMessage("Exception calling heatpump basement driver:" + e.getMessage());
            response.setExitCode(9);
            // handleResponse reacts to ErrorMessage - no call 'circuitBreakerOnError' needed here
            return response;
        }
    }

    private HeatpumpBasementModel emptyModel() {

        final var unknownHeatpumpModel = new HeatpumpBasementModel();
        unknownHeatpumpModel.setTimestamp(System.currentTimeMillis());
        return unknownHeatpumpModel;
    }

    private void circuitBreakerOnSuccess(){
        circuitBreaker.onSuccess(0, TimeUnit.MILLISECONDS);
    }

    private void circuitBreakerOnError(boolean cacheCall){
        if(!cacheCall){
            circuitBreaker.onError(0, TimeUnit.MILLISECONDS, new RuntimeException("heatpump basement driver call failed"));
        }
    }
}
