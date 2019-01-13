package homecontroller.domain.model;

import java.io.Serializable;

public class Switch extends AbstractDeviceModel implements Serializable {

	private static final long serialVersionUID = 1L;

	public Switch() {
		super();
	}

	private boolean state;

	private Boolean automation;

	private String automationInfoText;

	public boolean isState() {
		return state;
	}

	public void setState(boolean state) {
		this.state = state;
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
