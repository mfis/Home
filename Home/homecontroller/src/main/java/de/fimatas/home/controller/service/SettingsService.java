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
import de.fimatas.home.controller.domain.service.UploadService;
import de.fimatas.home.library.domain.model.PushNotifications;
import de.fimatas.home.library.model.SettingsContainer;
import de.fimatas.home.library.model.SettingsModel;

@Component
public class SettingsService {

    @Autowired
    private UploadService uploadService;

    @Autowired
    private PushService pushService;

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
                }
            });
            container.getSettings().add(model);
        });
        uploadService.upload(container);
    }

    public void createNewSettingsForToken(String token, String user, String client) {

        if (SettingsDAO.getInstance().read().stream().anyMatch(model -> model.getToken().equals(token))) {
            return;
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

        if (oldToken.isEmpty()) {
            pushService.sendRegistrationConfirmation(token, client);
        }

        refreshSettingsModelsComplete();
    }

    public void deleteSettingsForToken(String token) {
        SettingsDAO.getInstance().read().stream().filter(model -> model.getToken().equals(token)).findFirst()
            .ifPresent(SettingsDAO.getInstance()::delete);
        refreshSettingsModelsComplete();
    }

    public List<String> listTokensWithEnabledSetting(PushNotifications pushNotifications) {
        return SettingsDAO.getInstance().read().stream()
            .filter(model -> model.getPushNotifications().get(pushNotifications).booleanValue()).map(SettingsModel::getToken)
            .collect(Collectors.toList());
    }

}
