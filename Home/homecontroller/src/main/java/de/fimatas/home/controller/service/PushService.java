package de.fimatas.home.controller.service;

import com.eatthepath.pushy.apns.*;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.auth.AuthenticationToken;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.LiveActivityEvent;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import de.fimatas.home.controller.dao.PushMessageDAO;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.controller.model.PushToken;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.util.HomeAppConstants;
import de.fimatas.home.library.util.WeatherForecastConclusionTextFormatter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class PushService {

    @Autowired
    private SettingsService settingsService;

    private ApnsClient apnsClient;

    private ApnsClient apnsClientJwtBased;

    @Value("${apns.ios.app.identifier}")
    private String iOsAppIdentifier;

    @Value("${apns.cert.path}")
    private String apnsCertPath;

    @Value("${apns.cert.pass}")
    private String apnsCertPass;

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

    private static LocalDateTime timestampLastDoorbellPushMessage = LocalDateTime.now();

    private static final Log LOG = LogFactory.getLog(PushService.class);

    @PostConstruct
    public void init() {
        try {
            apnsClient = new ApnsClientBuilder()
                .setApnsServer(
                    apnsUseProductionServer ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                .setClientCredentials(new File(apnsCertPath), apnsCertPass)
                .build();
        } catch (IOException e) {
            LOG.error("Unable to build apnsClient.", e);
        }
        try {
            apnsClientJwtBased = new ApnsClientBuilder()
                    .setApnsServer(
                            apnsUseProductionServer ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                    .setSigningKey(ApnsSigningKey.loadFromPkcs8File(new File(pkcs8File),
                            apnsTeamId, apnsKeyId))
                    .build();
            // FIXME: REFRESH 60 MINUTES
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            LOG.error("Unable to build apnsClientJwtBased.", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (apnsClient != null) {
            CompletableFuture<Void> closeFuture = apnsClient.close();
            try {
                closeFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Unable to shutdown apnsClient.", e);
                Thread.currentThread().interrupt();
            }
        }
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

    @Scheduled(cron = "0 00 00 * * *")
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

    @Scheduled(cron = "0 2 5 * * *")
    public void sendWeatherAtMorning() {
        try {
            todayWeatherMessage(ModelObjectDAO.getInstance().readWeatherForecastModel());
        } catch (Exception e) {
            LogFactory.getLog(PushService.class).error("Could not [sendWeatherAtMorning] push notifications:", e);
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

    public void chargeFinished(boolean early, String user) {

        PushNotifications notification = early ? PushNotifications.CHARGELIMIT_ERROR : PushNotifications.CHARGELIMIT_OK;
        final PushToken pushToken = settingsService.tokenWithEnabledSettingForUser(notification, user);
        if(pushToken != null){
            handleMessage(pushToken, "Wallbox", notification.getPushText());
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

    private void lowBatteryMessage(HouseModel newModel) {

        if (!newModel.getLowBatteryDevices().isEmpty()) {
            settingsService.listTokensWithEnabledSetting(PushNotifications.LOW_BATTERY)
                .forEach(pushToken -> handleMessage(pushToken, PushNotifications.LOW_BATTERY.getPushText(),
                    StringUtils.join(newModel.getLowBatteryDevices(), ", ")));
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

    private void todayWeatherMessage(WeatherForecastModel model) {

        if(model==null || model.getConclusionToday()==null){
            return;
        }

        settingsService.listTokensWithEnabledSetting(PushNotifications.WEATHER_TODAY).forEach(pushToken -> {
            var text = WeatherForecastConclusionTextFormatter.formatConclusionText(model.getConclusionToday()).get(WeatherForecastConclusionTextFormatter.FORMAT_LONGEST);
            if(StringUtils.isNotBlank(text)){
                handleMessage(pushToken, PushNotifications.WEATHER_TODAY.getPushText(), text);
            }
        });
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
        payloadBuilder.setBadgeNumber(1);

        PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture =
            apnsClient.sendNotification(new SimpleApnsPushNotification(pushToken.getToken(), iOsAppIdentifier, payloadBuilder.build()));

        sendNotificationFuture.whenComplete((response, cause) -> {
            saveNewMessageToDatabase(ts, pushToken, title, message);
            handleApnsResponse(response, cause, pushToken, false);
        });
    }

    public void sendLiveActivityToApns(String pushToken /*, LocalDateTime ts, String title, String message*/) {

        Map<String, Object> contentState = new LinkedHashMap<>();
        contentState.put("valueLeading", DateTimeFormatter.ofPattern("HHmmss", Locale.GERMAN).format(LocalDateTime.now()));
        contentState.put("valueTrailing", "");
        contentState.put("colorLeading", "green");
        contentState.put("colorTrailing", "");

        final ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
        payloadBuilder.setEvent(LiveActivityEvent.UPDATE);
        payloadBuilder.setTimestamp(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        payloadBuilder.setContentState(contentState);

        var prority = DeliveryPriority.IMMEDIATE; // FIXME;
        var invalidationTime = Instant.now().plus(Duration.ofHours(4));
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

        if(doResetSettings && !isLiveActivity){ // FIXME
            settingsService.resetSettingsForToken(pushToken.getToken());
            saveNewMessageToDatabase(uniqueTimestampService.get(), pushToken, "Push-Zustellung Fehler", "Bitte erneut registrieren.");
        }
    }

    private synchronized void saveNewMessageToDatabase(LocalDateTime ts, PushToken token, String title, String text){

        if(pushMessageDAO.readMessagesFromLastThreeSeconds().stream().anyMatch(pm ->
                pm.getUsername().equalsIgnoreCase(token.getUsername()) && pm.getTitle().equals(title) && pm.getTextMessage().equals(text))){
            LOG.warn("Duplicate message for user " + token.getUsername() + ". Check token settings!");
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
