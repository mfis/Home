package de.fimatas.home.library.model;

import java.io.Serializable;
import java.util.UUID;
import de.fimatas.home.library.homematic.model.Device;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uid;

    private String client;

    private MessageType messageType;

    private Device device;

    private String hueDeviceId;

    private String value;

    private String user;

    private String securityPin;

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

    public String getSecurityPin() {
        return securityPin;
    }

    public void setSecurityPin(String securityPin) {
        this.securityPin = securityPin;
    }

    public String getHueDeviceId() {
        return hueDeviceId;
    }

    public void setHueDeviceId(String hueDeviceId) {
        this.hueDeviceId = hueDeviceId;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

}
