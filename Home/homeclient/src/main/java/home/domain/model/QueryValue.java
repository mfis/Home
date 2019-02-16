package home.domain.model;

import homecontroller.domain.model.ShutterPosition;

public class QueryValue {

	private ShutterPosition shutterPositionValue;
	
	private Boolean booleanValue;
	
	private Integer integerValue;

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
