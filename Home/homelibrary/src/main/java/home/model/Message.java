package home.model;

import java.io.Serializable;
import java.util.UUID;

import homecontroller.domain.model.Device;

public class Message implements Serializable {

	private static final long serialVersionUID = 1L;

	private String uid;

	private MessageType messageType;

	private Device device;

	private String value;

	public Message() {
		uid = System.currentTimeMillis() + "#" + UUID.randomUUID().toString();
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
