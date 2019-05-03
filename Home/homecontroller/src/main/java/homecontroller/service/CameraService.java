package homecontroller.service;

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
import org.springframework.util.Base64Utils;
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

	private static final String REPLACEMENT_IMAGE = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEACgoKCgrKC0yMi0/RDxEP11VTk5VXYxkbGRsZIzVhZuFhZuF1bzkua255Lz//+vr//////////////////////8BKCgoKCsoLTIyLT9EPEQ/XVVOTlVdjGRsZGxkjNWFm4WFm4XVvOS5rbnkvP//6+v////////////////////////CABEIADIAMgMBIgACEQEDEQH/xAAYAAEBAQEBAAAAAAAAAAAAAAAAAwIEBf/aAAgBAQAAAAD07CF0LoXEFwguI2QuhcQ1rYcH/8QAFgEBAQEAAAAAAAAAAAAAAAAAAQAC/9oACAECEAAAABGzqEpsf//EABgBAQADAQAAAAAAAAAAAAAAAAIAAQME/9oACAEDEAAAANAxW+NMuoJ2/wD/xAApEAACAQIEBQMFAAAAAAAAAAABAgMAEQQTIEEQEiExMhRRUiMzYXKC/9oACAEBAAE/APqYc/KOlZWFwdDM2JflXwFZEPwXhIjwOHTx3FI6utxRIAuad5JyUQWX3pECKANDo0Dc6ePtRdsSwC9EHelVVFgNMrtM+UnbejGcMQ6dV3pHV1uNDyNK+XH23NRxrGthwdXw7F06puKVlcXHBmaR8tP6NIgRbDQ8bQvmR3/Ir1aVhPtn9jqAFh0Ff//EABoRAAICAwAAAAAAAAAAAAAAAAExABARIDD/2gAIAQIBAT8AVPjkQLT/xAAkEQABAwMBCQAAAAAAAAAAAAABAAIhEBEyMQMSEyBBUVJhcv/aAAgBAwEBPwCH+nUw+lvO7mg0F8+iN7zTCTqibqHwclw3+JW0zdyf/9k=";

	@PostConstruct
	private void a() {
		restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());
	}

	public void takeEventPicture(Device device) {
		CameraPicture cameraPicture = new CameraPicture();
		cameraPicture.setTimestamp(new Date().getTime());
		cameraPicture.setBytes(takePictureFromCamera(device));
		ModelObjectDAO.getInstance().write(device, CameraMode.EVENT, cameraPicture);
	}

	public byte[] readCameraPicture(Device device, CameraMode cameraMode) {

		long eventTimestamp = lookupEventTimestamp(device);

		CameraPicture cameraPicture = ModelObjectDAO.getInstance().readCameraPicture(device, cameraMode,
				eventTimestamp);
		if (cameraPicture == null) {
			// FIXME: wait(15000);
			// FIXME: dao.readCameraPicture...
		}

		// FIXME: if replacement dann sofort neu lesen...

		if (cameraPicture != null) {
			return cameraPicture.getBytes();
		} else if (cameraMode == CameraMode.LIVE) {
			CameraPicture newCameraPicture = new CameraPicture();
			newCameraPicture.setTimestamp(new Date().getTime());
			newCameraPicture.setBytes(takePictureFromCamera(device));
			ModelObjectDAO.getInstance().write(device, CameraMode.LIVE, cameraPicture);
			return newCameraPicture.getBytes();
		}

		return new byte[0]; // FIXME
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

	private synchronized byte[] takePictureFromCamera(Device device) {

		try {
			return dao(device);
		} catch (Exception e) {
			return Base64Utils.decodeFromString(REPLACEMENT_IMAGE);
		} finally {
			// FIXME: monitor.notifyAll();
		}
	}

	private byte[] dao(Device device) {

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));

		HttpEntity<String> entity = new HttpEntity<String>(headers);

		ResponseEntity<byte[]> response = restTemplate.exchange(
				"http://homematic-ccu2/ise/img/homematic_logo_small.png", HttpMethod.GET, entity,
				byte[].class, "1");

		if (response.getStatusCode() == HttpStatus.OK) {
			// Files.write(Paths.get("/Users/mfi/Downloads/hmlogo.png"),
			// response.getBody());
		}
		return null;
	}

}
