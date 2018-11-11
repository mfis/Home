package homecontroller.service;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.RoomClimate;
import homecontroller.domain.model.SettingsModel;
import net.pushover.client.MessagePriority;
import net.pushover.client.PushoverClient;
import net.pushover.client.PushoverException;
import net.pushover.client.PushoverMessage;
import net.pushover.client.PushoverRestClient;
import net.pushover.client.Status;

@Component
public class PushService {

	@Autowired
	private SettingsService settingsService;

	// private final static String CROSS = "\u274C";

	private final static Log LOG = LogFactory.getLog(PushService.class);

	// public static void main(String[] args) {
	//
	// // TEST
	// long started = System.currentTimeMillis();
	//
	// PushService ps = new PushService();
	// ps.settingsService = new SettingsService();
	//
	// HouseModel oldModel = new HouseModel();
	// oldModel.setConclusionHintBathRoom(new Hint("Fenster öffnen",
	// "Badezimmer"));
	// oldModel.setConclusionHintLivingRoom(new Hint("Gleich Wohn",
	// "Wohnzimmer"));
	//
	// HouseModel newModel = new HouseModel();
	// newModel.setConclusionHintLivingRoom(new Hint("Gleich Wohn",
	// "Wohnzimmer"));
	// newModel.setConclusionHintKidsRoom(new Hint("Fenster öffnen",
	// "Kinderzimmer"));
	// newModel.setConclusionHintBedRoom(new Hint("Rolladen schließen",
	// "Schlafzimmer"));
	//
	// ps.send(oldModel, newModel);
	//
	// long ended = System.currentTimeMillis();
	// System.out.println("Duration: " + (ended - started) + "ms");
	// // TEST
	// }

	public void send(HouseModel oldModel, HouseModel newModel) {

		String messages = formatMessages(hintList(oldModel), hintList(newModel));
		if (StringUtils.isBlank(messages)) {
			return;
		}

		List<SettingsModel> userForPushMessage = settingsService.lookupUserForPushMessage();

		for (SettingsModel settingsModel : userForPushMessage) {
			sendMessages(messages, settingsModel);
		}

	}

	private String formatMessages(List<String> oldHints, List<String> newHints) {

		StringBuilder messages = new StringBuilder(300);

		int hintcounter = 0;
		for (int i = 0; i < oldHints.size(); i++) {
			if (!StringUtils.equals(oldHints.get(i), newHints.get(i))
					&& StringUtils.isBlank(oldHints.get(i))) {
				if (hintcounter > 0) {
					messages.append("\n");
				}
				messages.append("- " + newHints.get(i));
				hintcounter++;
			}
		}

		int cancelcounter = 0;
		for (int i = 0; i < oldHints.size(); i++) {
			if (!StringUtils.equals(oldHints.get(i), newHints.get(i))
					&& StringUtils.isBlank(newHints.get(i))) {
				if (hintcounter > 0 && cancelcounter == 0) {
					messages.append("\n");
				}
				if (cancelcounter == 0) {
					messages.append("Aufgehoben:");
				}
				messages.append("\n- " + oldHints.get(i));
				cancelcounter++;
			}
		}

		return messages.toString();
	}

	private List<String> hintList(HouseModel model) {

		HouseModel m = model != null ? model : new HouseModel();

		List<String> hintStrings = new LinkedList<>();
		for (RoomClimate room : m.lookupRooms()) {
			hintStrings.add((room != null && room.getHint() != null) ? room.getHint().formatWithRoomName(room)
					: null);
		}
		return hintStrings;
	}

	private void sendMessages(String messages, SettingsModel settingsModel) {

		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Void> future = executor.submit(new PushoverTask(messages, settingsModel));

		try {
			future.get(15, TimeUnit.SECONDS);
		} catch (TimeoutException | ExecutionException | InterruptedException e) {
			future.cancel(true);
			LOG.error("Could not send push message (#3).", e);
		}

		executor.shutdownNow();
	}

	private class PushoverTask implements Callable<Void> {

		private String messages;
		private SettingsModel settingsModel;

		public PushoverTask(String messages, SettingsModel settingsModel) {
			this.messages = messages;
			this.settingsModel = settingsModel;
		}

		@Override
		public Void call() throws Exception {

			PushoverClient client = new PushoverRestClient();

			try {
				Status result = client
						.pushMessage(PushoverMessage.builderWithApiToken(settingsModel.getPushoverApiToken()) //
								.setUserId(settingsModel.getPushoverUserId()) //
								.setDevice(settingsModel.getPushoverDevice()) //
								.setMessage(messages) //
								.setPriority(MessagePriority.NORMAL) //
								.setTitle("Zuhause - Empfehlungen") //
								.build()); //

				if (result.getStatus() != 1) {
					LOG.error("Could not send push message (#1):" + result.toString());
				}

			} catch (PushoverException pe) {
				LOG.error("Could not send push message (#2).", pe);
			}

			return null;
		}
	}

}
