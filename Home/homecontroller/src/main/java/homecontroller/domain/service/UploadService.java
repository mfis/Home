package homecontroller.domain.service;

import java.util.Arrays;
import java.util.Date;

import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import homecontroller.domain.model.CameraMode;
import homecontroller.domain.model.CameraModel;
import homecontroller.domain.model.CameraPicture;
import homecontroller.domain.model.Device;

@Component
public class UploadService {

	@Autowired
	private RestTemplate restTemplate;

	private static final String REPLACEMENT_IMAGE = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEACgoKCgrKC0yMi0/RDxEP11VTk5VXYxkbGRsZIzVhZuFhZuF1bzkua255Lz//+vr//////////////////////8BKCgoKCsoLTIyLT9EPEQ/XVVOTlVdjGRsZGxkjNWFm4WFm4XVvOS5rbnkvP//6+v////////////////////////CABEIADIAMgMBIgACEQEDEQH/xAAYAAEBAQEBAAAAAAAAAAAAAAAAAwIEBf/aAAgBAQAAAAD07CF0LoXEFwguI2QuhcQ1rYcH/8QAFgEBAQEAAAAAAAAAAAAAAAAAAQAC/9oACAECEAAAABGzqEpsf//EABgBAQADAQAAAAAAAAAAAAAAAAIAAQME/9oACAEDEAAAANAxW+NMuoJ2/wD/xAApEAACAQIEBQMFAAAAAAAAAAABAgMAEQQTIEEQEiExMhRRUiMzYXKC/9oACAEBAAE/APqYc/KOlZWFwdDM2JflXwFZEPwXhIjwOHTx3FI6utxRIAuad5JyUQWX3pECKANDo0Dc6ePtRdsSwC9EHelVVFgNMrtM+UnbejGcMQ6dV3pHV1uNDyNK+XH23NRxrGthwdXw7F06puKVlcXHBmaR8tP6NIgRbDQ8bQvmR3/Ir1aVhPtn9jqAFh0Ff//EABoRAAICAwAAAAAAAAAAAAAAAAExABARIDD/2gAIAQIBAT8AVPjkQLT/xAAkEQABAwMBCQAAAAAAAAAAAAABAAIhEBEyMQMSEyBBUVJhcv/aAAgBAwEBPwCH+nUw+lvO7mg0F8+iN7zTCTqibqHwclw3+JW0zdyf/9k=";

	// @Scheduled(fixedDelay = (1000 * 60))
	private void test() {

		CameraModel cameraModel = new CameraModel();
		CameraPicture cameraPicture = new CameraPicture();
		cameraPicture.setDevice(Device.HAUSTUER_KAMERA);
		cameraPicture.setCameraMode(CameraMode.EVENT);
		cameraPicture.setTimestamp(new Date().getTime());
		cameraPicture.setBytes(Base64Utils.decodeFromString(REPLACEMENT_IMAGE));
		cameraModel.setLivePicture(cameraPicture);

		upload(cameraModel);

	}

	public void upload(Object object) {
		uploadBinary("http://localhost:8099/upload" + object.getClass().getSimpleName(), object.getClass(),
				object);
	}

	private <T> T uploadBinary(String url, T t, Object instance) {

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.ALL));
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Cache-Control", "no-cache");

		@SuppressWarnings("unchecked")
		HttpEntity<T> request = new HttpEntity<>((T) instance, headers);
		ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
		int sc = response.getStatusCodeValue();

		try {
		} catch (RestClientException e) {
			LogFactory.getLog(UploadService.class).error("Could not upload data.", e);
		}
		LogFactory.getLog(UploadService.class).info("Uploaded data - " + sc);
		return t;
	}

}
