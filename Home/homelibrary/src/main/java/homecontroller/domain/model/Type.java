package homecontroller.domain.model;

public enum Type {

	THERMOSTAT("Thermostat"), //
	THERMOMETER("Thermometer"), //
	SUN_SENSOR("Sonnensensor"), //
	SHUTTER_LEFT("Rolllade links"), //
	SHUTTER_RIGHT("Rolllade rechts"), //
	SWITCH_WINDOWLIGHT("Schalter Fensterlicht"), //
	ELECTRIC_POWER("Stromverbrauch"), //
	CONCLUSION_OUTSIDE_TEMPERATURE("ConclusionOutsideTemperature"), //
	;

	private String typeName;

	private Type(String typeName) {
		this.typeName = typeName;
	}

	public String getTypeName() {
		return typeName;
	}
}
