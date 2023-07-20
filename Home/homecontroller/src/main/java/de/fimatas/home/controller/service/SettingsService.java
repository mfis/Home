package de.fimatas.home.controller.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import de.fimatas.home.controller.model.PushToken;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import de.fimatas.home.controller.dao.SettingsDAO;
import de.fimatas.home.controller.domain.service.HistoryService;
import de.fimatas.home.library.domain.model.PushNotifications;
import de.fimatas.home.library.model.SettingsContainer;
import de.fimatas.home.library.model.SettingsModel;

@Component
public class SettingsService {

    @Autowired
    private UploadService uploadService;

    @Autowired
    private Environment env;

    @PostConstruct
    public void init() {

        try {
            refreshSettingsModelsComplete();
        } catch (Exception e) {
            LogFactory.getLog(HistoryService.class).error("Could not initialize SettingsService completly.", e);
        }
    }

    public void refreshSettingsModelsComplete() {

        SettingsContainer container = new SettingsContainer();
        // SettingsContainer
        SettingsDAO.getInstance().read().forEach(model -> {
            // migrate up to new version if available
            AtomicBoolean changed = new AtomicBoolean(false);
            List.of(PushNotifications.values()).forEach(notification -> {
                if (!model.getPushNotifications().containsKey(notification)) {
                    model.getPushNotifications().put(notification, notification.getDefaultSetting());
                    changed.set(true);
                }
            });
            if(!model.getAttributes().containsKey("LON") || !model.getAttributes().get("LON").equals(env.getProperty("weatherForecast.lon"))){
                model.getAttributes().put("LON", Objects.requireNonNull(env.getProperty("weatherForecast.lon")));
                changed.set(true);
            }
            if(!model.getAttributes().containsKey("LAT") || !model.getAttributes().get("LAT").equals(env.getProperty("weatherForecast.lat"))){
                model.getAttributes().put("LAT", Objects.requireNonNull(env.getProperty("weatherForecast.lat")));
                changed.set(true);
            }
            if(!model.getAttributes().containsKey("RADIUS") || !model.getAttributes().get("RADIUS").equals(env.getProperty("presence.radius.meter"))){
                model.getAttributes().put("RADIUS", Objects.requireNonNull(env.getProperty("presence.radius.meter")));
                changed.set(true);
            }
            if(changed.get()){
                SettingsDAO.getInstance().write(model);
                SettingsDAO.getInstance().persist();
            }
            container.getSettings().add(model);
        });
        uploadService.uploadToClient(container);
    }

    public boolean createNewSettingsForToken(String token, String user, String client) {

        if (SettingsDAO.getInstance().read().stream().anyMatch(model -> model.getToken().equals(token))) {
            return false;
        }

        Optional<SettingsModel> oldToken = SettingsDAO.getInstance().read().stream()
            .filter(model -> model.getUser().equals(user) && model.getClient().equals(client)).findFirst();

        final var model = new SettingsModel();
        model.setToken(token);
        model.setLastTimestamp(System.currentTimeMillis());
        model.setUser(user);
        model.setClient(client);
        if (oldToken.isPresent()) {
            // after apns token switch adopt settings from old token
            List.of(PushNotifications.values()).forEach(notification -> model.getPushNotifications().put(notification,
                oldToken.get().getPushNotifications().get(notification)));
        } else {
            List.of(PushNotifications.values())
                .forEach(notification -> model.getPushNotifications().put(notification, notification.getDefaultSetting()));
        }
        SettingsDAO.getInstance().write(model);
        SettingsDAO.getInstance().persist();

        refreshSettingsModelsComplete();

        return oldToken.isEmpty();
    }

    public void resetSettingsForToken(String token) {
        EnumMap<PushNotifications, Boolean> newMap = new EnumMap<>(PushNotifications.class);
        SettingsDAO.getInstance().read().stream().filter(model -> model.getToken().equals(token)).findFirst()
            .ifPresent(s -> {
                s.getPushNotifications().keySet().forEach(k -> newMap.put(k, false));
                s.setPushNotifications(newMap);
                SettingsDAO.getInstance().write(s);
            });
        SettingsDAO.getInstance().persist();
        refreshSettingsModelsComplete();
    }

    public List<PushToken> listTokensWithEnabledSetting(PushNotifications pushNotifications) {
        return SettingsDAO.getInstance().read().stream()
            .filter(model -> model.getPushNotifications().get(pushNotifications))
                .map(settingsModel -> new PushToken(settingsModel.getUser(), settingsModel.getToken()))
            .collect(Collectors.toList());
    }

    public PushToken tokenWithEnabledSettingForUser(PushNotifications pushNotifications, String user) {
        return SettingsDAO.getInstance().read().stream()
                .filter(model -> model.getPushNotifications().get(pushNotifications))
                .filter(settings -> settings.getUser().equals(user))
                .map(settingsModel -> new PushToken(settingsModel.getUser(), settingsModel.getToken()))
                .findFirst().orElse(null);
    }

    public PushToken tokenForUser(String user) {
        return SettingsDAO.getInstance().read().stream()
                .filter(settings -> settings.getUser().equals(user))
                .map(settingsModel -> new PushToken(settingsModel.getUser(), settingsModel.getToken()))
                .findFirst().orElse(null);
    }

    public void editSetting(String token, String key, boolean value){
        SettingsDAO.getInstance().read().stream().filter(sm -> sm.getToken().equals(token)).findFirst().ifPresent(
                        sm -> {
                            sm.getPushNotifications().put(PushNotifications.valueOf(key), value);
                            SettingsDAO.getInstance().write(sm);
                            // Edit other settings for same user (=e.g. old tokens) too
                            SettingsDAO.getInstance().read().stream().filter(othersm -> othersm.getUser().equals(sm.getUser())).forEach(othersm -> {
                                othersm.getPushNotifications().put(PushNotifications.valueOf(key), value);
                                SettingsDAO.getInstance().write(othersm);
                            });
                        });
        SettingsDAO.getInstance().persist();
        refreshSettingsModelsComplete();
    }

}
