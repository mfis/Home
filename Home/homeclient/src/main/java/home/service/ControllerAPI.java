package home.service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import homecontroller.domain.model.ActionModel;
import homecontroller.domain.model.AutomationState;
import homecontroller.domain.model.CameraMode;
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

	@Autowired
	ByteArrayHttpMessageConverter byteArrayHttpMessageConverter;

	private static final String CONTROLLER_URL = "controller.url";

	public static final String BOOLEAN_VALUE = "booleanValue";

	public static final String DEVICE_NAME = "deviceName";

	public static final String CAMERA_MODE = "cameraMode";

	@PostConstruct
	public void postConstruct() {
		restTemplate.getMessageConverters().add(byteArrayHttpMessageConverter);
	}

	public void togglestate(Device device, Boolean booleanValue) {
		callForObject(env.getProperty(CONTROLLER_URL) + "togglestate", ActionModel.class, new URIParameter()
				.add(DEVICE_NAME, device.name()).add(BOOLEAN_VALUE, booleanValue.toString()).build());
	}

	public void toggleautomation(Device device, AutomationState automationStateValue) {
		callForObject(env.getProperty(CONTROLLER_URL) + "toggleautomation", ActionModel.class,
				new URIParameter().add(DEVICE_NAME, device.name())
						.add("automationStateValue", automationStateValue.name()).build());
	}

	public void heatingboost(Device device) {
		callForObject(env.getProperty(CONTROLLER_URL) + "heatingboost", ActionModel.class,
				new URIParameter().add(DEVICE_NAME, device.name()).build());
	}

	public void heatingmanual(Device device, BigDecimal temperature) {
		callForObject(env.getProperty(CONTROLLER_URL) + "heatingmanual", ActionModel.class,
				new URIParameter().add(DEVICE_NAME, device.name()).add("temperature",
						new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US)).format(temperature))
						.build());
	}

	public void shuttersetposition(Device device, int positionPercentage) {
		callForObject(env.getProperty(CONTROLLER_URL) + "shutterSetPosition", ActionModel.class,
				new URIParameter().add(DEVICE_NAME, device.name())
						.add("positionPercentage", String.valueOf(positionPercentage)).build());
	}

	public byte[] cameraPicture(Device device, CameraMode mode) {
		return callForBinary(env.getProperty(CONTROLLER_URL) + "cameraPicture",
				new URIParameter().add(DEVICE_NAME, device.name()).add(CAMERA_MODE, mode.name()).build());
	}

	public void settingspushtoggle(String userCookie) {
		callForObject(env.getProperty(CONTROLLER_URL) + "settingspushtoggle", ActionModel.class,
				new URIParameter().add("user", ExternalPropertiesDAO.getInstance().read(userCookie)).build());
	}

	public void settingspushover(String userCookie, String pushoverDevice) {
		callForObject(env.getProperty(CONTROLLER_URL) + "settingpushoverdevice", ActionModel.class,
				new URIParameter().add("user", ExternalPropertiesDAO.getInstance().read(userCookie))
						.add("device", pushoverDevice).build());
	}

	public HouseModel actualstate() {
		return callForObject(env.getProperty(CONTROLLER_URL) + "actualstate", HouseModel.class,
				new URIParameter().build());
	}

	public HistoryModel history() {
		return callForObject(env.getProperty(CONTROLLER_URL) + "history", HistoryModel.class,
				new URIParameter().build());
	}

	public SettingsModel settings(String userCookie) {
		return callForObject(env.getProperty(CONTROLLER_URL) + "settings", SettingsModel.class,
				new URIParameter().add("user", ExternalPropertiesDAO.getInstance().read(userCookie)).build());
	}

	private <T> T callForObject(String url, Class<T> clazz, MultiValueMap<String, String> parameters) {

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

	private byte[] callForBinary(String url, MultiValueMap<String, String> parameters) {

		try {
			HttpHeaders headers = createHeaders();
			headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);
			ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, request,
					byte[].class, "1");
			return response.getBody();
		} catch (Exception e) {
			LogFactory.getLog(HomeRequestMapping.class).error("Could not call controller!", e);
			return new byte[0];
		}
	}

	HttpHeaders createHeaders() {

		String plainClientCredentials = env.getProperty("controller.user") + ":"
				+ env.getProperty("controller.pass");
		String base64ClientCredentials = new String(Base64.encodeBase64(plainClientCredentials.getBytes()));

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Basic " + base64ClientCredentials);
		headers.set("Accept", "*/*");
		headers.set("Cache-Control", "no-cache");
		return headers;
	}
}
