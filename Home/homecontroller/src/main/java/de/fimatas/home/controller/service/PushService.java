package de.fimatas.home.controller.service;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import de.fimatas.home.controller.dao.PushMessageDAO;
import de.fimatas.home.controller.model.PushToken;
import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.util.WeatherForecastConclusionTextFormatter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.util.HomeAppConstants;

@Component
public class PushService {

    @Autowired
    private SettingsService settingsService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private ApnsClient apnsClient;

    @Value("${apns.cert.path}")
    private String apnsCertPath;

    @Value("${apns.cert.pass}")
    private String apnsCertPass;

    @Value("${apns.ios.app.identifier}")
    private String iOsAppIdentifier;

    @Value("${apns.use.production.server}")
    private boolean apnsUseProductionServer;

    @Autowired
    private PushMessageDAO pushMessageDAO;

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
    }

    @Scheduled(cron = "0 00 00 * * *")
    public void cleanUpDatabase() {
        pushMessageDAO.deleteMessagesOlderAsNDays(92);
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

        if (!HouseService.doorbellTimestampChanged(oldModel, newModel)) {
            return;
        }

        settingsService.listTokensWithEnabledSetting(PushNotifications.DOORBELL).forEach(pushToken -> {
            final String time =
                TIME_FORMATTER.format(Instant.ofEpochMilli(newModel.getFrontDoorBell().getTimestampLastDoorbell())
                .atZone(ZoneId.systemDefault()).toLocalDateTime());
            handleMessage(pushToken, PushNotifications.DOORBELL.getPushText(), "Zeitpunkt: " + time + " Uhr.");
        });
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

        if (HomeAppConstants.PUSH_TOKEN_NOT_AVAILABLE_INDICATOR.equals(pushToken.getToken())) {
            LOG.info("Push Message to dummy token: " + title + " - " + message);
            saveNewMessageToDatabase(pushToken, title, message);
        } else {
            sendToApns(pushToken, title, message);
        }
    }

    private void sendToApns(PushToken pushToken, String title, String message) {

        final ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
        payloadBuilder.setAlertTitle(title);
        payloadBuilder.setAlertBody(message);
        payloadBuilder.setSound("default");
        payloadBuilder.setBadgeNumber(1);

        PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture =
            apnsClient.sendNotification(new SimpleApnsPushNotification(pushToken.getToken(), iOsAppIdentifier, payloadBuilder.build()));

        sendNotificationFuture.whenComplete((response, cause) -> handleApnsResponse(response, cause, pushToken, title, message));
    }

    private void handleApnsResponse(PushNotificationResponse<SimpleApnsPushNotification> response, Throwable cause, PushToken pushToken, String title, String text) {

        if (response != null) {
            if (!response.isAccepted()) {
                LOG.warn("Push Notification rejected by the apns gateway: " + response.getRejectionReason());
                settingsService.deleteSettingsForToken(pushToken.getToken());
            }
        } else {
            LOG.error("Failed to send push notification to apns.", cause);
        }
        saveNewMessageToDatabase(pushToken, title, text);
    }

    private void saveNewMessageToDatabase(PushToken token, String title, String text){
        CompletableFuture.runAsync(() -> {
            pushMessageDAO.writeMessage(token.getUsername(), title, text);
            // FIXME: UPLOAD
        });
    }

}
