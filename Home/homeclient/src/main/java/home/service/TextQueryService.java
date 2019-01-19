package home.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import home.domain.model.PlacePrepositions;
import home.domain.model.Synonym;
import home.domain.model.SynonymeRepository;
import home.domain.service.HouseViewService;
import homecontroller.domain.model.AbstractDeviceModel;
import homecontroller.domain.model.Climate;
import homecontroller.domain.model.Device;
import homecontroller.domain.model.Heating;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.OutdoorClimate;
import homecontroller.domain.model.Place;
import homecontroller.domain.model.Switch;
import homecontroller.domain.model.Type;

@Component
public class TextQueryService {

	@Autowired
	private HouseViewService houseViewService;

	public String execute(HouseModel house, String input) {

		List<String> words = splitTextIntoWords(input);
		Set<Place> places = null;
		Set<Type> types = null;
		Map<Type, Device> devices = null;
		boolean controlQuery = false;

		controlQuery = lookupControlQuery(words);
		if (controlQuery) {
			return "Ich kann zur Zeit nur Werte auslesen. Die Stererung von Geräten ist noch nicht möglich.";
		}

		places = Synonym.lookup(Place.class, words);
		if (places.isEmpty()) {
			return "Entschuldige, ich habe nicht verstanden, welchen Raum oder Ort Du meinst.";
		}

		types = Synonym.lookup(Type.class, words);
		if (types.isEmpty()) {
			return "Entschuldige, ich habe nicht verstanden, welches Gerät Du meinst.";
		}

		devices = lookupDevices(places, types);
		if (devices.isEmpty()) {
			return "Entschuldige, für den angegbenen Ort konnte ich kein passendes Gerät finden.";
		}

		return invokeQueries(lookupModelObjects(house, devices));
	}

	private String invokeQueries(List<Object> modelObjects) {

		StringBuilder sb = new StringBuilder(300);
		for (Object modelObject : modelObjects) {
			sb.append(invokeQuery(modelObject));
		}

		String response = sb.toString();
		if (StringUtils.isBlank(response)) {
			return "Entschuldige, ich habe leider keinen Zugriff darauf.";
		} else {
			return response;
		}
	}

	private String invokeQuery(Object modelObject) {

		if (modelObject instanceof Climate) {
			return invokeQueryClimate((Climate) modelObject);
		}
		if (modelObject instanceof Heating) {
			return invokeQueryHeating((Heating) modelObject);
		}
		if (modelObject instanceof Switch) {
			return invokeQuerySwitch((Switch) modelObject);
		}

		return StringUtils.EMPTY;
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

	private String invokeQuerySwitch(Switch powerswitch) {

		SentenceBuilder builder = SentenceBuilder.newInstance() //
				.add(PlacePrepositions.getPreposition(powerswitch.getDevice().getPlace())) //
				.add(powerswitch.getDevice().getPlace().getPlaceName()) //
				.add("ist der") //
				.add(powerswitch.getDevice().getType().getTypeName()) //
				.add(powerswitch.isState() ? "eingeschaltet" : "ausgeschaltet") //
				.newSentence();

		if (powerswitch.getAutomation() != null) {
			builder //
					.add("Die Bedienung ist zu Zeit auf") //
					.add(powerswitch.getAutomation() ? "Automatik" : "manuell") //
					.add("eingestellt");
		}

		return builder.getText();
	}

	private List<Object> lookupModelObjects(HouseModel house, Map<Type, Device> map) {

		List<Object> modelObjects = new LinkedList<>();

		for (Entry<Type, Device> entry : map.entrySet()) {
			Object object = lookupModelObject(house, entry);
			if (object != null) {
				modelObjects.add(object);
			}
		}

		return modelObjects;
	}

	private Object lookupModelObject(HouseModel house, Entry<Type, Device> entry) {

		for (Method method : house.getClass().getMethods()) {
			if (isGetter(method)) {
				Object model = invokeMethod(house, method);
				if (model instanceof AbstractDeviceModel) {
					Device modelDevice = ((AbstractDeviceModel) model).getDevice();
					Type modelSubType = ((AbstractDeviceModel) model).getSubType();
					if (modelDevice == entry.getValue()
							&& ((modelSubType == null && modelDevice.getType() == entry.getKey())
									|| modelSubType == entry.getKey())) {
						return model;
					}
				}
			}
		}
		return null;
	}

	private Object invokeMethod(HouseModel house, Method method) {
		try {
			return method.invoke(house);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			LogFactory.getLog(TextQueryService.class).error("Could not read model.", e);
			return null;
		}
	}

	private boolean isGetter(Method method) {
		return method.getName().startsWith("get");
	}

	private List<String> splitTextIntoWords(String text) {

		text = StringUtils.remove(text, '.');
		text = StringUtils.remove(text, ',');
		text = StringUtils.remove(text, '!');
		text = StringUtils.remove(text, '?');
		text = StringUtils.remove(text, '\'');
		text = text.toLowerCase();

		return Arrays.asList(StringUtils.splitByWholeSeparator(text, StringUtils.SPACE));
	}

	private boolean lookupControlQuery(List<String> words) {
		for (String word : words) {
			for (String synonyme : SynonymeRepository.getControlSynonymes()) {
				if (synonyme.equalsIgnoreCase(word)) {
					return true;
				}
			}
		}
		return false;
	}

	private Map<Type, Device> lookupDevices(Set<Place> places, Set<Type> types) {

		Map<Type, Device> map = new LinkedHashMap<>();

		for (Place place : places) {
			for (Type type : types) {
				lookupDevice(map, place, type);
			}
		}

		return map;
	}

	private void lookupDevice(Map<Type, Device> map, Place place, Type type) {

		// search for device with place and type
		// then search with optional sub-types
		for (Device device : Device.values()) {
			for (Place devicePlaces : device.getPlace().allPlaces()) {
				for (Type deviceTypes : device.getType().allTypes()) {
					if (devicePlaces == place && deviceTypes == type && device.isTextQueryEnabled()) {
						map.put(type, device);
					}
				}
			}
		}
	}
}
