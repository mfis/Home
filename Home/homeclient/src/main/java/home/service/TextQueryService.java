package home.service;

import java.lang.reflect.Method;
import java.text.DecimalFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import home.domain.model.PlacePrepositions;
import home.domain.model.Synonym;
import home.domain.model.TextSynonymes;
import home.domain.service.HouseViewService;
import homecontroller.domain.model.AbstractDeviceModel;
import homecontroller.domain.model.Climate;
import homecontroller.domain.model.Device;
import homecontroller.domain.model.Heating;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.OutdoorClimate;
import homecontroller.domain.model.Place;
import homecontroller.domain.model.Type;

@Component
public class TextQueryService {

	@Autowired
	private HouseViewService houseViewService;

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
					+ type.getTypeName() + " nicht finden.";
		}

		return invokeQuery(lookupModelObject(house, device, type));
	}

	private String invokeQuery(Object modelObject) {

		if (modelObject instanceof Climate) {
			return invokeQueryClimate((Climate) modelObject);
		}
		if (modelObject instanceof Heating) {
			return invokeQueryHeating((Heating) modelObject);
		}

		return "Entschuldige, ich habe leider keinen Zugriff darauf.";
	}

	private String invokeQueryClimate(Climate climate) {

		SentenceBuilder builder = SentenceBuilder.newInstance() //
				.add(PlacePrepositions.getPreposition(climate.getDevice().getPlace())) //
				.add(climate.getDevice().getPlace().getPlaceName()) //
				.add("ist zur Zeit eine Temperatur von") //
				.add(new DecimalFormat("0.0").format(climate.getTemperature().getValue())) //
				.add("Grad");

		if (climate.getHumidity() != null) {
			builder //
					.add("bei einer Luftfeuchtigkeit von") //
					.add(new DecimalFormat("0").format(climate.getHumidity().getValue())) //
					.add("Prozent"); //
		}

		if (climate instanceof OutdoorClimate) {
			OutdoorClimate outdoorClimate = (OutdoorClimate) climate;
			String sunHeating = houseViewService.lookupSunHeating(outdoorClimate.getMaxSideSunHeating());
			if (StringUtils.isNotBlank(sunHeating)) {
				builder.newSentence();
				builder.add("Auf der");
				builder.add(outdoorClimate.getMaxSideSunHeating().getDevice().getPlace().getPlaceName());
				builder.add("ist es");
				builder.add(sunHeating.toLowerCase());
			}
		}

		return builder.getText();
	}

	private String invokeQueryHeating(Heating heating) {

		SentenceBuilder builder = SentenceBuilder.newInstance() //
				.add(PlacePrepositions.getPreposition(heating.getDevice().getPlace())) //
				.add(heating.getDevice().getPlace().getPlaceName()) //
				.add("ist das Thermostat");

		if (heating.isBoostActive()) {
			builder //
					.add("noch für") //
					.add(Integer.toString(heating.getBoostMinutesLeft())) //
					.add("Minuten auf schnelles Aufheizen"); //
		} else {
			builder //
					.add("auf") //
					.add(new DecimalFormat("0.0").format(heating.getTargetTemperature())) //
					.add("Grad"); //
		}
		builder.add("eingestellt");

		return builder.getText();
	}

	private Object lookupModelObject(HouseModel house, Device device, Type type) {

		try {
			for (Method method : house.getClass().getMethods()) {
				if (isGetter(method)) {
					Object model = method.invoke(house);
					if (model instanceof AbstractDeviceModel) {
						Device modelDevice = ((AbstractDeviceModel) model).getDevice();
						Type modelSubType = ((AbstractDeviceModel) model).getSubType();
						if (modelDevice == device && (modelSubType == null && modelDevice.getType() == type)
								|| modelSubType == type) {
							return model;
						}
					}
				}
			}
		} catch (

		Exception e) {
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
		// then search with optional sub-types
		for (Device device : Device.values()) {
			for (Place devicePlaces : device.getPlace().allPlaces())
				for (Type deviceTypes : device.getType().allTypes()) {
					if (devicePlaces == place && deviceTypes == type && device.isTextQueryEnabled()) {
						return device;
					}
				}
		}

		return null;
	}
}
