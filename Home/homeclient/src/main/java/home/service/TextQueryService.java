package home.service;

import java.lang.reflect.Method;
import java.text.DecimalFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import home.domain.model.PlacePrepositions;
import home.domain.model.Synonym;
import home.domain.model.TextSynonymes;
import homecontroller.domain.model.Device;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.Place;
import homecontroller.domain.model.RoomClimate;
import homecontroller.domain.model.Type;

@Component
public class TextQueryService {

	public String execute(HouseModel house, String input) {

		Place place = null;
		Device device = null;
		boolean controlQuery = false;

		String[] words = splitTextIntoWOrds(input);

		for (String word : words) {
			controlQuery = lookupControlQuery(word);
			if (controlQuery) {
				break;
			}
		}

		if (controlQuery) {
			return "Ich kann zur Zeit nur Werte auslesen. Die Stererung von Geräten ist noch nicht möglich.";
		}

		for (String word : words) {
			place = lookupPlace(word);
			if (place != null) {
				break;
			}
		}

		if (place == null) {
			return "Entschuldige, ich konnte für die Anfrage keinen Ort identifizieren.";
		}

		for (String word : words) {
			device = lookupDevice(place, word);
			if (device != null) {
				break;
			}
		}

		if (device == null) {
			return "Entschuldige, für den Ort " + place.getPlaceName()
					+ " konnte ich kein entsprechendes Gerät identifizieren.";
		}

		return invokeQuery(lookupModelObject(house, device));
	}

	private String invokeQuery(Object modelObject) {

		if (modelObject instanceof RoomClimate) {
			return invokeQueryRoomClimate((RoomClimate) modelObject);
		}

		return "Entschuldige, da hat etwas nicht funktioniert.";
	}

	private String invokeQueryRoomClimate(RoomClimate roomClimate) {

		SentenceBuilder builder = SentenceBuilder.newInstance() //
				.add(PlacePrepositions.getPreposition(roomClimate.getDeviceThermometer().getPlace())) //
				.add(roomClimate.getDeviceThermometer().getPlace().getPlaceName()) //
				.add("ist zur Zeit eine Temperatur von") //
				.add(new DecimalFormat("0.0").format(roomClimate.getTemperature().getValue())) //
				.add("Grad");

		if (roomClimate.getHumidity() != null) {
			builder //
					.add("bei einer Luftfeuchtigkeit von") //
					.add(new DecimalFormat("0").format(roomClimate.getHumidity().getValue())) //
					.add("Prozent"); //
		}

		return builder.getSentence();
	}

	private Object lookupModelObject(HouseModel house, Device device) {

		try {
			for (Method method : house.getClass().getMethods()) {
				if (isGetter(method)) {
					Object subModel = method.invoke(house);
					for (Method subModelMethod : subModel.getClass().getMethods()) {
						if (returnsDeviceObject(subModelMethod)
								&& (Device) subModelMethod.invoke(subModel) == device) {
							return subModel;
						}
					}
				}
			}
		} catch (Exception e) {
			LogFactory.getLog(TextQueryService.class).error("Could not read model.", e);
		}

		return null;
	}

	private boolean returnsDeviceObject(Method subModelMethod) {
		return subModelMethod.getReturnType().isAssignableFrom(Device.class);
	}

	private boolean isGetter(Method method) {
		return method.getName().startsWith("get");
	}

	private String[] splitTextIntoWOrds(String text) {

		text = StringUtils.remove(text, '.');
		text = StringUtils.remove(text, ',');
		text = StringUtils.remove(text, '!');
		text = StringUtils.remove(text, '?');
		text = StringUtils.remove(text, '\'');

		return StringUtils.splitByWholeSeparator(text, StringUtils.SPACE);
	}

	private boolean lookupControlQuery(String word) {
		for (String synonyme : TextSynonymes.getControlSynonymes()) {
			if (synonyme.equalsIgnoreCase(word)) {
				return true;
			}
		}
		return false;
	}

	private Place lookupPlace(String placeString) {

		for (Place place : Place.values()) {
			if (place.getPlaceName().equalsIgnoreCase(placeString)) {
				return place;
			}
		}
		for (Synonym<Place> entry : TextSynonymes.getPlaceSynonymes()) {
			if (entry.getSynonymWord().equalsIgnoreCase(placeString)) {
				return entry.getBase();
			}
		}
		return null;
	}

	private Device lookupDevice(Place place, String typeString) {

		for (Type type : Type.values()) {
			if (type.getTypeName().equalsIgnoreCase(typeString)) {
				Device device = lookupDevice(place, type);
				if (device != null) {
					return device;
				}
			}
		}
		for (Synonym<Type> entry : TextSynonymes.getTypeSynonymes()) {
			if (entry.getSynonymWord().equalsIgnoreCase(typeString)) {
				Device device = lookupDevice(place, entry.getBase());
				if (device != null) {
					return device;
				}
			}
		}
		return null;
	}

	private Device lookupDevice(Place place, Type type) {
		for (Device device : Device.values()) {
			if (device.getPlace() == place && device.getType() == type && device.isTextQueryEnabled()) {
				return device;
			}
		}
		return null;
	}
}
