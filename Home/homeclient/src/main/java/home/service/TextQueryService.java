package home.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import home.domain.model.PlacePrepositions;
import home.domain.model.QueryValue;
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
import homecontroller.domain.model.ShutterPosition;
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
		List<TypeAndDevice> devices = null;
		boolean controlQuery = false;
		QueryValue value = null;

		controlQuery = lookupControlQuery(words);
		if (controlQuery) {
			value = lookupValue(words);
		}

		places = Synonym.lookupSynonyms(Place.class, words);
		if (places.isEmpty()) {
			return "Entschuldige, ich habe nicht verstanden, welchen Raum oder Ort Du meinst.";
		}

		types = Synonym.lookupSynonyms(Type.class, words);
		if (types.isEmpty()) {
			return "Entschuldige, ich habe nicht verstanden, welches Gerät Du meinst.";
		}

		devices = lookupDevices(places, types);
		if (devices.isEmpty()) {
			return "Entschuldige, für den angegbenen Ort konnte ich kein passendes Gerät finden.";
		}

		return invokeQueries(lookupModelObjects(house, devices), controlQuery, value);
	}

	private String invokeQueries(List<AbstractDeviceModel> modelObjects, boolean controlQuery, QueryValue value) {

		StringBuilder sb = new StringBuilder(300);
		for (AbstractDeviceModel modelObject : modelObjects) {
			sb.append(invokeQuery(modelObject, controlQuery, value));
		}

		String response = sb.toString();
		if (StringUtils.isBlank(response)) {
			return "Entschuldige, ich habe leider keinen Zugriff darauf.";
		} else {
			return response;
		}
	}

	private String invokeQuery(AbstractDeviceModel modelObject, boolean controlQuery, QueryValue value) {

		if (controlQuery && !modelObject.getDevice().isControllable()) {
			return "Entschuldige, dieses Gerät ist nicht steuerbar.";
		}

		if (controlQuery && value != null && modelObject.getDevice().isControllable()) {
			if (!value.matchesDevice(modelObject.getDevice())) {
				return "Entschuldige, ich habe nicht verstanden, auf welchen Wert ich das Gerät einstellen soll.";
			}
		}

		if (modelObject instanceof Climate) {
			return invokeQueryClimate((Climate) modelObject);
		}
		if (modelObject instanceof Heating) {
			return invokeQueryHeating((Heating) modelObject, controlQuery, value);
		}
		if (modelObject instanceof Switch) {
			return invokeQuerySwitch((Switch) modelObject, controlQuery, value);
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

	private String invokeQueryHeating(Heating heating, boolean controlQuery, QueryValue value) {

		SentenceBuilder builder = SentenceBuilder.newInstance();

		if (controlQuery) {
			if (value.getBooleanValue() != null) {
				// TODO: BOOST
			} else if (value.getIntegerValue() != null) {
				// TODO: HEATING
			}
			builder.add("Erledigt");
			builder.newSentence();
		}

		builder //
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

	private String invokeQuerySwitch(Switch powerswitch, boolean controlQuery, QueryValue value) {

		SentenceBuilder builder = SentenceBuilder.newInstance();

		if (controlQuery) {
			if (value.getBooleanValue() != null) {
				// TODO: AUTOMATION STATE
			} else if (value.getAutomationState() != null) {
				// TODO: SWITCH
			}
			builder.add("Erledigt");
			builder.newSentence();
		}

		builder //
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

	private List<AbstractDeviceModel> lookupModelObjects(HouseModel house, List<TypeAndDevice> list) {

		List<AbstractDeviceModel> modelObjects = new LinkedList<>();

		for (TypeAndDevice entry : list) {
			AbstractDeviceModel object = lookupModelObject(house, entry);
			if (object != null) {
				modelObjects.add(object);
			}
		}

		return modelObjects;
	}

	private AbstractDeviceModel lookupModelObject(HouseModel house, TypeAndDevice entry) {

		for (Method method : house.getClass().getMethods()) {
			if (isGetter(method)) {
				Object model = invokeMethod(house, method);
				if (model instanceof AbstractDeviceModel) {
					Device modelDevice = ((AbstractDeviceModel) model).getDevice();
					Type modelSubType = ((AbstractDeviceModel) model).getSubType();
					if (modelDevice == entry.device && ((modelSubType == null && modelDevice.getType() == entry.type)
							|| modelSubType == entry.type)) {
						return (AbstractDeviceModel) model;
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

		List<String> words = Arrays.asList(StringUtils.splitByWholeSeparator(text, StringUtils.SPACE));
		words = new ArrayList<>(words);
		Synonym.checkForCompoundWords(words);
		return words;
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

	private QueryValue lookupValue(List<String> words) {

		QueryValue queryValue = new QueryValue();

		Set<ShutterPosition> shutterPositionValues = Synonym.lookupSynonyms(ShutterPosition.class, words);
		if (shutterPositionValues.size() == 1) {
			queryValue.setShutterPositionValue(shutterPositionValues.iterator().next());
		}

		Set<Boolean> booleanValues = Synonym.lookupSynonyms(Boolean.class, words);
		if (booleanValues.size() == 1) {
			queryValue.setBooleanValue(booleanValues.iterator().next());
		}

		for (String word : words) {
			if (StringUtils.isNumeric(word)) {
				queryValue.setIntegerValue(Integer.parseInt(word));
				break;
			}
		}

		if (queryValue.getIntegerValue() == null) {
			Set<Integer> integerValues = Synonym.lookupSynonyms(Integer.class, words);
			if (integerValues.size() == 1) {
				queryValue.setIntegerValue(integerValues.iterator().next());
			}
		}

		return queryValue;
	}

	private List<TypeAndDevice> lookupDevices(Set<Place> places, Set<Type> types) {

		List<TypeAndDevice> list = new LinkedList<>();

		for (Place place : places) {
			for (Type type : types) {
				lookupDevice(list, place, type);
			}
		}

		return list;
	}

	private void lookupDevice(List<TypeAndDevice> list, Place place, Type type) {

		// search for device with place and type
		// then search with optional sub-types
		for (Device device : Device.values()) {
			for (Place devicePlaces : device.getPlace().allPlaces()) {
				for (Type deviceTypes : device.getType().allTypes()) {
					if (devicePlaces == place && deviceTypes == type && device.isTextQueryEnabled()) {
						TypeAndDevice typeAndDevice = new TypeAndDevice();
						typeAndDevice.type = type;
						typeAndDevice.device = device;
						list.add(typeAndDevice);
					}
				}
			}
		}
	}

	private class TypeAndDevice {

		private Type type;

		private Device device;

	}
}
