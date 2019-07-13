package homecontroller.domain.model;

import java.io.Serializable;

public class SettingsModel implements Serializable {

	private static final long serialVersionUID = 1L;

	public SettingsModel() {
		super();
	}

	private String user;

	private boolean pushActive;

	private String pushoverApiToken;

	private String pushoverUserId;

	private String pushoverDevice;

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

	public String getPushoverDevice() {
		return pushoverDevice;
	}

	public void setPushoverDevice(String pushoverDevice) {
		this.pushoverDevice = pushoverDevice;
	}

	public boolean isPushActive() {
		return pushActive;
	}

	public void setPushActive(boolean pushActive) {
		this.pushActive = pushActive;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

}
