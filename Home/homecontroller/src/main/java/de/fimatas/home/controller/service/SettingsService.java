package de.fimatas.home.controller.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
            List.of(PushNotifications.values()).forEach(notification -> {
                if (!model.getPushNotifications().containsKey(notification)) {
                    model.getPushNotifications().put(notification, notification.getDefaultSetting());
                    SettingsDAO.getInstance().write(model);
                    SettingsDAO.getInstance().persist();
                }
            });
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

    public void deleteSettingsForToken(String token) {
        SettingsDAO.getInstance().read().stream().filter(model -> model.getToken().equals(token)).findFirst()
            .ifPresent(SettingsDAO.getInstance()::delete);
        refreshSettingsModelsComplete();
    }

    public List<String> listTokensWithEnabledSetting(PushNotifications pushNotifications) {
        return SettingsDAO.getInstance().read().stream()
            .filter(model -> model.getPushNotifications().get(pushNotifications)).map(SettingsModel::getToken)
            .collect(Collectors.toList());
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
