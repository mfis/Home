package homecontroller.domain.model;

import java.io.Serializable;

public class SwitchModel implements Serializable {

	private static final long serialVersionUID = 1L;

	public SwitchModel() {
		super();
	}

	private boolean state;

	private Device device;

	private Boolean automation;

	private String automationInfoText;

	public boolean isState() {
		return state;
	}

	public void setState(boolean state) {
		this.state = state;
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public Boolean getAutomation() {
		return automation;
	}

	public void setAutomation(Boolean automation) {
		this.automation = automation;
	}

	public String getAutomationInfoText() {
		return automationInfoText;
	}

	public void setAutomationInfoText(String automationInfoText) {
		this.automationInfoText = automationInfoText;
	}

}
