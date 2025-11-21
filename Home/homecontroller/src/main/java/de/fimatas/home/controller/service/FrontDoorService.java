package de.fimatas.home.controller.service;

import de.fimatas.home.controller.api.HomematicAPI;
import de.fimatas.home.controller.command.HomematicCommandBuilder;
import de.fimatas.home.controller.dao.TicketDAO;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.Doorlock;
import de.fimatas.home.library.domain.model.StateValue;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.model.Message;
import de.fimatas.home.library.model.PresenceState;
import lombok.extern.apachecommons.CommonsLog;
import mfi.files.api.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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
    private PushService pushService;

    @Autowired
    private TicketDAO ticketDAO;

    private final Device DEFAULT_DEVICE = Device.HAUSTUER_SCHLOSS;

    private final static String TICKET_EVENT = "FRONTDOOR";

    private LocalDateTime cachedLastOpened = null;

    @Scheduled(cron = "04 30 19,21 * * *")
    public void lockDoorInTheEvening() {
        if (isDoorLockAutomaticAndNotInState(StateValue.LOCK)) {
            changeDoorLockState(messageForDoorState(StateValue.LOCK), true);
        }
    }

    public void handlePresenceChange(String username, PresenceState state) {
        if(state == PresenceState.AWAY && isNoOneAtHome() && isDoorLockAutomaticAndNotInState(StateValue.LOCK) ) {
            changeDoorLockState(messageForDoorState(StateValue.LOCK), true);
            pushService.doorLock(username);
        }
    }

    public void changeDoorLockState(Message message, boolean unlockOnlyWithSecutityPin) {

        if(StringUtils.isBlank(message.getAdditionalData())){
            log.error("no ticket provided!");
            message.setSecurityPin("");
            return;
        }

        if(ticketDAO.existsUsedTicket(message.getAdditionalData())){
            log.error("ticket already exists: " + message.getAdditionalData());
            message.setSecurityPin("");
            return;
        }

        // check pin?
        boolean checkPin = switch (StateValue.valueOf(message.getValue())) {
            case LOCK -> false;
            case UNLOCK -> unlockOnlyWithSecutityPin;
            default -> true;
        };

        if (!checkPin || isSecurityPinCorrect(message)) {
            ticketDAO.write(message.getAdditionalData(), TICKET_EVENT, message.getValue());
            hmApi.executeCommand(homematicCommandBuilder.write(message.getDevice(), "Ansteuerung", true));
            switch (StateValue.valueOf(message.getValue())) {
                case LOCK -> hmApi.runProgramWithBusyState(message.getDevice(), "Lock");
                case UNLOCK -> hmApi.runProgramWithBusyState(message.getDevice(), "Unlock");
                case OPEN -> {
                    log.info("###### FRONTDOOR DO " + message.getValue() + " WITH TICKET " + message.getAdditionalData() + " #####");
                    cachedLastOpened = LocalDateTime.now();
                    hmApi.runProgramWithBusyState(message.getDevice(), "Open");
                }
            }
            houseService.refreshHouseModel(false);
        }
    }

    public LocalDateTime readLastOpened(){
        if(cachedLastOpened == null) {
            log.info("empty cache - read database");
            var lastOpened = ticketDAO.readLatestTicket(TICKET_EVENT, StateValue.OPEN.name());
            if(lastOpened != null){
                cachedLastOpened = lastOpened.getTimestamp();
            }
        }
        return cachedLastOpened;
    }

    private boolean isSecurityPinCorrect(Message message) {
        return StringUtils.isNotBlank(message.getSecurityPin())
                && userService.checkPin(message.getUser(), message.getSecurityPin());
    }

    private Message messageForDoorState(StateValue stateValue) {
        var m = new Message();
        m.setDevice(DEFAULT_DEVICE);
        m.setValue(stateValue.name());
        return m;
    }

    private boolean isDoorLockAutomaticAndNotInState(StateValue stateValue) {
        var frontDoorModel = getFrontDoorModel();
        return frontDoorModel != null && !frontDoorModel.isUnreach()
                && frontDoorModel.getLockAutomation()
                && stateValueFromModel(frontDoorModel) != stateValue;
    }

    private StateValue stateValueFromModel(Doorlock doorlock) {
        return doorlock.isOpen() ? StateValue.OPEN : (doorlock.isLockState() ? StateValue.LOCK : StateValue.UNLOCK);
    }

    private static Doorlock getFrontDoorModel() {
        return ModelObjectDAO.getInstance().readHouseModel() != null ? ModelObjectDAO.getInstance().readHouseModel().getFrontDoorLock() : null;
    }

    private boolean isNoOneAtHome() {
        return ModelObjectDAO.getInstance().readPresenceModel().getPresenceStates().entrySet()
                .stream().noneMatch(e -> e.getValue() == PresenceState.PRESENT);
    }
}
