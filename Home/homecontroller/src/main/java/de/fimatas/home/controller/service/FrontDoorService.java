package de.fimatas.home.controller.service;

import de.fimatas.home.controller.api.HomematicAPI;
import de.fimatas.home.controller.command.HomematicCommandBuilder;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.StateValue;
import de.fimatas.home.library.model.Message;
import lombok.extern.apachecommons.CommonsLog;
import mfi.files.api.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
public class FrontDoorService {

    @Autowired
    private HomematicAPI hmApi;

    @Autowired
    private HomematicCommandBuilder homematicCommandBuilder;

    @Autowired
    private HouseService houseService;

    @Autowired
    private UserService userService;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private Environment env;

    @Scheduled(cron = "04 30 19,21 * * *")
    public void lockDoorInTheEvening() {

        var frontDoorModel = ModelObjectDAO.getInstance().readHouseModel() != null ? ModelObjectDAO.getInstance().readHouseModel().getFrontDoorLock() : null;
        if (frontDoorModel != null && !frontDoorModel.isUnreach() && frontDoorModel.getLockAutomation()) {
            doorState(messageForDoorState(StateValue.LOCK), true);
            houseService.refreshHouseModel(false);
        }
    }

    public void doorState(Message message, boolean unlockOnlyWithSecutityPin) {

        // check pin?
        boolean checkPin = switch (StateValue.valueOf(message.getValue())) {
            case LOCK -> false;
            case UNLOCK -> unlockOnlyWithSecutityPin;
            default -> true;
        };

        if (!checkPin || isSecurityPinCorrect(message)) {
            hmApi.executeCommand(homematicCommandBuilder.write(message.getDevice(), "Ansteuerung", true));
            switch (StateValue.valueOf(message.getValue())) {
                case LOCK -> hmApi.runProgramWithBusyState(message.getDevice(), "Lock");
                case UNLOCK -> hmApi.runProgramWithBusyState(message.getDevice(), "Unlock");
                case OPEN -> hmApi.runProgramWithBusyState(message.getDevice(), "Open");
            }
        }
    }

    private boolean isSecurityPinCorrect(Message message) {
        return StringUtils.isNotBlank(message.getSecurityPin())
                && userService.checkPin(message.getUser(), message.getSecurityPin());
    }

    private Message messageForDoorState(StateValue stateValue) {
        var m = new Message();
        m.setValue(stateValue.name());
        return m;
    }
}
