package homecontroller.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import homecontroller.domain.model.Device;
import homecontroller.domain.model.Hint;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.RoomClimate;
import homecontroller.domain.model.SettingsModel;

@RunWith(MockitoJUnitRunner.class)
public class PushServiceTest {

	@InjectMocks
	private PushService pushService;

	@Mock
	private SettingsService settingsService;

	@Test
	public void testFormatMessagesAllNew() {

		mockSettings();

		HouseModel oldModel = new HouseModel();
		oldModel.setClimateBathRoom(new RoomClimate());
		oldModel.getClimateBathRoom().setDevice(Device.THERMOSTAT_BAD);
		oldModel.setClimateLivingRoom(new RoomClimate());
		oldModel.getClimateLivingRoom().setDevice(Device.THERMOMETER_WOHNZIMMER);

		HouseModel newModel = new HouseModel();
		newModel.setClimateBathRoom(new RoomClimate());
		newModel.getClimateBathRoom().setDevice(Device.THERMOSTAT_BAD);
		newModel.setClimateLivingRoom(new RoomClimate());
		newModel.getClimateLivingRoom().setDevice(Device.THERMOMETER_WOHNZIMMER);
		newModel.getClimateBathRoom().getHints().add(Hint.OPEN_WINDOW); // same
		newModel.getClimateLivingRoom().getHints().add(Hint.INCREASE_HUMIDITY); // same
		newModel.getClimateLivingRoom().getHints().add(Hint.CLOSE_ROLLER_SHUTTER); // new
		// getClimateBathRoom().getHints().add(Hint.DECREASE_HUMIDITY); removed

		String actual = pushService.hintMessage(oldModel, newModel).get(0).getMessage();
		System.out.println(actual);
		String expected = "- Bad: Fenster öffnen\n- Wohnzimmer: Luftfeuchtigkeit erhöhen\n- Wohnzimmer: Rolllade schließen";
		assertEquals(expected, actual);
	}

	@Test
	public void testFormatMessagesChanged() {

		mockSettings();

		HouseModel oldModel = new HouseModel();
		oldModel.setClimateBathRoom(new RoomClimate());
		oldModel.getClimateBathRoom().setDevice(Device.THERMOSTAT_BAD);
		oldModel.setClimateLivingRoom(new RoomClimate());
		oldModel.getClimateLivingRoom().setDevice(Device.THERMOMETER_WOHNZIMMER);
		oldModel.getClimateBathRoom().getHints().add(Hint.OPEN_WINDOW);
		oldModel.getClimateBathRoom().getHints().add(Hint.DECREASE_HUMIDITY);
		oldModel.getClimateLivingRoom().getHints().add(Hint.INCREASE_HUMIDITY);

		HouseModel newModel = new HouseModel();
		newModel.setClimateBathRoom(new RoomClimate());
		newModel.getClimateBathRoom().setDevice(Device.THERMOSTAT_BAD);
		newModel.setClimateLivingRoom(new RoomClimate());
		newModel.getClimateLivingRoom().setDevice(Device.THERMOMETER_WOHNZIMMER);
		newModel.getClimateBathRoom().getHints().add(Hint.OPEN_WINDOW); // same
		newModel.getClimateLivingRoom().getHints().add(Hint.INCREASE_HUMIDITY); // same
		newModel.getClimateLivingRoom().getHints().add(Hint.CLOSE_ROLLER_SHUTTER); // new

		String actual = pushService.hintMessage(oldModel, newModel).get(0).getMessage();
		String expected = "- Wohnzimmer: Rolllade schließen\nAufgehoben:\n- Bad: Luftfeuchtigkeit verringern";
		assertEquals(expected, actual);
	}

	private void mockSettings() {
		List<SettingsModel> settings = new LinkedList<SettingsModel>();
		settings.add(new SettingsModel());
		when(settingsService.lookupUserForPushMessage()).thenReturn(settings);
	}

}
