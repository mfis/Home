package de.fimatas.home.library.model;

import java.io.Serializable;
import java.util.UUID;
import de.fimatas.home.library.homematic.model.Device;
import lombok.Data;

@Data
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uid;

    private String client;

    private MessageType messageType;

    private Device device;

    private String hueDeviceId;

    private String key;

    private String value;

    private String token;

    private String user;

    private String securityPin;

    boolean successfullExecuted;

    private String response;

    public Message() {
        uid = System.currentTimeMillis() + "#" + UUID.randomUUID();
    }

}
