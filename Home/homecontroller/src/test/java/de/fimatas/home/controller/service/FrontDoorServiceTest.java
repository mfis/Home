package de.fimatas.home.controller.service;

import static org.mockito.ArgumentMatchers.eq;
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

    @BeforeAll
    public static void beforeAll(){
        ModelObjectDAO.resetAll();
    }

    @Test
    void testChangeDoorLockState() {

    }

    @Test
    void testHandlePresenceChangeToOne() {
        presence(1);
        doorLockState(StateValue.UNLOCK, true);
        frontDoorService.handlePresenceChange("user", PresenceState.PRESENT);
        verify(hmApi, times(0)).runProgramWithBusyState(eq(Device.HAUSTUER_SCHLOSS), eq("Lock"));
        verify(homematicCommandBuilder, times(0)).write(eq(Device.HAUSTUER_SCHLOSS), eq("Ansteuerung"), eq(true));
        verify(pushService, times(0)).doorLock(eq("user"));
    }

    @Test
    void testHandlePresenceChangeToZeroNoAutomation() {
        presence(0);
        doorLockState(StateValue.UNLOCK, false);
        frontDoorService.handlePresenceChange("user", PresenceState.PRESENT);
        verify(hmApi, times(0)).runProgramWithBusyState(eq(Device.HAUSTUER_SCHLOSS), eq("Lock"));
        verify(homematicCommandBuilder, times(0)).write(eq(Device.HAUSTUER_SCHLOSS), eq("Ansteuerung"), eq(true));
        verify(pushService, times(0)).doorLock(eq("user"));
    }

    @Test
    void testHandlePresenceChangeToZeroIsLocked() {
        presence(0);
        doorLockState(StateValue.LOCK, true);
        frontDoorService.handlePresenceChange("user", PresenceState.PRESENT);
        verify(hmApi, times(0)).runProgramWithBusyState(eq(Device.HAUSTUER_SCHLOSS), eq("Lock"));
        verify(homematicCommandBuilder, times(0)).write(eq(Device.HAUSTUER_SCHLOSS), eq("Ansteuerung"), eq(true));
        verify(pushService, times(0)).doorLock(eq("user"));
    }

    @Test
    void testHandlePresenceChangeToZeroAndLock() {
        presence(0);
        doorLockState(StateValue.UNLOCK, true);
        frontDoorService.handlePresenceChange("user", PresenceState.AWAY);
        verify(hmApi, times(1)).runProgramWithBusyState(eq(Device.HAUSTUER_SCHLOSS), eq("Lock"));
        verify(homematicCommandBuilder, times(1)).write(eq(Device.HAUSTUER_SCHLOSS), eq("Ansteuerung"), eq(true));
        verify(pushService, times(1)).doorLock(eq("user"));
    }

    @Test
    void testLockDoorInTheEveningUnlockPresence0() {
        presence(0);
        doorLockState(StateValue.UNLOCK, true);
        frontDoorService.lockDoorInTheEvening();
        verify(hmApi, times(1)).runProgramWithBusyState(eq(Device.HAUSTUER_SCHLOSS), eq("Lock"));
        verify(homematicCommandBuilder, times(1)).write(eq(Device.HAUSTUER_SCHLOSS), eq("Ansteuerung"), eq(true));
    }

    @Test
    void testLockDoorInTheEveningUnlockPresence1() {
        presence(1);
        doorLockState(StateValue.UNLOCK, true);
        frontDoorService.lockDoorInTheEvening();
        verify(hmApi, times(1)).runProgramWithBusyState(eq(Device.HAUSTUER_SCHLOSS), eq("Lock"));
        verify(homematicCommandBuilder, times(1)).write(eq(Device.HAUSTUER_SCHLOSS), eq("Ansteuerung"), eq(true));
    }

    @Test
    void testLockDoorInTheEveningUnlockNoAutomation() {
        presence(1);
        doorLockState(StateValue.UNLOCK, false);
        frontDoorService.lockDoorInTheEvening();
        verify(hmApi, times(0)).runProgramWithBusyState(eq(Device.HAUSTUER_SCHLOSS), eq("Lock"));
        verify(homematicCommandBuilder, times(0)).write(eq(Device.HAUSTUER_SCHLOSS), eq("Ansteuerung"), eq(true));
    }

    @Test
    void testLockDoorInTheEveningLock() {
        presence(1);
        doorLockState(StateValue.LOCK, true);
        frontDoorService.lockDoorInTheEvening();
        verify(hmApi, times(0)).runProgramWithBusyState(eq(Device.HAUSTUER_SCHLOSS), eq("Lock"));
        verify(homematicCommandBuilder, times(0)).write(eq(Device.HAUSTUER_SCHLOSS), eq("Ansteuerung"), eq(true));
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
}
