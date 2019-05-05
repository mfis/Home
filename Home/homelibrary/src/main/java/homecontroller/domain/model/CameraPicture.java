package homecontroller.domain.model;

import java.io.Serializable;

public class CameraPicture implements Serializable {

	private byte[] bytes;

	private long timestamp;

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
}