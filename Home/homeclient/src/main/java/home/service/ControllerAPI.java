package home.service;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import homecontroller.domain.model.ActionModel;
import homecontroller.domain.model.Device;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.SettingsModel;
import homecontroller.util.URIParameter;

@Component
public class ControllerAPI {

	@Autowired
	private Environment env;
	
	@Autowired
	private RestTemplate restTemplate;
	
	private static final String CONTROLLER_URL = "controller.url";
	
	public static final String BOOLEAN_VALUE = "booleanValue";

	public static final String DEVICE_NAME = "deviceName";
	
	public void togglestate(Device device, String booleanValue) {
		call(env.getProperty(CONTROLLER_URL) + "togglestate", ActionModel.class,
				new URIParameter().add("deviceName", device.name()).add("booleanValue", booleanValue).build());
	}
	
	public void toggleautomation(Device device, String booleanValue) {
		call(env.getProperty(CONTROLLER_URL) + "toggleautomation", ActionModel.class,
				new URIParameter().add("deviceName", device.name()).add("booleanValue", booleanValue).build());
	}
	
	public void heatingboost(Device device) {
		call(env.getProperty(CONTROLLER_URL) + "heatingboost", ActionModel.class,
				new URIParameter().add("deviceName", device.name()).build());
	}
	
	public void heatingmanual(Device device, String temperature) {
		call(env.getProperty(CONTROLLER_URL) + "heatingmanual", ActionModel.class,
				new URIParameter().add("deviceName", device.name()).add("temperature", temperature).build());
	}
	
	public void shuttersetposition(Device device, int positionPercentage) {
		call(env.getProperty(CONTROLLER_URL) + "shutterSetPosition", ActionModel.class,
				new URIParameter().add("deviceName", device.name()).add("positionPercentage", String.valueOf(positionPercentage)).build());
	}
	
	public void settingspushtoggle(String userCookie) {
		call(env.getProperty(CONTROLLER_URL) + "settingspushtoggle", ActionModel.class,
				new URIParameter().add("user", ExternalPropertiesDAO.getInstance().read(userCookie)).build());
	}
	
	public void settingspushover(String userCookie, String pushoverDevice) {
		call(env.getProperty(CONTROLLER_URL) + "settingpushoverdevice", ActionModel.class,
				new URIParameter().add("user", ExternalPropertiesDAO.getInstance().read(userCookie))
						.add("device", pushoverDevice).build());
	}
	
	public HouseModel actualstate() {
		HouseModel house = call(env.getProperty(CONTROLLER_URL) + "actualstate", HouseModel.class,
				new URIParameter().build());
		return house;
	}
	
	public HistoryModel history() {
		HistoryModel history = call(env.getProperty(CONTROLLER_URL) + "history", HistoryModel.class,
				new URIParameter().build());
		return history;
	}
	
	public SettingsModel settings(String userCookie) {
		SettingsModel settings = call(env.getProperty(CONTROLLER_URL) + "settings", SettingsModel.class,
				new URIParameter().add("user", ExternalPropertiesDAO.getInstance().read(userCookie)).build());
		return settings;
	}
	
	private <T> T call(String url, Class<T> clazz, MultiValueMap<String, String> parameters) {

		try {
			HttpHeaders headers = createHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);
			ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
			return new ObjectMapper().readValue(response.getBody(), clazz);
		} catch (Exception e) {
			LogFactory.getLog(HomeRequestMapping.class).error("Could not call controller!", e);
			return null;
		}
	}

	HttpHeaders createHeaders() {

		String plainClientCredentials = env.getProperty("controller.user") + ":" + env.getProperty("controller.pass");
		String base64ClientCredentials = new String(Base64.encodeBase64(plainClientCredentials.getBytes()));

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Basic " + base64ClientCredentials);
		headers.set("Accept", "*/*");
		headers.set("Cache-Control", "no-cache");
		return headers;
	}
}
