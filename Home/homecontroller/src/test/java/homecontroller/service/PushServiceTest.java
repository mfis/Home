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

import homecontroller.domain.model.Hint;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.RoomClimate;
import homecontroller.domain.model.SettingsModel;
import homelibrary.homematic.model.Device;

@RunWith(MockitoJUnitRunner.class)
public class PushServiceTest {

	@InjectMocks
	private PushService pushService;

	@Mock
	private SettingsService settingsService;

	@Test
	public void testFormatMessagesAllNew() {

		mockSettings();
		long dateTimeOld = System.currentTimeMillis() - (1000 * 60 * 60);
		long dateTimeNew = System.currentTimeMillis();

		HouseModel oldModel = new HouseModel();
		oldModel.setDateTime(dateTimeOld);
		oldModel.setClimateBathRoom(new RoomClimate());
		oldModel.getClimateBathRoom().setDevice(Device.THERMOSTAT_BAD);
		oldModel.setClimateLivingRoom(new RoomClimate());
		oldModel.getClimateLivingRoom().setDevice(Device.THERMOMETER_WOHNZIMMER);

		HouseModel newModel = new HouseModel();
		newModel.setClimateBathRoom(new RoomClimate());
		newModel.getClimateBathRoom().setDevice(Device.THERMOSTAT_BAD);
		newModel.setClimateLivingRoom(new RoomClimate());

		newModel.getClimateBathRoom().getHints().overtakeOldHints(oldModel.getClimateBathRoom().getHints(),
				dateTimeNew);
		newModel.getClimateLivingRoom().getHints()
				.overtakeOldHints(oldModel.getClimateLivingRoom().getHints(), dateTimeNew);

		newModel.getClimateLivingRoom().setDevice(Device.THERMOMETER_WOHNZIMMER);
		newModel.getClimateBathRoom().getHints().giveHint(Hint.OPEN_WINDOW, dateTimeNew); // same
		newModel.getClimateLivingRoom().getHints().giveHint(Hint.INCREASE_HUMIDITY, dateTimeNew); // same
		newModel.getClimateLivingRoom().getHints().giveHint(Hint.CLOSE_ROLLER_SHUTTER, dateTimeNew); // new
		// getClimateBathRoom().getHints().add(Hint.DECREASE_HUMIDITY); removed

		String actual = pushService.hintMessage(oldModel, newModel).get(0).getMessage();
		System.out.println(actual);
		String expected = "- Bad: Fenster öffnen\n- Wohnzimmer: Luftfeuchtigkeit erhöhen\n- Wohnzimmer: Rolllade schließen";
		assertEquals(expected, actual);
	}

	@Test
	public void testFormatMessagesChanged() {

		mockSettings();
		long dateTimeOld = System.currentTimeMillis() - (1000 * 60 * 60);
		long dateTimeNew = System.currentTimeMillis();

		HouseModel oldModel = new HouseModel();
		oldModel.setDateTime(dateTimeOld);
		oldModel.setClimateBathRoom(new RoomClimate());
		oldModel.getClimateBathRoom().setDevice(Device.THERMOSTAT_BAD);
		oldModel.setClimateLivingRoom(new RoomClimate());
		oldModel.getClimateLivingRoom().setDevice(Device.THERMOMETER_WOHNZIMMER);

		oldModel.getClimateBathRoom().getHints().giveHint(Hint.OPEN_WINDOW, dateTimeOld);
		oldModel.getClimateBathRoom().getHints().giveHint(Hint.DECREASE_HUMIDITY, dateTimeOld);
		oldModel.getClimateLivingRoom().getHints().giveHint(Hint.INCREASE_HUMIDITY, dateTimeOld);

		HouseModel newModel = new HouseModel();
		newModel.setDateTime(dateTimeNew);
		newModel.setClimateBathRoom(new RoomClimate());
		newModel.getClimateBathRoom().setDevice(Device.THERMOSTAT_BAD);
		newModel.setClimateLivingRoom(new RoomClimate());
		newModel.getClimateLivingRoom().setDevice(Device.THERMOMETER_WOHNZIMMER);

		newModel.getClimateBathRoom().getHints().overtakeOldHints(oldModel.getClimateBathRoom().getHints(),
				dateTimeNew);
		newModel.getClimateLivingRoom().getHints()
				.overtakeOldHints(oldModel.getClimateLivingRoom().getHints(), dateTimeNew);

		newModel.getClimateBathRoom().getHints().giveHint(Hint.OPEN_WINDOW, dateTimeNew); // same
		newModel.getClimateLivingRoom().getHints().giveHint(Hint.INCREASE_HUMIDITY, dateTimeNew); // same
		newModel.getClimateLivingRoom().getHints().giveHint(Hint.CLOSE_ROLLER_SHUTTER, dateTimeNew); // new

		String actual = pushService.hintMessage(oldModel, newModel).get(0).getMessage();
		String expected = "- Wohnzimmer: Rolllade schließen\nAufgehoben:\n- Bad: Luftfeuchtigkeit verringern";
		assertEquals(expected, actual);
	}

	private void mockSettings() {
		List<SettingsModel> settings = new LinkedList<SettingsModel>();
		SettingsModel settingsModel = new SettingsModel();
		settingsModel.setPushHints(true);
		settings.add(settingsModel);
		when(settingsService.lookupUserForPushMessage()).thenReturn(settings);
	}

}
