package homecontroller.service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import homecontroller.dao.ModelObjectDAO;
import homecontroller.domain.model.CameraMode;
import homecontroller.domain.model.CameraPicture;
import homecontroller.domain.model.Device;
import homecontroller.domain.model.HouseModel;

@Component
public class CameraService {

	@Autowired
	private RestTemplate restTemplate;

	@PostConstruct
	private void a() {
		restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());
	}

	public void takeEventPicture(Device device) { // TODO: call
		CameraPicture cameraPicture = new CameraPicture();
		cameraPicture.setTimestamp(new Date().getTime());
		cameraPicture.setBytes(readPictureFromCamera(device));
		ModelObjectDAO.getInstance().write(device, CameraMode.EVENT, cameraPicture);
	}

	public byte[] lookupCameraPicture(Device device, CameraMode cameraMode) {

		long eventTimestamp = lookupEventTimestamp(device);

		CameraPicture cameraPicture = ModelObjectDAO.getInstance().readCameraPicture(device, cameraMode,
				eventTimestamp);
		if (cameraPicture != null) {
			return cameraPicture.getBytes();
		} else if (cameraMode == CameraMode.LIVE) {
			CameraPicture newCameraPicture = new CameraPicture();
			newCameraPicture.setTimestamp(new Date().getTime());
			newCameraPicture.setBytes(readPictureFromCamera(device));
			ModelObjectDAO.getInstance().write(device, CameraMode.LIVE, cameraPicture);
			return newCameraPicture.getBytes();
		}

		return new byte[0];
	}

	private long lookupEventTimestamp(Device device) {
		switch (device) { // NOSONAR
		case HAUSTUER_KAMERA:
			HouseModel houseModel = ModelObjectDAO.getInstance().readHouseModel();
			return houseModel == null || houseModel.getFrontDoor() == null ? 0
					: houseModel.getFrontDoor().getTimestampLastDoorbell();
		default:
			throw new IllegalArgumentException("Unknown Device:" + device);
		}
	}

	private byte[] readPictureFromCamera(Device device) {

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));

		HttpEntity<String> entity = new HttpEntity<String>(headers);

		ResponseEntity<byte[]> response = restTemplate.exchange(
				"http://homematic-ccu2/ise/img/homematic_logo_small.png", HttpMethod.GET, entity,
				byte[].class, "1");

		if (response.getStatusCode() == HttpStatus.OK) {
			Files.write(Paths.get("/Users/mfi/Downloads/hmlogo.png"), response.getBody());
		}
	}

}
