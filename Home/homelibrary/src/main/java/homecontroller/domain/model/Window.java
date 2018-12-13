package homecontroller.domain.model;

import java.io.Serializable;

public class Window implements Serializable {

	private static final long serialVersionUID = 1L;

	private ShutterPosition shutterPosition;

	private Integer shutterPositionPercentage;

	private Device shutterDevice;

	private Boolean shutterAutomation;

	private String shutterAutomationInfoText;

	public ShutterPosition getShutterPosition() {
		return shutterPosition;
	}

	public void setShutterPosition(ShutterPosition shutterPosition) {
		this.shutterPosition = shutterPosition;
	}

	public Integer getShutterPositionPercentage() {
		return shutterPositionPercentage;
	}

	public void setShutterPositionPercentage(Integer shutterPositionPercentage) {
		this.shutterPositionPercentage = shutterPositionPercentage;
	}

	public Device getShutterDevice() {
		return shutterDevice;
	}

	public void setShutterDevice(Device shutterDevice) {
		this.shutterDevice = shutterDevice;
	}

	public Boolean getShutterAutomation() {
		return shutterAutomation;
	}

	public void setShutterAutomation(Boolean shutterAutomation) {
		this.shutterAutomation = shutterAutomation;
	}

	public String getShutterAutomationInfoText() {
		return shutterAutomationInfoText;
	}

	public void setShutterAutomationInfoText(String shutterAutomationInfoText) {
		this.shutterAutomationInfoText = shutterAutomationInfoText;
	}

}
