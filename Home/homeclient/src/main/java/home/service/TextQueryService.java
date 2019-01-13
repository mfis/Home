package home.service;

import java.lang.reflect.Method;
import java.text.DecimalFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import home.domain.model.PlacePrepositions;
import home.domain.model.Synonym;
import home.domain.model.TextSynonymes;
import homecontroller.domain.model.AbstractDeviceModel;
import homecontroller.domain.model.Device;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.Place;
import homecontroller.domain.model.RoomClimate;
import homecontroller.domain.model.Type;

@Component
public class TextQueryService {

	public String execute(HouseModel house, String input) {

		String[] words = splitTextIntoWords(input);
		Place place = null;
		Type type = null;
		Device device = null;
		boolean controlQuery = false;

		controlQuery = lookupControlQuery(words);
		if (controlQuery) {
			return "Ich kann zur Zeit nur Werte auslesen. Die Stererung von Geräten ist noch nicht möglich.";
		}

		place = lookupPlace(words);
		if (place == null) {
			return "Entschuldige, ich habe nicht verstanden, welchen Raum oder Ort Du meinst.";
		}

		type = lookupType(words);
		if (type == null) {
			return "Entschuldige, ich habe nicht verstanden, welches Gerät Du meinst.";
		}

		device = lookupDevice(place, type);
		if (device == null) {
			return "Entschuldige, für den Ort " + place.getPlaceName() + " konnte ich das Gerät "
					+ type.getTypeName() + "nicht finden.";
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
				.add(PlacePrepositions.getPreposition(roomClimate.getDevice().getPlace())) //
				.add(roomClimate.getDevice().getPlace().getPlaceName()) //
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
					Object model = method.invoke(house);
					if (model instanceof AbstractDeviceModel) {
						Device modelDevice = ((AbstractDeviceModel) model).getDevice();
						Type subType = ((AbstractDeviceModel) model).getSubType();
						if (modelDevice == device && (subType == null
								|| modelDevice.getType().getSubTypes().contains(subType))) {
							return model;
						}
					}
				}
			}
		} catch (Exception e) {
			LogFactory.getLog(TextQueryService.class).error("Could not read model.", e);
		}

		return null;
	}

	private boolean isGetter(Method method) {
		return method.getName().startsWith("get");
	}

	private String[] splitTextIntoWords(String text) {

		text = StringUtils.remove(text, '.');
		text = StringUtils.remove(text, ',');
		text = StringUtils.remove(text, '!');
		text = StringUtils.remove(text, '?');
		text = StringUtils.remove(text, '\'');

		return StringUtils.splitByWholeSeparator(text, StringUtils.SPACE);
	}

	private boolean lookupControlQuery(String[] words) {
		for (String word : words) {
			for (String synonyme : TextSynonymes.getControlSynonymes()) {
				if (synonyme.equalsIgnoreCase(word)) {
					return true;
				}
			}
		}
		return false;
	}

	private Place lookupPlace(String[] placeStrings) {

		for (String placeString : placeStrings) {
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
		}
		return null;
	}

	private Type lookupType(String[] typeStrings) {

		for (String typeString : typeStrings) {
			for (Type type : Type.values()) {
				if (type.getTypeName().equalsIgnoreCase(typeString)) {
					return type;
				}
			}
			for (Synonym<Type> entry : TextSynonymes.getTypeSynonymes()) {
				if (entry.getSynonymWord().equalsIgnoreCase(typeString)) {
					return entry.getBase();
				}
			}
		}
		return null;
	}

	private Device lookupDevice(Place place, Type type) {

		// search for device with place and type
		for (Device device : Device.values()) {
			if (device.getPlace() == place && device.getType() == type && device.isTextQueryEnabled()) {
				return device;
			}
		}

		// then search with optional device sub-types
		for (Device device : Device.values()) {
			for (Type subType : device.getType().getSubTypes())
				if (device.getPlace() == place && subType == type && device.isTextQueryEnabled()) {
					return device;
				}
		}

		return null;
	}
}
