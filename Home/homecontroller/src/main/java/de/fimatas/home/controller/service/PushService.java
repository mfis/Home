package de.fimatas.home.controller.service;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.domain.model.HouseModel;
import de.fimatas.home.library.domain.model.Setting;
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

    public synchronized void send(HouseModel oldModel, HouseModel newModel) {

        try {
            doorbellMessage(oldModel, newModel);
        } catch (Exception e) {
            LogFactory.getLog(PushService.class).error("Could not send push notifications:", e);
        }
    }

    private void doorbellMessage(HouseModel oldModel, HouseModel newModel) {

        if (!HouseService.doorbellTimestampChanged(oldModel, newModel)) {
            return;
        }

        settingsService.listTokensWithEnabledSetting(Setting.DOORBELL).forEach(pushToken -> {
            final String time =
                TIME_FORMATTER.format(Instant.ofEpochMilli(newModel.getFrontDoorBell().getTimestampLastDoorbell())
                .atZone(ZoneId.systemDefault()).toLocalDateTime());
            handleMessage(pushToken, "Türklingelbetätigung", "Zeitpunkt: " + time + " Uhr.");
        });
    }

    private void handleMessage(String pushToken, String title, String message) {

        if (HomeAppConstants.PUSH_TOKEN_NOT_AVAILABLE_INDICATOR.equals(pushToken)) {
            LOG.info("Push Message to dummy token: " + title + " - " + message);
        } else {
            sendToApns(pushToken, title, message);
        }
    }

    private void sendToApns(String pushToken, String title, String message) {

        final ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
        payloadBuilder.setAlertTitle(title);
        payloadBuilder.setAlertBody(message);

        PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture =
            apnsClient.sendNotification(new SimpleApnsPushNotification(pushToken, iOsAppIdentifier, payloadBuilder.build()));

        sendNotificationFuture.whenComplete((response, cause) -> handleApnsResponse(response, cause, pushToken));
    }

    public void handleApnsResponse(PushNotificationResponse<SimpleApnsPushNotification> response, Throwable cause,
            String pushToken) {

        if (response != null) {
            if (!response.isAccepted()) {
                LOG.warn("Push Notification rejected by the apns gateway: " + response.getRejectionReason());
                settingsService.deleteSettingsForToken(pushToken);
            }
        } else {
            LOG.error("Failed to send push notification to apns.", cause);
        }
    }

}
