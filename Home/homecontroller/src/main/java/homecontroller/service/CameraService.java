package homecontroller.service;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import homecontroller.domain.model.CameraMode;
import homecontroller.domain.model.CameraModel;
import homecontroller.domain.model.CameraPicture;
import homecontroller.domain.model.Device;
import homecontroller.domain.model.FrontDoor;
import homecontroller.domain.service.UploadService;
import homelibrary.dao.ModelObjectDAO;

@Component
public class CameraService {

	@Autowired
	@Qualifier("restTemplateLowTimeout")
	private RestTemplate restTemplateLowTimeout;

	@Autowired
	@Qualifier("restTemplateBinaryResponse")
	private RestTemplate restTemplateBinaryResponse;

	@Autowired
	private HomematicAPI homematicAPI;

	@Autowired
	private UploadService uploadService;

	private static final Log LOG = LogFactory.getLog(PushService.class);

	private static final Object MONITOR = new Object();

	public void takeEventPicture(FrontDoor frontdoor) {
		CameraPicture cameraPicture = new CameraPicture();
		cameraPicture.setTimestamp(frontdoor.getTimestampLastDoorbell());
		cameraPicture.setDevice(frontdoor.getDeviceCamera());
		cameraPicture.setCameraMode(CameraMode.EVENT);
		takePicture(cameraPicture);
	}

	public String takeLivePicture(Device device) {
		CameraPicture cameraPicture = new CameraPicture();
		cameraPicture.setTimestamp(new Date().getTime());
		cameraPicture.setDevice(device);
		cameraPicture.setCameraMode(CameraMode.LIVE);
		takePicture(cameraPicture);
		return String.valueOf(cameraPicture.getTimestamp());
	}

	private void takePicture(CameraPicture cameraPicture) {

		CompletableFuture.runAsync(() -> {
			synchronized (MONITOR) {
				long l1 = System.currentTimeMillis();
				try {
					turnOnCamera(cameraPicture.getDevice());
					byte[] picture = cameraReadPicture(Device.HAUSTUER_KAMERA);
					writePicture(cameraPicture, picture);
				} catch (Exception e) {
					LOG.error("Exception taking picture:", e);
				}
				if (LOG.isInfoEnabled()) {
					LOG.info("TIME = " + (System.currentTimeMillis() - l1) + " ms");
				}
			}
		});
	}

	private void writePicture(CameraPicture cameraPicture, byte[] picture) {

		if (picture.length == 0) {
			LOG.error("empty camera image!");
			return;
		}

		cameraPicture.setBytes(picture);
		CameraModel cameraModel = ModelObjectDAO.getInstance().readCameraModel();
		if (cameraPicture.getCameraMode() == CameraMode.LIVE) {
			cameraModel.setLivePicture(cameraPicture);
		} else if (cameraPicture.getCameraMode() == CameraMode.EVENT) {
			LOG.info("writing event picture: " + cameraPicture.getTimestamp());
			cameraModel.getEventPictures().add(cameraPicture); // FIXME:
																// DELETE
																// OLDEST
		}
		uploadService.upload(cameraModel);
	}

	private void turnOnCamera(Device deviceSwitch) {

		if (pingCamera(false)) {
			LOG.info("Camera already on");
			return;
		}

		LOG.info("RUN PROGRAM: " + deviceSwitch.programNamePrefix() + "DirektEinschalten");
		homematicAPI.runProgram(deviceSwitch.programNamePrefix() + "DirektEinschalten");
		boolean pingCameraOk = false;
		long startPolling = System.currentTimeMillis();
		do {
			if (System.currentTimeMillis() - startPolling > (1000L * 20L)) {
				throw new IllegalStateException("camera not started");
			}
			pingCameraOk = pingCamera(true);
		} while (!pingCameraOk);
	}

	private boolean pingCamera(boolean sleepIfNotReachable) {

		boolean pingCameraOk = false;

		try {
			LOG.info("PING");
			ResponseEntity<String> response = restTemplateLowTimeout.getForEntity("http://192.168.2.203/ping",
					String.class); // TODO:
									// externalize
									// configuration
			if (response.getStatusCode() == HttpStatus.OK) {
				pingCameraOk = true;
			} else {
				sleep(1000, sleepIfNotReachable); // RC=404 etc
			}
		} catch (Exception e) {
			if (isExceptionExpectedTimeout(e)) {
				LOG.info("PING - Timeout");
				sleep(1000, sleepIfNotReachable);
			} else if (isExceptionExpectedHostDown(e)) {
				LOG.info("PING - Host is down");
				sleep(1000, sleepIfNotReachable);
			} else {
				LOG.error("Error ping camera: ", e);
				sleep(1000, sleepIfNotReachable);
			}
		}
		return pingCameraOk;
	}

	private void sleep(long ms, boolean doSleep) {
		try {
			if (doSleep) {
				Thread.sleep(ms);
			}
		} catch (InterruptedException e) { // NOSONAR
			// noop
		}
	}

	private boolean isExceptionExpectedTimeout(Exception e) {
		return e.getClass().isAssignableFrom(ResourceAccessException.class) && e.getCause() != null
				&& e.getCause().getClass().isAssignableFrom(ConnectTimeoutException.class)
				&& e.getCause().getCause() != null
				&& e.getCause().getCause().getClass().isAssignableFrom(SocketTimeoutException.class);
	}

	private boolean isExceptionExpectedHostDown(Exception e) {
		return e.getClass().isAssignableFrom(ResourceAccessException.class) && e.getCause() != null
				&& e.getCause().getClass().isAssignableFrom(HttpHostConnectException.class)
				&& e.getCause().getCause() != null
				&& e.getCause().getCause().getClass().isAssignableFrom(ConnectException.class);
	}

	private byte[] cameraReadPicture(Device device) {

		try {
			ResponseEntity<byte[]> response = restTemplateBinaryResponse
					.getForEntity("http://192.168.2.203/capture", byte[].class); // FIXME:
																					// externalize
																					// url

			if (response.getStatusCode() == HttpStatus.OK) {
				return response.getBody();
			} else {
				throw new IllegalStateException(
						"could not capture picture from camera: " + response.getStatusCode());
			}
		} catch (RestClientException rce) {
			LOG.error("Error read camera picture: ", rce);
			return new byte[0];
		}
	}

}
