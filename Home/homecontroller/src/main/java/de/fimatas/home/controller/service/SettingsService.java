package de.fimatas.home.controller.service;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import de.fimatas.home.controller.dao.ExternalPropertiesDAO;
import de.fimatas.home.controller.domain.service.HistoryService;
import de.fimatas.home.controller.domain.service.UploadService;
import de.fimatas.home.library.domain.model.SettingsModel;

@Component
public class SettingsService {

	@Autowired
	private Environment env;

	@Autowired
	private UploadService uploadService;

	private static final String PUSH_DEVICE = ".push.device";
	private static final String PUSH_HINTS = ".push.hints";
	private static final String PUSH_HINTS_HYSTERESIS = ".push.hintshysteresis";
	private static final String PUSH_HINTS_DOORBELL = ".push.doorbell";

	private static final String PUSH_USERID = "pushover.userid";
	private static final String PUSH_TOKEN = "pushover.token";

	@PostConstruct
	public void init() {

		try {
			refreshSettingsModelsComplete();
		} catch (Exception e) {
			LogFactory.getLog(HistoryService.class).error("Could not initialize SettingsService completly.",
					e);
		}
	}

	public void refreshSettingsModelsComplete() {

		List<String> names = ExternalPropertiesDAO.getInstance().lookupNamesContainingString(PUSH_DEVICE);
		for (String name : names) {
			String user = StringUtils.substringBefore(name, PUSH_DEVICE);
			SettingsModel settingsModel = read(user);
			uploadService.upload(settingsModel);
		}
	}

	public void updateSettingsModel(SettingsModel settingsModel) {

		ExternalPropertiesDAO.getInstance().write(settingsModel.getUser() + PUSH_DEVICE,
				StringUtils.trimToEmpty(settingsModel.getClientName()));
		ExternalPropertiesDAO.getInstance().write(settingsModel.getUser() + PUSH_HINTS,
				Boolean.toString(settingsModel.isPushHints()));
		ExternalPropertiesDAO.getInstance().write(settingsModel.getUser() + PUSH_HINTS_HYSTERESIS,
				Boolean.toString(settingsModel.isHintsHysteresis()));
		ExternalPropertiesDAO.getInstance().write(settingsModel.getUser() + PUSH_HINTS_DOORBELL,
				Boolean.toString(settingsModel.isPushDoorbell()));
		uploadService.upload(settingsModel);
	}

	public SettingsModel read(String user) {

		SettingsModel model = new SettingsModel();

		String token = StringUtils.trimToEmpty(env.getProperty(PUSH_TOKEN));
		model.setPushoverApiToken(token);

		String userid = StringUtils.trimToEmpty(env.getProperty(PUSH_USERID));
		model.setPushoverUserId(userid);

		model.setUser(user);

		String device = StringUtils.trimToEmpty(ExternalPropertiesDAO.getInstance().read(user + PUSH_DEVICE));
		model.setClientName(device);

		String hints = StringUtils.trimToEmpty(ExternalPropertiesDAO.getInstance().read(user + PUSH_HINTS));
		model.setPushHints(Boolean.parseBoolean(hints));

		String hysteresis = StringUtils
				.trimToEmpty(ExternalPropertiesDAO.getInstance().read(user + PUSH_HINTS_HYSTERESIS));
		model.setHintsHysteresis(Boolean.parseBoolean(hysteresis));

		String doorbell = StringUtils
				.trimToEmpty(ExternalPropertiesDAO.getInstance().read(user + PUSH_HINTS_DOORBELL));
		model.setPushDoorbell(Boolean.parseBoolean(doorbell));

		return model;
	}

	public List<SettingsModel> lookupUserForPushMessage() {

		List<SettingsModel> userModelsWithActivePush = new LinkedList<>();

		List<String> names = ExternalPropertiesDAO.getInstance().lookupNamesContainingString(PUSH_DEVICE);
		for (String name : names) {
			String user = StringUtils.substringBefore(name, PUSH_DEVICE);
			SettingsModel model = read(user);
			if (model.isPushHints() || model.isPushDoorbell()) {
				userModelsWithActivePush.add(model);
			}
		}

		return userModelsWithActivePush;
	}

}
