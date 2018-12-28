package homecontroller.service;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import homecontroller.dao.ExternalPropertiesDAO;
import homecontroller.domain.model.SettingsModel;

@Component
public class SettingsService {

	@Autowired
	private Environment env;

	private static final String PUSH_DEVICE = ".push.device";
	private static final String PUSH_ACTIVE = ".push.active";

	private static final String PUSH_USERID = "pushover.userid";
	private static final String PUSH_TOKEN = "pushover.token";

	public SettingsModel read(String user) {

		SettingsModel model = new SettingsModel();

		String token = StringUtils.trimToEmpty(env.getProperty(PUSH_TOKEN));
		model.setPushoverApiToken(token);

		String userid = StringUtils.trimToEmpty(env.getProperty(PUSH_USERID));
		model.setPushoverUserId(userid);

		String state = StringUtils.trimToEmpty(ExternalPropertiesDAO.getInstance().read(user + PUSH_ACTIVE));
		model.setPushActive(Boolean.parseBoolean(state));

		String device = StringUtils.trimToEmpty(ExternalPropertiesDAO.getInstance().read(user + PUSH_DEVICE));
		model.setPushoverDevice(device);

		return model;
	}

	public void togglePush(String user) {

		boolean state = Boolean.parseBoolean(
				StringUtils.trimToEmpty(ExternalPropertiesDAO.getInstance().read(user + PUSH_ACTIVE)));
		ExternalPropertiesDAO.getInstance().write(user + PUSH_ACTIVE, Boolean.toString(!state));
	}

	public void setupPushDevice(String user, String device) {
		ExternalPropertiesDAO.getInstance().write(user + PUSH_DEVICE, StringUtils.trimToEmpty(device));
	}

	public List<SettingsModel> lookupUserForPushMessage() {

		List<SettingsModel> userModelsWithActivePush = new LinkedList<>();

		List<String> names = ExternalPropertiesDAO.getInstance().lookupNamesContainingString(PUSH_ACTIVE);
		for (String name : names) {
			String user = StringUtils.substringBefore(name, PUSH_ACTIVE);
			SettingsModel model = read(user);
			if (model.isPushActive()) {
				userModelsWithActivePush.add(model);
			}
		}

		return userModelsWithActivePush;
	}

}
