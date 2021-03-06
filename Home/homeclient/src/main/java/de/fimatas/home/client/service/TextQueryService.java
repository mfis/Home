package de.fimatas.home.client.service;

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

import de.fimatas.home.client.domain.model.PlacePrepositions;
import de.fimatas.home.client.domain.model.QueryValue;
import de.fimatas.home.client.domain.model.Synonym;
import de.fimatas.home.client.domain.model.SynonymeRepository;
import de.fimatas.home.client.domain.service.HouseViewService;
import de.fimatas.home.client.model.MessageQueue;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.AbstractDeviceModel;
import de.fimatas.home.library.domain.model.AutomationState;
import de.fimatas.home.library.domain.model.Climate;
import de.fimatas.home.library.domain.model.Heating;
import de.fimatas.home.library.domain.model.HouseModel;
import de.fimatas.home.library.domain.model.OutdoorClimate;
import de.fimatas.home.library.domain.model.Place;
import de.fimatas.home.library.domain.model.ShutterPosition;
import de.fimatas.home.library.domain.model.Switch;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.homematic.model.Type;
import de.fimatas.home.library.model.Message;
import de.fimatas.home.library.model.MessageType;

@Component
public class TextQueryService {

    private static final String DER_NEUE_WERT_KONNTE_LEIDER_NICHT_BESTAETIGT_WERDEN =
        "Der neue Wert konnte leider nicht bestätigt werden";

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
        value = lookupValue(words);

        places = Synonym.lookupSynonyms(Place.class, words);
        if (places.isEmpty()) {
            return "Entschuldige, ich habe nicht verstanden, welchen Raum oder Ort Du meinst.";
        }

        types = Synonym.lookupSynonyms(Type.class, words);
        if (types.isEmpty()) {
            return "Entschuldige, ich habe nicht verstanden, welches Gerät Du meinst.";
        }

        devices = lookupDevices(places, types, controlQuery);
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

        if (controlQuery && modelObject.getDevice().isControllable() && !value.matchesDevice(modelObject.getDevice())) {
            return "Entschuldige, ich habe nicht verstanden, auf welchen Wert ich das Gerät einstellen soll.";
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
            Message message = new Message();
            message.setDevice(heating.getDevice());
            if (value.getBooleanValue() != null) {
                message.setMessageType(MessageType.HEATINGBOOST);
            } else if (value.getIntegerValue() != null) {
                message.setMessageType(MessageType.HEATINGMANUAL);
                message.setValue(value.getIntegerValue().toString());
            }
            Message response = MessageQueue.getInstance().request(message, true);
            boolean success = response != null && response.isSuccessfullExecuted();

            if (!success) {
                builder.add(DER_NEUE_WERT_KONNTE_LEIDER_NICHT_BESTAETIGT_WERDEN);
                return builder.getText();
            }

            builder.add("Erledigt");
            builder.newSentence();
            heating = refreshModel(heating);
        }

        if (heating == null) {
            builder.add(DER_NEUE_WERT_KONNTE_LEIDER_NICHT_BESTAETIGT_WERDEN);
            return builder.getText();
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
            Message message = new Message();
            message.setDevice(powerswitch.getDevice());
            if (value.getBooleanValue() != null) {
                message.setMessageType(MessageType.TOGGLESTATE);
                message.setValue(value.getBooleanValue().toString());
            } else if (value.getAutomationState() != null) {
                message.setMessageType(MessageType.TOGGLEAUTOMATION);
                message.setValue(String.valueOf(value.getAutomationState().name()));
            }
            Message response = MessageQueue.getInstance().request(message, true);
            boolean success = response != null && response.isSuccessfullExecuted();
            powerswitch = refreshModel(powerswitch);

            if (!success) {
                builder.add(DER_NEUE_WERT_KONNTE_LEIDER_NICHT_BESTAETIGT_WERDEN);
                return builder.getText();
            }

            builder.add("Erledigt");
            builder.newSentence();
        }

        builder //
            .add(PlacePrepositions.getPreposition(powerswitch.getDevice().getPlace())) // NOSONAR
            .add(powerswitch.getDevice().getPlace().getPlaceName()) //
            .add("ist der") //
            .add(powerswitch.getDevice().getType().getTypeName()) //
            .add(powerswitch.isState() ? "eingeschaltet" : "ausgeschaltet") //
            .newSentence();

        if (powerswitch.getAutomation() != null) {
            builder //
                .add("Die Bedienung ist zu Zeit auf") //
                .add(Boolean.TRUE.equals(powerswitch.getAutomation()) ? "Automatik" : "manuell") //
                .add("eingestellt");
        }

        return builder.getText();
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractDeviceModel> T refreshModel(T deviceModel) {

        HouseModel refreshedModel = ModelObjectDAO.getInstance().readHouseModel();
        TypeAndDevice typeAndDevice = new TypeAndDevice();
        typeAndDevice.type = deviceModel.getDevice().getType();
        typeAndDevice.device = deviceModel.getDevice();
        return (T) lookupModelObject(refreshedModel, typeAndDevice);
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
                    if (modelDevice == entry.device
                        && ((modelSubType == null && modelDevice.getType() == entry.type) || modelSubType == entry.type)) {
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

        Set<AutomationState> automationStateValueValues = Synonym.lookupSynonyms(AutomationState.class, words);
        if (automationStateValueValues.size() == 1) {
            queryValue.setAutomationState(automationStateValueValues.iterator().next());
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

    private List<TypeAndDevice> lookupDevices(Set<Place> places, Set<Type> types, boolean controlQuery) {

        List<TypeAndDevice> list = new LinkedList<>();

        for (Place place : places) {
            for (Type type : types) {
                lookupDevice(list, place, type, controlQuery);
            }
        }

        return list;
    }

    private void lookupDevice(List<TypeAndDevice> list, Place place, Type type, boolean controlQuery) {

        // search for device with place and type
        // then search with optional sub-types
        for (Device device : Device.values()) {
            for (Place devicePlaces : device.getPlace().allPlaces()) {
                for (Type deviceTypes : device.getType().allTypes()) {
                    if (isMatchingDevice(place, type, device, devicePlaces, deviceTypes)
                        && !isNotControllable(type, controlQuery)) {
                        TypeAndDevice typeAndDevice = new TypeAndDevice();
                        typeAndDevice.type = type;
                        typeAndDevice.device = device;
                        list.add(typeAndDevice);
                    }
                }
            }
        }
    }

    private boolean isNotControllable(Type type, boolean controlQuery) {
        return controlQuery && !type.isControllable();
    }

    private boolean isMatchingDevice(Place place, Type type, Device device, Place devicePlaces, Type deviceTypes) {
        return devicePlaces == place && deviceTypes == type && device.isTextQueryEnabled();
    }

    private class TypeAndDevice {

        private Type type;

        private Device device;

    }
}
