package homecontroller.domain.model;

public enum AutomationState {

	AUTOMATIC(true), //
	MANUAL(false), //
	;
	
	private boolean booleanValue;
	
	public boolean isBooleanValue() {
		return booleanValue;
	}

	private AutomationState(Boolean booleanValue) {
		this.booleanValue = booleanValue;
	}
}
