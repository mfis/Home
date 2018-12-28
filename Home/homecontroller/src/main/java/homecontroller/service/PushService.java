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

import homecontroller.domain.model.Hint;
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

	private static final Log LOG = LogFactory.getLog(PushService.class);

	public void send(HouseModel oldModel, HouseModel newModel) {

		String messages = formatMessages(oldModel, newModel);
		if (StringUtils.isBlank(messages)) {
			return;
		}

		List<SettingsModel> userForPushMessage = settingsService.lookupUserForPushMessage();

		for (SettingsModel settingsModel : userForPushMessage) {
			sendMessages(messages, settingsModel);
		}

	}

	public String formatMessages(HouseModel oldModel, HouseModel newModel) {

		List<String> oldHints = hintList(oldModel);
		List<String> newHints = hintList(newModel);

		StringBuilder messages = new StringBuilder(300);

		for (String newHint : newHints) {
			if (!oldHints.contains(newHint)) {
				messages.append("\n");
				messages.append("- " + newHint);
			}
		}

		int cancelcounter = 0;
		for (String oldHint : oldHints) {
			if (!newHints.contains(oldHint)) {
				messages.append("\n");
				if (cancelcounter == 0) {
					messages.append("Aufgehoben:");
				}
				messages.append("\n- " + oldHint);
				cancelcounter++;
			}
		}

		return messages.toString().trim();
	}

	private List<String> hintList(HouseModel model) {

		HouseModel m = model != null ? model : new HouseModel();

		List<String> hintStrings = new LinkedList<>();
		for (RoomClimate room : m.lookupFields(RoomClimate.class).values()) {
			for (Hint hint : room.getHints()) {
				if (hint != null) {
					hintStrings.add(hint.formatWithRoomName(room));
				}
			}
		}
		return hintStrings;
	}

	private void sendMessages(String messages, SettingsModel settingsModel) {

		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Void> future = executor.submit(new PushoverTask(messages, settingsModel));

		try {
			future.get(15, TimeUnit.SECONDS);
		} catch (TimeoutException | ExecutionException | InterruptedException e) { // NOSONAR
			// Future object is canceled here and the loss of the push
			// notification is no big problem
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
