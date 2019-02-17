package home.domain.model;

import java.lang.reflect.Field;

import org.apache.commons.logging.LogFactory;

import homecontroller.domain.model.AutomationState;
import homecontroller.domain.model.Device;
import homecontroller.domain.model.ShutterPosition;

public class QueryValue {

	private ShutterPosition shutterPositionValue;

	private Boolean booleanValue;

	private Integer integerValue;

	private AutomationState automationState;

	public boolean matchesDevice(Device device) {
		
		if (device.getValueTypes() == null) {
			return false;
		}
		
		try {
			for (Field field : this.getClass().getFields()) {
				if (field.get(this) != null) {
					for (Class<?> valueType : device.getValueTypes()) {
						if (valueType.isAssignableFrom(field.getType())) {
							return true;
						}
					}
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			LogFactory.getLog(this.getClass()).error("Could not read fields.", e);
		}
		return false;
	}

	public AutomationState getAutomationState() {
		return automationState;
	}

	public void setAutomationState(AutomationState automationState) {
		this.automationState = automationState;
	}

	public ShutterPosition getShutterPositionValue() {
		return shutterPositionValue;
	}

	public void setShutterPositionValue(ShutterPosition shutterPositionValue) {
		this.shutterPositionValue = shutterPositionValue;
	}

	public Boolean getBooleanValue() {
		return booleanValue;
	}

	public void setBooleanValue(Boolean booleanValue) {
		this.booleanValue = booleanValue;
	}

	public Integer getIntegerValue() {
		return integerValue;
	}

	public void setIntegerValue(Integer integerValue) {
		this.integerValue = integerValue;
	}

}
