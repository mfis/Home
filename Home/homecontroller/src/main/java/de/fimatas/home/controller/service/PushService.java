package de.fimatas.home.controller.service;

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

import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.domain.model.HouseModel;
import de.fimatas.home.library.domain.model.RoomClimate;
import de.fimatas.home.library.domain.model.SettingsModel;
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

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

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
		List<SettingsModel> settingsModels = settingsService.lookupUserForPushMessage();

		for (SettingsModel settingsModel : settingsModels) {
			hintMessagePerUser(oldModel, newModel, pushMessages, settingsModel);
		}
		return pushMessages;
	}

	private void hintMessagePerUser(HouseModel oldModel, HouseModel newModel,
			List<PushoverMessage> pushMessages, SettingsModel settingsModel) {

		List<String> oldHints = hintList(oldModel, settingsModel);
		List<String> newHints = hintList(newModel, settingsModel);

		StringBuilder messages = new StringBuilder(300);

		formatNewHints(oldHints, newHints, messages);
		formatCanceledHints(oldHints, newHints, messages);

		String msgString = messages.toString().trim();
		if (StringUtils.isNotBlank(msgString)) {
			pushMessages.add(PushoverMessage.builderWithApiToken(settingsModel.getPushoverApiToken()) //
					.setUserId(settingsModel.getPushoverUserId()) //
					.setDevice(settingsModel.getClientName()) //
					.setMessage(msgString) //
					.setPriority(MessagePriority.NORMAL) //
					.setTitle("Zuhause - Empfehlungen") //
					.build());
		}
	}

	private void formatCanceledHints(List<String> oldHints, List<String> newHints, StringBuilder messages) {

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
	}

	private void formatNewHints(List<String> oldHints, List<String> newHints, StringBuilder messages) {

		for (String newHint : newHints) {
			if (!oldHints.contains(newHint)) {
				messages.append("\n");
				messages.append("- " + newHint);
			}
		}
	}

	private List<PushoverMessage> doorbellMessage(HouseModel oldModel, HouseModel newModel) {

		List<PushoverMessage> pushMessages = new LinkedList<>();

		if (!HouseService.doorbellTimestampChanged(oldModel, newModel)) {
			return pushMessages;
		}

		List<SettingsModel> settingsModels = settingsService.lookupUserForPushMessage();
		for (SettingsModel settingsModel : settingsModels) {
			if (settingsModel.isPushDoorbell()) {
				String time = TIME_FORMATTER
						.format(Instant.ofEpochMilli(newModel.getFrontDoor().getTimestampLastDoorbell())
								.atZone(ZoneId.systemDefault()).toLocalDateTime());
				String message = "Türklingelbetätigung um " + time + " Uhr";
				if (StringUtils.equals(settingsModel.getClientName(), "ONLY_LOGGING")) {
					LOG.info("MESSAGE: " + message);
				} else {
					pushMessages.add(PushoverMessage.builderWithApiToken(settingsModel.getPushoverApiToken()) //
							.setUserId(settingsModel.getPushoverUserId()) //
							.setDevice(settingsModel.getClientName()) //
							.setMessage(message) //
							.setPriority(MessagePriority.HIGH) //
							.setTitle("Zuhause - Türklingel") //
							.build());
				}
			}
		}
		return pushMessages;
	}

	private List<String> hintList(HouseModel model, SettingsModel settingsModel) {

		List<String> hintStrings = new LinkedList<>();
		if (!settingsModel.isPushHints()) {
			return hintStrings;
		}

		HouseModel m = model != null ? model : new HouseModel();

		for (RoomClimate room : m.lookupFields(RoomClimate.class).values()) {
			for (String text : room.getHints().formatAsText(settingsModel.isHintsHysteresis(), true, room)) {
				hintStrings.add(text);
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
