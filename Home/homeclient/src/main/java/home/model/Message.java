package home.model;

import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {

	private static final long serialVersionUID = 1L;

	private String uid;

	public Message() {
		uid = System.currentTimeMillis() + "#" + UUID.randomUUID().toString();
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

}
