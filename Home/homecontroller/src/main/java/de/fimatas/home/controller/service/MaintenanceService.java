package de.fimatas.home.controller.service;

import de.fimatas.home.library.model.MaintenanceOptions;
import de.fimatas.home.library.model.Message;
import lombok.extern.apachecommons.CommonsLog;
import mfi.files.api.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
public class MaintenanceService {

    @Autowired
    private UserService userService;

    @Autowired
    private ClientCommunicationService clientCommunicationService;

    public void doMaintenance(Message message){
        if (StringUtils.isNotBlank(message.getSecurityPin())
                && userService.checkPin(message.getUser(), message.getSecurityPin())) {

            var maintenanceOption = MaintenanceOptions.valueOf(message.getValue());
            log.warn("DO_MAINTENANCE: " + maintenanceOption);
            switch (maintenanceOption){
                case REFRESH_MODELS -> clientCommunicationService.refreshAll();
                case CONTROLLER_REBOOT -> controllerReboot();
            }
        }
    }

    private void controllerReboot() {
        log.warn("MAINTENANCE: Rebooting controller");
    }
}
