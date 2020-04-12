package de.fimatas.home.library.domain.model;

public enum State {

	AUTOMATIC(true), //
	MANUAL(false), //
	
	UNLOCK(true), //
	LOCK(false), //
	;
	
	private boolean booleanValue;
	
	public boolean isBooleanValue() {
		return booleanValue;
	}

	private State(Boolean booleanValue) {
		this.booleanValue = booleanValue;
	}
}
