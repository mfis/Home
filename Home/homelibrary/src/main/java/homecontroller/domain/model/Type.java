package homecontroller.domain.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum Type {

	// regular types
	THERMOMETER("Thermometer"), //
	SUN_SENSOR("Sonnensensor"), //
	SHUTTER_LEFT("Rolllade links"), //
	SHUTTER_RIGHT("Rolllade rechts"), //
	SWITCH_WINDOWLIGHT("Schalter Fensterlicht"), //
	ELECTRIC_POWER("Stromverbrauch"), //
	// with sub-types
	THERMOSTAT("Thermostat", Type.THERMOMETER), //
	// pseudo-types
	CONCLUSION_OUTSIDE_TEMPERATURE("ConclusionOutsideTemperature", Type.THERMOMETER), //
	;

	private final String typeName;

	private final List<Type> subTypes = new ArrayList<>();

	private Type(String typeName, Type... subTypes) {
		this.typeName = typeName;
		if (subTypes != null) {
			this.subTypes.addAll(Arrays.asList(subTypes));
		}
	}

	public String getTypeName() {
		return typeName;
	}

	public List<Type> getSubTypes() {
		return subTypes;
	}
}
