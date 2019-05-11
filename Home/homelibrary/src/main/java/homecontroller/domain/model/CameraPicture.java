package homecontroller.domain.model;

import java.io.Serializable;

public class CameraPicture implements Serializable {

	private static final long serialVersionUID = 1L;

	private byte[] bytes;

	private long timestamp;

	private Device device;

	private CameraMode cameraMode;

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public CameraMode getCameraMode() {
		return cameraMode;
	}

	public void setCameraMode(CameraMode cameraMode) {
		this.cameraMode = cameraMode;
	}
}