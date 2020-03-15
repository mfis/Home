package de.fimatas.home.library.domain.model;

import java.io.Serializable;

public class SettingsModel implements Serializable {

	private static final long serialVersionUID = 1L;

	public SettingsModel() {
		super();
	}

	private String user;

	private boolean pushHints;

	private boolean hintsHysteresis;

	private boolean pushDoorbell;

	private String pushoverApiToken;

	private String pushoverUserId;

	private String clientName;

	public String getPushoverApiToken() {
		return pushoverApiToken;
	}

	public void setPushoverApiToken(String pushoverApiToken) {
		this.pushoverApiToken = pushoverApiToken;
	}

	public String getPushoverUserId() {
		return pushoverUserId;
	}

	public void setPushoverUserId(String pushoverUserId) {
		this.pushoverUserId = pushoverUserId;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public boolean isPushHints() {
		return pushHints;
	}

	public void setPushHints(boolean pushHints) {
		this.pushHints = pushHints;
	}

	public boolean isHintsHysteresis() {
		return hintsHysteresis;
	}

	public void setHintsHysteresis(boolean hintsHysteresis) {
		this.hintsHysteresis = hintsHysteresis;
	}

	public boolean isPushDoorbell() {
		return pushDoorbell;
	}

	public void setPushDoorbell(boolean pushDoorbell) {
		this.pushDoorbell = pushDoorbell;
	}

}
