package de.fimatas.home.controller.service;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import de.fimatas.home.controller.dao.SettingsDAO;
import de.fimatas.home.controller.domain.service.HistoryService;
import de.fimatas.home.controller.domain.service.UploadService;
import de.fimatas.home.library.domain.model.PushNotifications;
import de.fimatas.home.library.domain.model.SettingsModel;

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
        SettingsDAO.getInstance().read().forEach(model -> uploadService.upload(model));
    }

    public void createNewSettingsForToken(String token, String user) {

        final var model = new SettingsModel();
        model.setToken(token);
        model.setLastTimestamp(System.currentTimeMillis());
        model.setUser(user);
        List.of(PushNotifications.values())
            .forEach(notification -> model.getPushNotifications().put(notification, notification.getDefaultSetting()));
        SettingsDAO.getInstance().write(model);
        uploadService.upload(model);
    }

    public void deleteSettingsForToken(String token) {
        SettingsDAO.getInstance().read().stream().filter(model -> model.getToken().equals(token)).findFirst()
            .ifPresent(SettingsDAO.getInstance()::delete);
    }

    public List<String> listTokensWithEnabledSetting(PushNotifications pushNotifications) {
        return SettingsDAO.getInstance().read().stream().filter(model -> model.getPushNotifications().get(pushNotifications).booleanValue())
            .map(SettingsModel::getToken)
            .collect(Collectors.toList());
    }

}
