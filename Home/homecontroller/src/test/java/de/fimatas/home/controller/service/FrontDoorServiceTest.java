package de.fimatas.home.controller.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import de.fimatas.home.controller.api.HomematicAPI;
import de.fimatas.home.controller.command.HomematicCommandBuilder;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.Doorlock;
import de.fimatas.home.library.domain.model.HouseModel;
import de.fimatas.home.library.domain.model.StateValue;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.model.Message;
import de.fimatas.home.library.model.PresenceModel;
import de.fimatas.home.library.model.PresenceState;
import mfi.files.api.UserService;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class FrontDoorServiceTest {

    @Mock
    private HomematicAPI hmApi;

    @Mock
    private HomematicCommandBuilder homematicCommandBuilder;

    @Mock
    private HouseService houseService;

    @Mock
    private UserService userService;

    @Mock
    private PushService pushService;

    @Mock
    private Environment env;

    @InjectMocks
    private FrontDoorService frontDoorService;

    private final String CORRECT_TEST_USER = "USER";
    private final String CORRECT_TEST_PIN = "PIN";

    private final String COMMAND_LOCK = "Lock";
    private final String COMMAND_UNLOCK = "Unlock";
    private final String COMMAND_OPEN = "Open";

    @BeforeAll
    public static void beforeAll(){
        ModelObjectDAO.resetAll();
    }

    @Test
    void testChangeDoorLockStateNoPin() {
        whenUserService();
        frontDoorService.changeDoorLockState(message(StateValue.LOCK, null), false);
        verifyFrontDoorCommand(COMMAND_LOCK, true);
    }

    @Test
    void testChangeDoorLockStateWrongPin() {
        whenUserService();
        frontDoorService.changeDoorLockState(message(StateValue.LOCK, false), false);
        verifyFrontDoorCommand(COMMAND_LOCK, true);
    }

    @Test
    void testChangeDoorLockStateCorrectPin() {
        whenUserService();
        frontDoorService.changeDoorLockState(message(StateValue.LOCK, true), false);
        verifyFrontDoorCommand(COMMAND_LOCK, true);
    }

    @Test
    void testChangeDoorUnLockStateNoPinButNotNeeded() {
        whenUserService();
        frontDoorService.changeDoorLockState(message(StateValue.UNLOCK, null), false);
        verifyFrontDoorCommand(COMMAND_UNLOCK, true);
    }

    @Test
    void testChangeDoorUnLockStateWrongPinButNotNeeded() {
        whenUserService();
        frontDoorService.changeDoorLockState(message(StateValue.UNLOCK, false), false);
        verifyFrontDoorCommand(COMMAND_UNLOCK, true);
    }

    @Test
    void testChangeDoorUnLockStateCorrectPinButNotNeeded() {
        whenUserService();
        frontDoorService.changeDoorLockState(message(StateValue.UNLOCK, true), false);
        verifyFrontDoorCommand(COMMAND_UNLOCK, true);
    }

    @Test
    void testChangeDoorUnLockStateNoPinAndIsNeeded() {
        whenUserService();
        frontDoorService.changeDoorLockState(message(StateValue.UNLOCK, null), true);
        verifyFrontDoorCommand(COMMAND_UNLOCK, false);
    }

    @Test
    void testChangeDoorUnLockStateWrongPinAndIsNeeded() {
        whenUserService();
        frontDoorService.changeDoorLockState(message(StateValue.UNLOCK, false), true);
        verifyFrontDoorCommand(COMMAND_UNLOCK, false);
    }

    @Test
    void testChangeDoorUnLockStateCorrectPinAndIsNeeded() {
        whenUserService();
        frontDoorService.changeDoorLockState(message(StateValue.UNLOCK, true), true);
        verifyFrontDoorCommand(COMMAND_UNLOCK, true);
    }

    @Test
    void testChangeDoorOpenStateNoPinButNotNeeded() {
        whenUserService();
        frontDoorService.changeDoorLockState(message(StateValue.OPEN, null), false);
        verifyFrontDoorCommand(COMMAND_OPEN, false);
    }

    @Test
    void testChangeDoorOpenStateWrongPinButNotNeeded() {
        whenUserService();
        frontDoorService.changeDoorLockState(message(StateValue.OPEN, false), false);
        verifyFrontDoorCommand(COMMAND_OPEN, false);
    }

    @Test
    void testChangeDoorOpenStateCorrectPinButNotNeeded() {
        whenUserService();
        frontDoorService.changeDoorLockState(message(StateValue.OPEN, true), false);
        verifyFrontDoorCommand(COMMAND_OPEN, true);
    }

    @Test
    void testChangeDoorOpenStateNoPinAndIsNeeded() {
        whenUserService();
        frontDoorService.changeDoorLockState(message(StateValue.OPEN, null), true);
        verifyFrontDoorCommand(COMMAND_OPEN, false);
    }

    @Test
    void testChangeDoorOpenStateWrongPinAndIsNeeded() {
        whenUserService();
        frontDoorService.changeDoorLockState(message(StateValue.OPEN, false), true);
        verifyFrontDoorCommand(COMMAND_OPEN, false);
    }

    @Test
    void testChangeDoorOpenStateCorrectPinAndIsNeeded() {
        whenUserService();
        frontDoorService.changeDoorLockState(message(StateValue.OPEN, true), true);
        verifyFrontDoorCommand(COMMAND_OPEN, true);
    }

    @Test
    void testHandlePresenceChangeToOne() {
        whenUserService();
        presence(1);
        doorLockState(StateValue.UNLOCK, true);
        frontDoorService.handlePresenceChange("user", PresenceState.PRESENT);
        verifyFrontDoorCommand(COMMAND_LOCK, false);
        verify(pushService, times(0)).doorLock(eq("user"));
    }

    @Test
    void testHandlePresenceChangeToZeroNoAutomation() {
        whenUserService();
        presence(0);
        doorLockState(StateValue.UNLOCK, false);
        frontDoorService.handlePresenceChange("user", PresenceState.PRESENT);
        verifyFrontDoorCommand(COMMAND_LOCK, false);
        verify(pushService, times(0)).doorLock(eq("user"));
    }

    @Test
    void testHandlePresenceChangeToZeroIsLocked() {
        whenUserService();
        presence(0);
        doorLockState(StateValue.LOCK, true);
        frontDoorService.handlePresenceChange("user", PresenceState.PRESENT);
        verifyFrontDoorCommand(COMMAND_LOCK, false);
        verify(pushService, times(0)).doorLock(eq("user"));
    }

    @Test
    void testHandlePresenceChangeToZeroAndLock() {
        whenUserService();
        presence(0);
        doorLockState(StateValue.UNLOCK, true);
        frontDoorService.handlePresenceChange("user", PresenceState.AWAY);
        verifyFrontDoorCommand(COMMAND_LOCK, true);
        verify(pushService, times(1)).doorLock(eq("user"));
    }

    @Test
    void testLockDoorInTheEveningUnlockPresence0() {
        whenUserService();
        presence(0);
        doorLockState(StateValue.UNLOCK, true);
        frontDoorService.lockDoorInTheEvening();
        verifyFrontDoorCommand(COMMAND_LOCK, true);
    }

    @Test
    void testLockDoorInTheEveningUnlockPresence1() {
        whenUserService();
        presence(1);
        doorLockState(StateValue.UNLOCK, true);
        frontDoorService.lockDoorInTheEvening();
        verifyFrontDoorCommand(COMMAND_LOCK, true);
    }

    @Test
    void testLockDoorInTheEveningUnlockNoAutomation() {
        whenUserService();
        presence(1);
        doorLockState(StateValue.UNLOCK, false);
        frontDoorService.lockDoorInTheEvening();
        verifyFrontDoorCommand(COMMAND_LOCK, false);
    }

    @Test
    void testLockDoorInTheEveningLock() {
        whenUserService();
        presence(1);
        doorLockState(StateValue.LOCK, true);
        frontDoorService.lockDoorInTheEvening();
        verifyFrontDoorCommand(COMMAND_LOCK, false);
    }

    private void verifyFrontDoorCommand(String command, boolean invocation) {
        verify(hmApi, times(invocation?1:0)).runProgramWithBusyState(eq(Device.HAUSTUER_SCHLOSS), eq(command));
        verify(homematicCommandBuilder, times(invocation?1:0)).write(eq(Device.HAUSTUER_SCHLOSS), eq("Ansteuerung"), eq(true));
    }

    private void doorLockState(StateValue stateValue, boolean automation) {
        var houseModel = new HouseModel();
        var doorLock = new Doorlock();
        if(stateValue == StateValue.OPEN){
            doorLock.setOpen(true);
        }else{
            doorLock.setLockState(stateValue == StateValue.LOCK);
        }
        doorLock.setLockAutomation(automation);
        doorLock.setUnreach(false);
        houseModel.setFrontDoorLock(doorLock);
        ModelObjectDAO.getInstance().write(houseModel);
    }

    private void presence(int count) {
        var presendeModel = new PresenceModel();
        for(int i = 0 ; i < 5 ; i++){
            presendeModel.getPresenceStates().put(i + "", i >= count ? PresenceState.AWAY : PresenceState.PRESENT);
        }
        ModelObjectDAO.getInstance().write(presendeModel);
    }

    private Message message(StateValue stateValue, Boolean correctPin) {
        var m = new Message();
        m.setDevice(Device.HAUSTUER_SCHLOSS);
        m.setValue(stateValue.name());
        if(correctPin != null){
            if(correctPin){
                m.setUser(CORRECT_TEST_USER);
                m.setSecurityPin(CORRECT_TEST_PIN);
            }else{
                m.setUser("x");
                m.setSecurityPin("y");
            }
        }
        return m;
    }

    private void whenUserService() {
        lenient().when(userService.checkPin(anyString(), anyString())).thenReturn(false);
        lenient().when(userService.checkPin(CORRECT_TEST_USER, CORRECT_TEST_PIN)).thenReturn(true);
    }
}
