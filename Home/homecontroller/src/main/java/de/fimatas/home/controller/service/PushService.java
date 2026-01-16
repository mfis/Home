package de.fimatas.home.controller.service;

import com.eatthepath.pushy.apns.*;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.LiveActivityEvent;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import de.fimatas.heatpump.basement.driver.api.HeatpumpBasementDatapoints;
import de.fimatas.home.controller.api.HomematicAPI;
import de.fimatas.home.controller.dao.LiveActivityDAO;
import de.fimatas.home.controller.dao.PushMessageDAO;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.controller.model.PushToken;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.model.PhotovoltaicsStringsStatus;
import de.fimatas.home.library.model.PvAdditionalDataModel;
import de.fimatas.home.library.model.TaskState;
import de.fimatas.home.library.model.TasksModel;
import de.fimatas.home.library.util.HomeAppConstants;
import de.fimatas.home.library.util.HomeUtils;
import de.fimatas.home.library.util.WeatherForecastConclusionTextFormatter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static de.fimatas.home.library.util.HomeUtils.buildDecimalFormat;

@Component
public class PushService {

    @Autowired
    private SettingsService settingsService;

    private ApnsClient apnsClientJwtBased;

    @Value("${apns.ios.app.identifier}")
    private String iOsAppIdentifier;

    @Value("${apns.ios.app.teamId}")
    private String apnsTeamId;

    @Value("${apns.ios.app.keyId}")
    private String apnsKeyId;

    @Value("${apns.ios.app.pkcs8File}")
    private String pkcs8File;

    @Value("${apns.use.production.server}")
    private boolean apnsUseProductionServer;

    @Autowired
    private PushMessageDAO pushMessageDAO;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private UniqueTimestampService uniqueTimestampService;

    @Autowired
    private HomematicAPI homematicAPI;

    private static LocalDateTime timestampLastDoorbellPushMessage = LocalDateTime.now();

    private static final Log LOG = LogFactory.getLog(PushService.class);

    @PostConstruct
    public void init() {
        try {
            apnsClientJwtBased = new ApnsClientBuilder()
                    .setApnsServer(
                            apnsUseProductionServer ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                    .setSigningKey(ApnsSigningKey.loadFromPkcs8File(new File(pkcs8File),
                            apnsTeamId, apnsKeyId))
                    .build();
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            LOG.error("Unable to build apnsClientJwtBased.", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (apnsClientJwtBased != null) {
            CompletableFuture<Void> closeFuture = apnsClientJwtBased.close();
            try {
                closeFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Unable to shutdown apnsClientJwtBased.", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    @Scheduled(cron = "30 55 1 * * *")
    public void cleanUpDatabase() {
        pushMessageDAO.deleteMessagesOlderAsNDays(92);
        refreshModel();
    }

    public void refreshModel() {
        PushMessageModel pmm = new PushMessageModel();
        pmm.setAdditionalEntries(false);
        pmm.getList().addAll(pushMessageDAO.readMessages());
        ModelObjectDAO.getInstance().write(pmm);
        uploadService.uploadToClient(pmm);
    }

    @Scheduled(cron = "0 00 22 * * *")
    public void sendOpenWindowAtLateEvening() {

        try {
            windowOpenMessage(ModelObjectDAO.getInstance().readHouseModel());
        } catch (Exception e) {
            LogFactory.getLog(PushService.class).error("Could not [sendOpenWindowAtLateEvening] push notifications:", e);
        }
    }

    @Scheduled(cron = "0 00 4 * * *")
    public void sendLowBatteryAtEarlyMorning() {

        try {
            lowBatteryMessage(ModelObjectDAO.getInstance().readHouseModel());
        } catch (Exception e) {
            LogFactory.getLog(PushService.class).error("Could not [sendLowBatteryAtEarlyMorning] push notifications:", e);
        }
    }

    @Scheduled(cron = "0 30 5 * * *")
    public void sendLowRoofTemperatureAtEarlyMorning() {

        try {
            lowRoofTemperature(ModelObjectDAO.getInstance().readHouseModel());
        } catch (Exception e) {
            LogFactory.getLog(PushService.class).error("Could not [sendLowRoofTemperatureAtEarlyMorning] push notifications:", e);
        }
    }

    @Scheduled(cron = "0 30 07,13,20 * * *")
    public void sendPvAlert() {

        try {
            pvAlert(ModelObjectDAO.getInstance().readPvAdditionalDataModel());
        } catch (Exception e) {
            LogFactory.getLog(PushService.class).error("Could not [sendPvAlert] push notifications:", e);
        }
    }

    @Scheduled(cron = "0 31 08,14,20 * * *")
    public void sendHeatingAlert() {

        try {
            heatingAlert(ModelObjectDAO.getInstance().readHeatpumpBasementModel());
        } catch (Exception e) {
            LogFactory.getLog(PushService.class).error("Could not [sendHeatingAlert] push notifications:", e);
        }
    }

    @Scheduled(cron = "0 7 5 * * *")
    public void sendWeatherAtMorning() {
        try {
            todayWeatherMessage(ModelObjectDAO.getInstance().readWeatherForecastModel());
        } catch (Exception e) {
            LogFactory.getLog(PushService.class).error("Could not [sendWeatherAtMorning] push notifications:", e);
        }
    }

    @Scheduled(cron = "0 0 16 * * *")
    public void sendTaskNotifications() {
        try {
            taskNotifications(ModelObjectDAO.getInstance().readTasksModel());
        } catch (Exception e) {
            LogFactory.getLog(PushService.class).error("Could not [sendTaskNotifications] push notifications:", e);
        }
    }

    @Scheduled(cron = "11 01 8,13,18 * * *")
    public void sendHomematicApiFailure() {
        try {
            if(homematicAPI.isRequestFailure(true)){
                settingsService.listTokensWithEnabledSetting(PushNotifications.ERRORMESSAGE)
                        .forEach(pushToken -> handleMessage(pushToken, PushNotifications.ERRORMESSAGE.getPushText(), "Homematic CCU nicht erreichbar!"));
            }
        } catch (Exception e) {
            LogFactory.getLog(PushService.class).error("Could not [sendHomematicApiFailure] push notifications:", e);
        }
    }

    @Scheduled(cron = "30 25 11,16 * * *")
    public void sendPvStringFailure() {
        try {
            if(ModelObjectDAO.getInstance().readPvAdditionalDataModel() != null){
                if(ModelObjectDAO.getInstance().readPvAdditionalDataModel().getAlarm() != null){
                    settingsService.listTokensWithEnabledSetting(PushNotifications.ERRORMESSAGE)
                            .forEach(pushToken -> handleMessage(pushToken, PushNotifications.ERRORMESSAGE.getPushText(),
                                    "Photovoltaikanlage meldet Fehler: " + ModelObjectDAO.getInstance().readPvAdditionalDataModel().getAlarm()));
                }
                if(ModelObjectDAO.getInstance().readPvAdditionalDataModel().getStringsStatus() == PhotovoltaicsStringsStatus.ONE_FAULTY){
                    settingsService.listTokensWithEnabledSetting(PushNotifications.ERRORMESSAGE)
                            .forEach(pushToken -> handleMessage(pushToken, PushNotifications.ERRORMESSAGE.getPushText(),
                                    "Teilausfall der Photovoltaikanlage erkannt."));
                }
            }
        } catch (Exception e) {
            LogFactory.getLog(PushService.class).error("Could not [sendPvStringFailure] push notifications:", e);
        }
    }

    public void sendRegistrationConfirmation(String user, String token, String client) {

        handleMessage(new PushToken(user, token), "Registrierung erfolgreich",
            "Auf dem Gerät '" + client + "' können nun Benachrichtigungen empfangen werden.");
    }

    public synchronized void sendAfterModelRefresh(HouseModel oldModel, HouseModel newModel) {

        try {
            doorbellMessage(oldModel, newModel);
        } catch (Exception e) {
            LogFactory.getLog(PushService.class).error("Could not [sendAfterModelRefresh] push notifications:", e);
        }
    }

    public void sendErrorMessage(String message) {

        try {
            settingsService.listTokensWithEnabledSetting(PushNotifications.ERRORMESSAGE).forEach(pushToken ->
                    handleMessage(pushToken, PushNotifications.ERRORMESSAGE.getPushText(), message));
        } catch (Exception e) {
            LogFactory.getLog(PushService.class).error("Could not [sendErorMessage] push notifications:", e);
        }
    }

    @Async
    public void sendNotice(String message) {

        try {
            settingsService.listTokensWithEnabledSetting(PushNotifications.NOTICE).forEach(pushToken ->
                    handleMessage(pushToken, PushNotifications.NOTICE.getPushText(), message));
        } catch (Exception e) {
            LogFactory.getLog(PushService.class).error("Could not [sendNotice] push notifications:", e);
        }
    }

    public void chargeFinished(boolean early, String user) {

        PushNotifications notification = early ? PushNotifications.CHARGELIMIT_ERROR : PushNotifications.CHARGELIMIT_OK;
        final PushToken pushToken = settingsService.tokenWithEnabledSettingForUser(notification, user);
        if(pushToken != null){
            handleMessage(pushToken, "Wallbox", notification.getPushText());
        }
    }

    public void clientError(String message) {

        try {
            settingsService.listTokensWithEnabledSetting(PushNotifications.CLIENT_ERROR).forEach(pushToken ->
                    handleMessage(pushToken, PushNotifications.CLIENT_ERROR.getPushText(), message));
        } catch (Exception e) {
            LogFactory.getLog(PushService.class).error("Could not [clientError] push notifications:", e);
        }
    }

    public void testMessage(String user) {
        final PushToken pushToken = settingsService.tokenForUser(user);
        if(pushToken != null){
            handleMessage(pushToken, "Test", LocalDateTime.now().toString());
        }
    }

    private void windowOpenMessage(HouseModel newModel) {

        var roomNames = new LinkedList<String>();
        newModel.lookupFields(WindowSensor.class).forEach((fieldname, newValue) -> {
            if (newValue.isState()) {
                roomNames.add(newValue.getDevice().getPlace().getPlaceName());
            }
        });

        if(!roomNames.isEmpty()) {
            settingsService.listTokensWithEnabledSetting(PushNotifications.WINDOW_OPEN).forEach(pushToken -> 
            handleMessage(pushToken, PushNotifications.WINDOW_OPEN.getPushText(), StringUtils.join(roomNames, ", "))
            );
        }
    }

    private void pvAlert(PvAdditionalDataModel pvAdditionalDataModel) {

        if(pvAdditionalDataModel == null){
            return;
        }

        var liste = new LinkedList<String>();

        if(pvAdditionalDataModel.getStringsStatus() == PhotovoltaicsStringsStatus.ERROR_DETECTING){
            liste.add("Status der Photovoltaikanlage konnte nicht geprüft werden!");
        } else if(pvAdditionalDataModel.getStringsStatus() == PhotovoltaicsStringsStatus.ONE_FAULTY){
            liste.add("Teilausfall der Photovoltaikanlage erkannt!");
        }

        if (StringUtils.isNotBlank(pvAdditionalDataModel.getAlarm())) {
            liste.add("Photovoltaikanlage meldet Fehler: " + pvAdditionalDataModel.getAlarm() + "!");
        }

        if(pvAdditionalDataModel.getBatteryStateOfCharge() < 19){
            liste.add("PV-Speicher Ladestand niedrig: " + pvAdditionalDataModel.getBatteryStateOfCharge() + "%!");
        }

        if (!liste.isEmpty()) {
            settingsService.listTokensWithEnabledSetting(PushNotifications.ERRORMESSAGE)
                .forEach(pushToken -> handleMessage(pushToken, PushNotifications.ERRORMESSAGE.getPushText(),
                        String.join(", ", liste)));
        }
    }

    private void heatingAlert(HeatpumpBasementModel heatpumpBasementModel) {

        if(heatpumpBasementModel == null){
            return;
        }

        var liste = new LinkedList<String>();

        var preasure = heatpumpBasementModel.getDatapoints().stream()
                .filter(dp -> dp.getId().equals(HeatpumpBasementDatapoints.ANLAGEN_DRUCK.getId()))
                .findFirst();

        if(preasure.isPresent() && preasure.get().getValueWithTendency() != null) {
            if (preasure.get().getValueWithTendency().getValue().compareTo(new BigDecimal("1.40")) < 0) {
                liste.add("Heizkreislauf Druck gering: " + preasure.get().getValueWithTendency().getValue() + " Bar");
            }
        }

        if (!liste.isEmpty()) {
            settingsService.listTokensWithEnabledSetting(PushNotifications.ERRORMESSAGE)
                    .forEach(pushToken -> handleMessage(pushToken, PushNotifications.ERRORMESSAGE.getPushText(),
                            String.join(", ", liste)));
        }
    }

    private void lowBatteryMessage(HouseModel newModel) {

        if (!newModel.getLowBatteryDevices().isEmpty()) {
            settingsService.listTokensWithEnabledSetting(PushNotifications.LOW_BATTERY)
                    .forEach(pushToken -> handleMessage(pushToken, PushNotifications.LOW_BATTERY.getPushText(),
                            StringUtils.join(newModel.getLowBatteryDevices(), ", ")));
        }
    }

    private void lowRoofTemperature(HouseModel newModel) {

        if(newModel.getClimateRoof().getTemperature().getValue().compareTo(new BigDecimal("5.0")) < 0){
            var formattedTemperature = buildDecimalFormat("0.0")
                    .format(newModel.getClimateRoof().getTemperature().getValue()) + "°C";
            settingsService.listTokensWithEnabledSetting(PushNotifications.ATTENTION).forEach(pushToken ->
                    handleMessage(pushToken, PushNotifications.ATTENTION.getPushText(), "Temperatur Dachboden nah am Gefrierpunkt: " + formattedTemperature));
        }
    }

    private void doorbellMessage(HouseModel oldModel, HouseModel newModel) {

        if(!HouseService.doorbellTimestampChanged(oldModel, newModel)){
            return; // detect previous doorbell ring
        }

        if (Math.abs(Duration.between(timestampLastDoorbellPushMessage, LocalDateTime.now()).toSeconds()) < 4) {
            return; // detect multiple doorbell ring
        }

        timestampLastDoorbellPushMessage = LocalDateTime.now();

        settingsService.listTokensWithEnabledSetting(PushNotifications.DOORBELL).forEach(pushToken ->
                handleMessage(pushToken, PushNotifications.DOORBELL.getPushText(), "Türklingelbetätigung!"));
    }

    public void doorLock(String user) {

        final PushToken pushToken = settingsService.tokenWithEnabledSettingForUser(PushNotifications.DOOR_LOCK, user);
        if(pushToken != null){
            handleMessage(pushToken, PushNotifications.DOOR_LOCK.getPushText(), "Tür wurde verriegelt.");
        }
    }

    private void todayWeatherMessage(WeatherForecastModel model) {

        if(model==null || model.getConclusionToday()==null){
            return;
        }

        settingsService.listTokensWithEnabledSetting(PushNotifications.WEATHER_TODAY).forEach(pushToken -> {
            var text = WeatherForecastConclusionTextFormatter.formatConclusionText(model.getConclusionToday(), false).get(WeatherForecastConclusionTextFormatter.FORMAT_LONGEST);
            if(StringUtils.isNotBlank(text)){
                handleMessage(pushToken, PushNotifications.WEATHER_TODAY.getPushText(), text);
            }
        });
    }

    private void taskNotifications(TasksModel model) {

        if(model==null){
            return;
        }

        settingsService.listTokensWithEnabledSetting(PushNotifications.TASKS).forEach(pushToken -> model.getTasks().forEach(task -> {
            if(task.getNextExecutionTime() != null && HomeUtils.isSameDay(task.getNextExecutionTime(), LocalDateTime.now())){
                handleMessage(pushToken, "Heute fällige Aufgabe", task.getName());
            } else if (task.getState() == TaskState.FAR_OUT_OF_RANGE){
                handleMessage(pushToken, "Überfällige Aufgabe", task.getName());
            }
        }));
    }

    private void handleMessage(PushToken pushToken, String title, String message) {

        LocalDateTime ts = uniqueTimestampService.get();
        if (HomeAppConstants.PUSH_TOKEN_NOT_AVAILABLE_INDICATOR.equals(pushToken.getToken())) {
            LOG.info("Push Message to dummy token: " + title + " - " + message);
            saveNewMessageToDatabase(ts, pushToken, title, message);
        } else {
            sendNotificationToApns(pushToken, ts, title, message);
        }
    }

    private void sendNotificationToApns(PushToken pushToken, LocalDateTime ts, String title, String message) {

        final ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
        payloadBuilder.setAlertTitle(title);
        payloadBuilder.setAlertBody(message);
        payloadBuilder.setSound("default");
        payloadBuilder.setBadgeNumber(0);

        PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture =
                apnsClientJwtBased.sendNotification(new SimpleApnsPushNotification(pushToken.getToken(), iOsAppIdentifier, payloadBuilder.build()));

        sendNotificationFuture.whenComplete((response, cause) -> {
            saveNewMessageToDatabase(ts, pushToken, title, message);
            handleApnsResponse(response, cause, pushToken, false);
        });
    }

    @SuppressWarnings("ConstantValue")
    public void sendLiveActivityToApns(String pushToken, boolean highPriority, boolean isEnd, Duration staleDuration, Instant dismissalDate, Map<String, Object> contentState) {

        final ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
        payloadBuilder.setEvent(isEnd ? LiveActivityEvent.END : LiveActivityEvent.UPDATE);
        payloadBuilder.setTimestamp(Instant.now());
        payloadBuilder.setStaleDate(Instant.now().plus(staleDuration));
        payloadBuilder.setContentState(contentState);
        payloadBuilder.setDismissalDate(dismissalDate); // set 'now' at end to close activity widget

        var prority = highPriority ? DeliveryPriority.IMMEDIATE : DeliveryPriority.CONSERVE_POWER;
        final Instant invalidationTime = null; // not mapped
        var topic = iOsAppIdentifier + ".push-type.liveactivity"; // muss manuell angehängt werden

        var notification = new SimpleApnsPushNotification(pushToken, topic, payloadBuilder.build(), invalidationTime, prority, PushType.LIVE_ACTIVITY);
        var sendNotificationFuture = apnsClientJwtBased.sendNotification(notification);

        sendNotificationFuture.whenComplete((response, cause) -> handleApnsResponse(response, cause, new PushToken("", pushToken), true));
    }

    private void handleApnsResponse(PushNotificationResponse<SimpleApnsPushNotification> response, Throwable cause, PushToken pushToken, boolean isLiveActivity) {

        boolean doResetSettings = false;
        if (response != null) {
            if (!response.isAccepted()) {
                LOG.warn("Push Notification rejected by the apns gateway: " + response.getStatusCode() + "//" + response.getRejectionReason());
                doResetSettings = true;
            }
        } else {
            LOG.error("Failed to send push notification to apns.", cause);
        }

        if(doResetSettings){
            if(isLiveActivity){
                LiveActivityDAO.getInstance().getActiveLiveActivities().remove(pushToken.getToken());
            }else{
                settingsService.resetSettingsForToken(pushToken.getToken());
                saveNewMessageToDatabase(uniqueTimestampService.get(), pushToken, "Push-Zustellung Fehler", "Bitte erneut registrieren.");
            }
        }
    }

    private synchronized void saveNewMessageToDatabase(LocalDateTime ts, PushToken token, String title, String text){

        if(pushMessageDAO.readMessagesFromLastThreeSeconds().stream().anyMatch(pm ->
                pm.getUsername().equalsIgnoreCase(token.getUsername()) && pm.getTitle().equals(title) && pm.getTextMessage().equals(text))){
            LOG.warn("Duplicate message for user " + token.getUsername() + ". Check token settings! " + title + " - " + text);
            return;
        }

        final PushMessage message = pushMessageDAO.writeMessage(ts, token.getUsername(), title, text);
        PushMessageModel pmm = new PushMessageModel();
        pmm.setAdditionalEntries(true);
        pmm.getList().add(message);
        ModelObjectDAO.getInstance().write(pmm);
        uploadService.uploadToClient(pmm);
    }
}
