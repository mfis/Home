package home.model;

import java.io.Serializable;
import java.util.UUID;

import homelibrary.homematic.model.Device;

public class Message implements Serializable {

	private static final long serialVersionUID = 1L;

	private String uid;

	private MessageType messageType;

	private Device device;

	private String value;

	private String user;

	boolean successfullExecuted;

	private String response;

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

	public boolean isSuccessfullExecuted() {
		return successfullExecuted;
	}

	public void setSuccessfullExecuted(boolean successfullExecuted) {
		this.successfullExecuted = successfullExecuted;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

}
