package home.service;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import home.request.HomeRequestMapping;
import homecontroller.domain.model.ActionModel;
import homecontroller.domain.model.CameraMode;
import homecontroller.domain.model.CameraPicture;
import homecontroller.domain.model.Device;
import homecontroller.util.URIParameter;
import homelibrary.dao.ModelObjectDAO;

@Component
public class ControllerAPI { // FIXME: DELETE

	@Autowired
	private Environment env;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ByteArrayHttpMessageConverter byteArrayHttpMessageConverter;

	private static final String CONTROLLER_URL = "controller.url";

	public static final String DEVICE_NAME = "deviceName";

	public static final String CAMERA_MODE = "cameraMode";

	@PostConstruct
	public void postConstruct() {
		restTemplate.getMessageConverters().add(byteArrayHttpMessageConverter);
	}

	public void shuttersetposition(Device device, int positionPercentage) {
		callForObject(env.getProperty(CONTROLLER_URL) + "shutterSetPosition", ActionModel.class,
				new URIParameter().add(DEVICE_NAME, device.name())
						.add("positionPercentage", String.valueOf(positionPercentage)).build());
	}

	public byte[] cameraPicture(Device device, CameraMode mode, long timestamp) {
		CameraPicture cameraPicture = ModelObjectDAO.getInstance().readCameraPicture(device, mode, timestamp);
		if (cameraPicture != null) {
			return cameraPicture.getBytes();
		}
		return new byte[0];
	}

	public String cameraPictureRequest(Device device) {
		ActionModel actionModel = callForObject(env.getProperty(CONTROLLER_URL) + "cameraLivePicture",
				ActionModel.class, new URIParameter().add(DEVICE_NAME, device.name()).build());
		return actionModel != null ? actionModel.getStatus() : "";
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
