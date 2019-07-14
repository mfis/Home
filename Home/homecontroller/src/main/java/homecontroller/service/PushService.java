package homecontroller.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import homecontroller.domain.model.Hint;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.RoomClimate;
import homecontroller.domain.model.SettingsModel;
import homecontroller.domain.service.HouseService;
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

	private PushoverClient pushClient;

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

	private static final Log LOG = LogFactory.getLog(PushService.class);

	@PostConstruct
	public void init() {
		pushClient = new PushoverRestClient();
	}

	public synchronized void send(HouseModel oldModel, HouseModel newModel) {

		try {
			List<PushoverMessage> pushMessages = new LinkedList<>();

			pushMessages.addAll(hintMessage(oldModel, newModel));
			pushMessages.addAll(doorbellMessage(oldModel, newModel));

			for (PushoverMessage pushMessage : pushMessages) {
				sendMessages(pushMessage);
			}
		} catch (Exception e) {
			LogFactory.getLog(PushService.class).error("Could not send push notifications:", e);
		}
	}

	public List<PushoverMessage> hintMessage(HouseModel oldModel, HouseModel newModel) {

		List<PushoverMessage> pushMessages = new LinkedList<>();
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

		String msgString = messages.toString().trim();
		if (StringUtils.isNotBlank(msgString)) {
			List<SettingsModel> settingsModels = settingsService.lookupUserForPushMessage();
			for (SettingsModel settingsModel : settingsModels) {
				pushMessages.add(PushoverMessage.builderWithApiToken(settingsModel.getPushoverApiToken()) //
						.setUserId(settingsModel.getPushoverUserId()) //
						.setDevice(settingsModel.getPushoverDevice()) //
						.setMessage(msgString) //
						.setPriority(MessagePriority.NORMAL) //
						.setTitle("Zuhause - Empfehlungen") //
						.build());
			}
		}
		return pushMessages;
	}

	private List<PushoverMessage> doorbellMessage(HouseModel oldModel, HouseModel newModel) {

		List<PushoverMessage> pushMessages = new LinkedList<>();

		if (!HouseService.doorbellTimestampChanged(oldModel, newModel)) {
			return pushMessages;
		}
		List<SettingsModel> settingsModels = settingsService.lookupUserForPushMessage();
		for (SettingsModel settingsModel : settingsModels) {
			String time = TIME_FORMATTER
					.format(Instant.ofEpochMilli(newModel.getFrontDoor().getTimestampLastDoorbell())
							.atZone(ZoneId.systemDefault()).toLocalDate());
			pushMessages.add(PushoverMessage.builderWithApiToken(settingsModel.getPushoverApiToken()) //
					.setUserId(settingsModel.getPushoverUserId()) //
					.setDevice(settingsModel.getPushoverDevice()) //
					.setMessage("Türklingelbetätigung um " + time + " Uhr") //
					.setPriority(MessagePriority.HIGH) //
					.setTitle("Zuhause - Türklingel") //
					.build());
		}
		return pushMessages;
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

	private void sendMessages(PushoverMessage message) {

		CompletableFuture.runAsync(() -> {
			try {
				Status result = pushClient.pushMessage(message); //

				if (result.getStatus() != 1) {
					LOG.error("Could not send push message (#1):" + result.toString());
				}

			} catch (PushoverException pe) {
				LOG.error("Could not send push message (#2):" + pe.getMessage());
			}
		});
	}

}
