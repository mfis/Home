package homecontroller.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import homecontroller.domain.model.Hint;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.RoomClimate;

@RunWith(MockitoJUnitRunner.class)
public class PushServiceTest {

	@InjectMocks
	private PushService pushService;

	@Test
	public void testFormatMessagesAllNew() {

		HouseModel oldModel = new HouseModel();
		oldModel.setClimateBathRoom(new RoomClimate());
		oldModel.getClimateBathRoom().setPlaceName("Bad");
		oldModel.setClimateLivingRoom(new RoomClimate());
		oldModel.getClimateLivingRoom().setPlaceName("Wohn");

		HouseModel newModel = new HouseModel();
		newModel.setClimateBathRoom(new RoomClimate());
		newModel.getClimateBathRoom().setPlaceName("Bad");
		newModel.setClimateLivingRoom(new RoomClimate());
		newModel.getClimateLivingRoom().setPlaceName("Wohn");
		newModel.getClimateBathRoom().getHints().add(Hint.OPEN_WINDOW); // same
		newModel.getClimateLivingRoom().getHints().add(Hint.INCREASE_HUMIDITY); // same
		newModel.getClimateLivingRoom().getHints().add(Hint.CLOSE_ROLLER_SHUTTER); // new
		// getClimateBathRoom().getHints().add(Hint.DECREASE_HUMIDITY); removed

		String actual = pushService.formatMessages(oldModel, newModel);
		System.out.println(actual);
		String expected = "- Bad: Fenster öffnen\n- Wohn: Luftfeuchtigkeit erhöhen\n- Wohn: Rolllade schließen";
		assertEquals(expected, actual);
	}

	@Test
	public void testFormatMessagesChanged() {

		HouseModel oldModel = new HouseModel();
		oldModel.setClimateBathRoom(new RoomClimate());
		oldModel.getClimateBathRoom().setPlaceName("Bad");
		oldModel.setClimateLivingRoom(new RoomClimate());
		oldModel.getClimateLivingRoom().setPlaceName("Wohn");
		oldModel.getClimateBathRoom().getHints().add(Hint.OPEN_WINDOW);
		oldModel.getClimateBathRoom().getHints().add(Hint.DECREASE_HUMIDITY);
		oldModel.getClimateLivingRoom().getHints().add(Hint.INCREASE_HUMIDITY);

		HouseModel newModel = new HouseModel();
		newModel.setClimateBathRoom(new RoomClimate());
		newModel.getClimateBathRoom().setPlaceName("Bad");
		newModel.setClimateLivingRoom(new RoomClimate());
		newModel.getClimateLivingRoom().setPlaceName("Wohn");
		newModel.getClimateBathRoom().getHints().add(Hint.OPEN_WINDOW); // same
		newModel.getClimateLivingRoom().getHints().add(Hint.INCREASE_HUMIDITY); // same
		newModel.getClimateLivingRoom().getHints().add(Hint.CLOSE_ROLLER_SHUTTER); // new

		String actual = pushService.formatMessages(oldModel, newModel);
		String expected = "- Wohn: Rolllade schließen\nAufgehoben:\n- Bad: Luftfeuchtigkeit verringern";
		assertEquals(expected, actual);
	}

}
