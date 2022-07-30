package de.fimatas.home.client.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import de.fimatas.home.client.domain.model.*;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.model.*;
import de.fimatas.home.library.util.WeatherForecastConclusionTextFormatter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import de.fimatas.home.client.domain.service.ViewFormatter.TimestampFormat;
import de.fimatas.home.client.model.MessageQueue;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.homematic.model.Type;
import de.fimatas.home.library.util.HomeAppConstants;

@Component
public class HouseViewService {

    private static final String REGEXP_NOT_ALPHANUMERIC = "[^a-zA-Z0-9]";

    private static final String UNBEKANNT = "unbekannt";

    private static final String EREIGNISGESTEUERT = ", Ereignissteuerung";

    private static final String PROGRAMMGESTEUERT = ", Automatik";

    private static final String EINGESCHALTET = "eingeschaltet";

    private static final String AUSGESCHALTET = "ausgeschaltet";

    private static final String MANUELL = ", Manuell";

    private static final String AND_VALUE_IS = "&value=";

    private static final String AND_DEVICE_IS = "&deviceName=";

    private static final String AND_HUE_DEVICE_ID_IS = "&hueDeviceId=";

    private static final String NEEDS_PIN = "&needsPin";

    private static final String TYPE_IS = "type=";

    public static final String MESSAGEPATH = "/message?"; // NOSONAR

    private static final String TOGGLE_STATE = MESSAGEPATH + TYPE_IS + MessageType.TOGGLESTATE + AND_DEVICE_IS;

    private static final String TOGGLE_AUTOMATION = MESSAGEPATH + TYPE_IS + MessageType.TOGGLEAUTOMATION + AND_DEVICE_IS;

    private static final String OPEN_STATE = MESSAGEPATH + TYPE_IS + MessageType.OPEN + AND_DEVICE_IS;

    private static final String TOGGLE_LIGHT = MESSAGEPATH + TYPE_IS + MessageType.TOGGLELIGHT + AND_HUE_DEVICE_ID_IS;

    private static final BigDecimal HIGH_TEMP = new BigDecimal("25");

    private static final BigDecimal MEDIUM_HIGH_TEMP = new BigDecimal("23.0");

    private static final BigDecimal LOW_TEMP = new BigDecimal("18");

    private static final BigDecimal FROST_TEMP = new BigDecimal("3");

    public static final String PLACE_SUBTITLE_PREFIX = "place_subtitle_";

    @Autowired
    private ViewFormatter viewFormatter;

    @PostConstruct
    public void init() {
        CompletableFuture.runAsync(() -> {
            try {
                Message message = new Message();
                message.setMessageType(MessageType.REFRESH_ALL_MODELS);
                MessageQueue.getInstance().request(message, false);
            } catch (Exception e) {
                LogFactory.getLog(HouseViewService.class).error("Could not initialize HouseViewService completly.", e);
            }
        });
    }

    public void fillViewModel(Model model, HouseModel house, HistoryModel historyModel, LightsModel lightsModel, WeatherForecastModel weatherForecastModel, PresenceModel presenceModel) {

        model.addAttribute("modelTimestamp", ModelObjectDAO.getInstance().calculateModelTimestamp());

        formatClimate(model, "tempBathroom", house.getClimateBathRoom(), house.getHeatingBathRoom(), false);
        formatClimate(model, "tempKids1", house.getClimateKidsRoom1(), null, true);
        formatClimate(model, "tempKids2", house.getClimateKidsRoom2(), null, true);
        formatClimate(model, "tempLivingroom", house.getClimateLivingRoom(), null, false);
        formatClimate(model, "tempBedroom", house.getClimateBedRoom(), null, true);
        formatClimate(model, "tempLaundry", house.getClimateLaundry(), null, true);
        formatClimate(model, "tempGuestroom", house.getClimateGuestRoom(), house.getHeatingGuestRoom(), false);
        formatClimate(model, "tempWorkshop", house.getClimateWorkshop(), null, false);

        formatClimateGroup(model, "upperFloor", Place.UPPER_FLOOR_TEMPERATURE, house);

        // formatWindow(model, "leftWindowBedroom", // NOSONAR
        // house.getLeftWindowBedRoom()); // NOSONAR

        formatFacadeTemperatures(model, "tempMinHouse", "tempMaxHouse", house);

        formatWindowSensor(model, "windowSensorGuestroom", house.getGuestRoomWindowSensor());
        formatWindowSensor(model, "windowSensorWorkshop", house.getWorkshopWindowSensor());
        formatWindowSensor(model, "windowSensorLaundry", house.getLaundryWindowSensor());

        formatSwitch(model, "switchKitchen", house.getKitchenWindowLightSwitch());
        formatSwitch(model, "switchWallbox", house.getWallboxSwitch());
        formatSwitch(model, "switchWorkshopVentilation", house.getWorkshopVentilationSwitch());

        formatFrontDoorBell(model, "frontDoor", house.getFrontDoorBell(), house.getFrontDoorCamera());
        formatFrontDoorLock(model, "frontDoorLock", house.getFrontDoorLock());
        formatPower(model, house.getTotalElectricalPowerConsumption(), historyModel==null?null:historyModel.getTotalElectricPowerConsumptionDay());
        formatPower(model, house.getWallboxElectricalPowerConsumption(), historyModel==null?null:historyModel.getWallboxElectricPowerConsumptionDay());

        formatHeatpump(model);

        formatLowBattery(model, house.getLowBatteryDevices());

        formatWarnings(model, house, lightsModel, weatherForecastModel, historyModel);

        formatPlaceSubtitles(model, house);

        formatLights(lightsModel, model);

        formatWeatherForecast(model, weatherForecastModel);

        formatPresence(model, presenceModel);
    }

    private void formatClimateGroup(Model model, String viewKey, Place place, HouseModel house) {

        var subPlaces = house.lookupFields(RoomClimate.class).values().stream()
                .filter(c -> place.getSubPlaces().contains(c.getDevice().getPlace())).collect(Collectors.toList());
        var unreach = subPlaces.stream().anyMatch(AbstractDeviceModel::isUnreach);

        ClimateView view = new ClimateView();
        model.addAttribute(viewKey, view);

        view.setId(viewKey);
        view.setPlaceEnum(place);
        view.setUnreach(Boolean.toString(unreach));
        if (unreach || subPlaces.size()==0) {
            return;
        }

        Comparator<RoomClimate> comparator =
                Comparator.comparing(Climate::getTemperature, Comparator.comparing(ValueWithTendency::getValue));
        Optional<RoomClimate> minTemperature = subPlaces.stream().min(comparator);
        Optional<RoomClimate> maxTemperature = subPlaces.stream().max(comparator);

        var from = format(minTemperature.get().getTemperature().getValue(), false, false);
        var to = format(maxTemperature.get().getTemperature().getValue(), false, false);

        StringBuilder combinedTemperatures = new StringBuilder(20);
        combinedTemperatures.append(from);
        if(!from.equals(to)) {
            combinedTemperatures.append(" bis ");
            combinedTemperatures.append(to);
        }
        combinedTemperatures.append(ViewFormatter.DEGREE + "C");
        view.setStateTemperature(combinedTemperatures.toString());

        formatClimateBackground(maxTemperature.get(), view);
    }

    private void formatFrontDoorBell(Model model, String id, Doorbell doorbell, Camera camera) {

        FrontDoorView frontDoorView = new FrontDoorView();
        frontDoorView.setId(id);
        frontDoorView.setUnreach(Boolean.toString(doorbell.isUnreach()));
        if (doorbell.isUnreach()) {
            model.addAttribute(id, frontDoorView);
            return;
        }

        frontDoorView.setIcon("fas fa-bell");
        if (doorbell.getTimestampLastDoorbell() != null) {
            frontDoorView.setLastDoorbells(StringUtils.capitalize(
                    viewFormatter.formatTimestamp(doorbell.getTimestampLastDoorbell(), TimestampFormat.DATE_TIME)));
            frontDoorView.setElementTitleState(StringUtils
                    .capitalize(viewFormatter.formatTimestamp(doorbell.getTimestampLastDoorbell(), TimestampFormat.SHORT_WITH_TIME)));
        } else {
            frontDoorView.setLastDoorbells(UNBEKANNT);
        }

        long minutesSinceLastDoorbellRing = Duration
                .between(Instant.ofEpochMilli(doorbell.getTimestampLastDoorbell() != null ? doorbell.getTimestampLastDoorbell() : 0)
                        .atZone(ZoneId.systemDefault()).toLocalDateTime(), LocalDateTime.now())
                .toMinutes();
        if (minutesSinceLastDoorbellRing < 5) {
            frontDoorView.setColorClass(ConditionColor.RED.getUiClass());
        } else if (minutesSinceLastDoorbellRing < 60) {
            frontDoorView.setColorClass(ConditionColor.ORANGE.getUiClass());
        } else {
            frontDoorView.setColorClass(ConditionColor.GRAY.getUiClass());
        }


        formatCamera(doorbell, camera, frontDoorView);

        model.addAttribute(id, frontDoorView);
    }

    private void formatCamera(Doorbell doorbell, Camera camera, FrontDoorView frontDoorView) {

        if (camera == null || camera.getDevice() == null) {
            return;
        }

        frontDoorView.setIdLive("frontdoorcameralive");
        frontDoorView.setIdBell("frontdoorcamerabell");
        frontDoorView
                .setLinkLive("/cameraPicture?deviceName=" + camera.getDevice() + "&cameraMode=" + CameraMode.LIVE + "&ts=");
        frontDoorView.setLinkLiveRequest("/cameraPictureRequest?type=" + MessageType.CAMERAPICTUREREQUEST + AND_DEVICE_IS
                + camera.getDevice() + "&value=null");
        frontDoorView.setLinkBell("/cameraPicture?deviceName=" + camera.getDevice() + "&cameraMode=" + CameraMode.EVENT + "&ts="
                + doorbell.getTimestampLastDoorbell());
    }

    private void formatFrontDoorLock(Model model, String id, Doorlock doorlock) {

        LockView view = new LockView();
        view.setId(id);
        view.setName(doorlock.getDevice().getType().getTypeName());
        view.setCaption(doorlock.getDevice().getPlace().getPlaceName());
        view.setPlaceEnum(doorlock.getDevice().getPlace());
        view.setUnreach(Boolean.toString(doorlock.isUnreach()));

        view.setBusy(Boolean.toString(doorlock.isBusy()));
        boolean setButtonLock = false;
        boolean setButtonUnlock = false;
        boolean setButtonOpen = false;

        if (doorlock.isUnreach()) {
            setButtonOpen = true;
            setButtonLock = true;
            setButtonUnlock = true;
        } else if (doorlock.getErrorcode() != 0) {
            view.setState("Mechanischer Fehler!");
            view.setIcon("fas fa-bug");
            view.setColorClass(ConditionColor.RED.getUiClass());
            setButtonOpen = true;
            setButtonLock = true;
            setButtonUnlock = true;
        } else if (doorlock.isOpen()) {
            view.setState("Öffner aktiv");
            view.setIcon("fas fa-door-open");
            view.setColorClass(ConditionColor.RED.getUiClass());
            setButtonLock = true;
        } else {
            setButtonOpen = true;
            if (doorlock.isLockStateUncertain()) {
                view.setState("Unbestimmt");
                view.setIcon("fas fa-question-circle");
                view.setColorClass(ConditionColor.ORANGE.getUiClass());
                setButtonLock = true;
                setButtonUnlock = true;
            } else {
                if (doorlock.isLockState()) {
                    view.setState("Verriegelt");
                    view.setIcon("fas fa-lock");
                    view.setColorClass(ConditionColor.GREEN.getUiClass());
                    setButtonUnlock = true;
                } else {
                    view.setState("Entriegelt");
                    view.setIcon("fas fa-lock-open");
                    view.setColorClass(ConditionColor.ORANGE.getUiClass());
                    setButtonLock = true;
                }
            }
        }

        if (setButtonLock) {
            view.setLinkLock(OPEN_STATE + doorlock.getDevice().name() + AND_VALUE_IS + StateValue.LOCK.name() + NEEDS_PIN);
        }
        if (setButtonUnlock) {
            view.setLinkUnlock(OPEN_STATE + doorlock.getDevice().name() + AND_VALUE_IS + StateValue.UNLOCK.name() + NEEDS_PIN);
        }
        if (setButtonOpen) {
            view.setLinkOpen(OPEN_STATE + doorlock.getDevice().name() + AND_VALUE_IS + StateValue.OPEN.name() + NEEDS_PIN);
        }

        formatFrontDoorLockLinks(doorlock, view);
        view.setAutoInfoText(StringUtils.trimToEmpty(doorlock.getLockAutomationInfoText()));

        model.addAttribute(id, view);
    }

    public void formatFrontDoorLockLinks(Doorlock doorlock, LockView view) {

        view.setElementTitleState(MANUELL.replaceAll(REGEXP_NOT_ALPHANUMERIC, StringUtils.EMPTY));
        if (Boolean.TRUE.equals(doorlock.getLockAutomationEvent())) {
            view.setLinkManual(TOGGLE_AUTOMATION + doorlock.getDevice().name() + AND_VALUE_IS + AutomationState.MANUAL.name());
            view.setLinkAuto(TOGGLE_AUTOMATION + doorlock.getDevice().name() + AND_VALUE_IS + AutomationState.AUTOMATIC.name());
            if (doorlock.getErrorcode() == 0) {
                view.setStateSuffix(EREIGNISGESTEUERT);
                view.setElementTitleState(EREIGNISGESTEUERT.replaceAll(REGEXP_NOT_ALPHANUMERIC, StringUtils.EMPTY));
            }
        } else if (Boolean.TRUE.equals(doorlock.getLockAutomation())) {
            view.setLinkManual(TOGGLE_AUTOMATION + doorlock.getDevice().name() + AND_VALUE_IS + AutomationState.MANUAL.name());
            view.setLinkAutoEvent(
                    TOGGLE_AUTOMATION + doorlock.getDevice().name() + AND_VALUE_IS + AutomationState.AUTOMATIC_PLUS_EVENT.name());
            if (doorlock.getErrorcode() == 0) {
                view.setStateSuffix(PROGRAMMGESTEUERT);
                view.setElementTitleState(PROGRAMMGESTEUERT.replaceAll(REGEXP_NOT_ALPHANUMERIC, StringUtils.EMPTY));
            }
        } else {
            view.setLinkAuto(TOGGLE_AUTOMATION + doorlock.getDevice().name() + AND_VALUE_IS + AutomationState.AUTOMATIC.name());
            view.setLinkAutoEvent(
                    TOGGLE_AUTOMATION + doorlock.getDevice().name() + AND_VALUE_IS + AutomationState.AUTOMATIC_PLUS_EVENT.name());
        }
    }

    protected String format(BigDecimal val, boolean rounded, boolean onlyInteger) {

        if (val != null) {
            if (onlyInteger) {
                var formatted = new DecimalFormat(rounded ? "#" : "0").format(val);
                if(formatted.equals("-0")){
                    formatted = "0";
                }
                return formatted;
            } else {
                return new DecimalFormat("0." + (rounded ? "#" : "0")).format(val);
            }
        } else {
            return null;
        }
    }

    private void formatClimate(Model model, String viewKey, Climate climate, Heating heating, boolean history) {
        ClimateView view = formatClimate(climate, heating, viewKey, history);
        model.addAttribute(viewKey, view);
    }

    private ClimateView formatClimate(Climate climate, Heating heating, String viewKey, boolean history) {

        ClimateView view = new ClimateView();
        view.setId(viewKey);
        view.setPlaceEnum(climate.getDevice().getPlace());
        view.setUnreach(Boolean.toString(climate.isUnreach() || (heating != null && heating.isUnreach())));
        if (climate.isUnreach() || (heating != null && heating.isUnreach())) {
            return view;
        }

        if (climateStateUnknown(climate)) {
            view.setStateTemperature(UNBEKANNT);
            view.setStateShort("?");
            return view;
        }

        if (history) {
            view.setHistoryKey(climate.getDevice().historyKeyPrefix());
        }

        if (climate.getTemperature() != null) {
            // Temperature and humidity
            view.setStateTemperature(format(climate.getTemperature().getValue(), false, false) + ViewFormatter.DEGREE + "C");
            view.setStateShort(format(climate.getTemperature().getValue(), true, true) + ViewFormatter.DEGREE);
            view.setElementTitleState(view.getStateTemperature());
            if (climate.getHumidity() != null) {
                view.setStateHumidity(format(climate.getHumidity().getValue(), true, true) + "%rH");
                view.setElementTitleState(view.getElementTitleState() + ", " + view.getStateHumidity());
            }
            formatClimateIcons(climate, view);

            // Background color
            formatClimateBackground(climate, view);

            // Tendency icons
            formatClimateTendency(climate, view);

            // Heating
            formatClimateHeating(heating, climate, view);

        } else {
            view.setStateTemperature("?");
            view.setStateShort("?");
        }

        formatClimateHints(climate, view);

        return view;
    }

    public void formatClimateIcons(Climate climate, ClimateView view) {

        if (climate instanceof RoomClimate && ((RoomClimate) climate).getHumidityWetterThanOutdoor() != null) {
            view.setAbsoluteHumidityIcon(
                    ((RoomClimate) climate).getHumidityWetterThanOutdoor() ? "fas fa-tint" : "fas fa-tint-slash");
        }
        if (climate.getTemperature().getValue().compareTo(FROST_TEMP) < 0) {
            view.setStatePostfixIconTemperature("far fa-snowflake");
        }
    }

    public void formatClimateHints(Climate climate, ClimateView view) {
        if (climate instanceof RoomClimate) {
            for (String hintText : ((RoomClimate) climate).getHints().formatAsText(false, false, null)) {
                view.getHints().add(hintText);
            }
        }
    }

    public boolean climateStateUnknown(Climate climate) {
        return (climate == null || climate.getTemperature() == null || climate.getTemperature().getValue() == null)
                && (climate == null || climate.getHumidity() == null || climate.getHumidity().getValue() == null);
    }

    private void formatClimateTendency(Climate climate, ClimateView view) {

        if (climate.getTemperature().getTendency() != null) {
            view.setTendencyIconTemperature(climate.getTemperature().getTendency().getIconCssClass());
        }
        if (climate.getHumidity() != null && climate.getHumidity().getTendency() != null) {
            view.setTendencyIconHumidity(climate.getHumidity().getTendency().getIconCssClass());
        }
    }

    private void formatClimateHeating(Heating heating, Climate climate, ClimateView view) {

        if (heating != null) {

            view.setTargetTemp(format(heating.getTargetTemperature(), false, false));
            view.setHeatericon("fab fa-hotjar");
            view.setBusy(Boolean.toString(heating.isBusy()));

            view.setLinkManual(MESSAGEPATH + TYPE_IS + MessageType.HEATINGMANUAL + AND_DEVICE_IS + heating.getDevice().name());
            view.setLinkAdjustTemperature(MESSAGEPATH + TYPE_IS + MessageType.HEATINGMANUAL + AND_DEVICE_IS + heating.getDevice().name());
            view.setLinkAuto(MESSAGEPATH + TYPE_IS + MessageType.HEATINGAUTO + AND_DEVICE_IS + heating.getDevice().name()
                    + "&value=null");
            view.setLinkBoost(MESSAGEPATH + TYPE_IS + MessageType.HEATINGBOOST + AND_DEVICE_IS + heating.getDevice().name()
                    + "&value=null");

            lookupHeaterColorClass(heating, climate, view);

            if (heating.isBoostActive()) {
                view.setLinkBoost(StringUtils.EMPTY);
                view.setBoostTimeLeft(String.valueOf(heating.getBoostMinutesLeft()));
                view.setHeaterElementTitleState("Aufheizen");
            } else if (heating.isAutoActive()) {
                view.setLinkAuto(StringUtils.EMPTY);
                view.setHeaterElementTitleState("Automatik, " + view.getTargetTemp() + ViewFormatter.DEGREE + "C");
            }else if(heating.isManualActive()){
                view.setLinkManual(StringUtils.EMPTY);
                view.setHeaterElementTitleState("Manuell, " + view.getTargetTemp() + ViewFormatter.DEGREE + "C");
            } else {
                view.setHeaterElementTitleState(view.getTargetTemp() + ViewFormatter.DEGREE + "C");
            }
        }
    }

    private void lookupHeaterColorClass(Heating heating, Climate climate, ClimateView view) {

        var switchColorClass = ConditionColor.ACTIVE_BUTTON.getUiClass();
        var elementColorClass = ConditionColor.GRAY.getUiClass();

        if (heating.isBoostActive()) {
            switchColorClass = ConditionColor.RED.getUiClass();
            elementColorClass = ConditionColor.RED.getUiClass();
        }else if (heating.getTargetTemperature() != null && climate.getTemperature() != null &&
                climate.getTemperature().getValue() != null) {
            BigDecimal diffTemp = climate.getTemperature().getValue().subtract(heating.getTargetTemperature());
            if (diffTemp.compareTo(HomeAppConstants.MAX_DIFF_HEATING_TEMPERATURE) < 0) {
                switchColorClass = ConditionColor.ORANGE.getUiClass();
                elementColorClass = ConditionColor.ORANGE.getUiClass();
            }else{
                switchColorClass = ConditionColor.GREEN.getUiClass();
                elementColorClass = ConditionColor.GREEN.getUiClass();
            }
        }

        view.setColorClassHeating(elementColorClass);
        view.setActiveSwitchColorClass(switchColorClass);
    }

    private void formatClimateBackground(Climate climate, ClimateView view) {

        formatTemperatureConditionColor(view, List.of(), climate.getTemperature().getValue(), climate.getTemperature().getValue());

        // for now only used in app
        if (climate instanceof RoomClimate && climate.getHumidity() != null) {
            if (climate.getHumidity().getValue().compareTo(HomeAppConstants.TARGET_HUMIDITY_MAX_INSIDE) > 0) {
                view.setColorClassHumidity(ConditionColor.ORANGE.getUiClass());
            } else if (climate.getHumidity().getValue().compareTo(HomeAppConstants.TARGET_HUMIDITY_MIN_INSIDE) < 0) {
                view.setColorClassHumidity(ConditionColor.ORANGE.getUiClass());
            } else {
                view.setColorClassHumidity(ConditionColor.GREEN.getUiClass());
            }
        } else {
            view.setColorClassHumidity(ConditionColor.GRAY.getUiClass());
        }
    }

    private void formatTemperatureConditionColor(View view, List<WeatherConditions> conditions, BigDecimal temperatureMin, BigDecimal temperatureMax) {

        if(temperatureMin == null || temperatureMax == null){
            return;
        }

        if (temperatureMax.compareTo(HIGH_TEMP) > 0) {
            view.setColorClass(ConditionColor.RED.getUiClass());
            view.setIcon("fas fa-thermometer-full");
        } else if (temperatureMax.compareTo(MEDIUM_HIGH_TEMP) > 0) {
            view.setColorClass(ConditionColor.ORANGE.getUiClass());
            view.setIcon("fas fa-thermometer-half");
        } else if (temperatureMin.compareTo(FROST_TEMP) < 0 && temperatureMax.compareTo(MEDIUM_HIGH_TEMP) < 0) {
            view.setColorClass(ConditionColor.COLD.getUiClass());
            view.setIcon("fas fa-thermometer-empty");
        } else if (temperatureMax.compareTo(LOW_TEMP) < 0) {
            view.setColorClass(ConditionColor.BLUE.getUiClass());
            view.setIcon("fas fa-thermometer-empty");
        } else {
            if(!conditions.stream()
                    .filter(WeatherConditions::isSignificant).anyMatch(c -> c.isKindOfRain())){
                view.setColorClass(ConditionColor.GREEN.getUiClass());
            }else if(view instanceof WeatherForecastView){
                view.setColorClass(ConditionColor.DEFAULT.getUiClass());
            }
            view.setIcon("fas fa-thermometer-half");
        }
    }

    private void formatFacadeTemperatures(Model model, String viewKeyMin, String viewKeyMax, HouseModel house) {

        ClimateView viewMin = formatClimate(house.getConclusionClimateFacadeMin(), null, viewKeyMin, false);
        ClimateView viewMax = new ClimateView();
        viewMax.setId(viewKeyMax);

        if (!house.getConclusionClimateFacadeMin().isUnreach()) {
            viewMin
                    .setStateSecondLine("Messpunkt: " + house.getConclusionClimateFacadeMin().getBase().getPlace().getPlaceName());
            viewMin.setHistoryKey(house.getConclusionClimateFacadeMin().getDevice().historyKeyPrefix());
        }

        if (!house.getConclusionClimateFacadeMax().isUnreach()) {
            viewMax.setStateTemperature(house.getConclusionClimateFacadeMax().getSunBeamIntensity().getHeating());
            viewMax.setElementTitleState(house.getConclusionClimateFacadeMax().getSunBeamIntensity().getHeating());
            viewMax.setName(house.getConclusionClimateFacadeMax().getDevice().getPlace().getPlaceName());

            switch (house.getConclusionClimateFacadeMax().getSunBeamIntensity()) {
                case NO:
                    viewMax.setIcon("fas fa-cloud");
                    viewMax.setColorClass(ConditionColor.GRAY.getUiClass());
                    break;
                case LOW:
                    viewMax.setColorClass(ConditionColor.GREEN.getUiClass());
                    viewMax.setIcon("fas fa-cloud-sun");
                    break;
                case MEDIUM:
                    viewMax.setColorClass(ConditionColor.ORANGE.getUiClass());
                    viewMax.setIcon("far fa-sun");
                    break;
                case HIGH:
                    viewMax.setColorClass(ConditionColor.RED.getUiClass());
                    viewMax.setIcon("fas fa-sun");
            }
        }

        model.addAttribute(viewKeyMin, viewMin);
        model.addAttribute(viewKeyMax, viewMax);
    }

    private void formatPower(Model model, PowerMeter powerMeter, List<PowerConsumptionDay> pcd) {

        PowerView power = new PowerView();
        power.setId(powerMeter.getDevice().programNamePrefix());
        power.setPlaceEnum(powerMeter.getDevice().getPlace());
        power.setDescription(powerMeter.getDevice().getDescription());
        power.setUnreach(Boolean.toString(powerMeter.isUnreach()));
        if (powerMeter.isUnreach()) {
            model.addAttribute(powerMeter.getDevice().programNamePrefix(), power);
            return;
        }

        power.setHistoryKey(powerMeter.getDevice().historyKeyPrefix());
        power.setState(powerMeter.getActualConsumption().getValue() == null ? UNBEKANNT
                : powerMeter.getActualConsumption().getValue().intValue() + " W");
        power.setName(powerMeter.getDevice().getType().getTypeName());
        if (powerMeter.getActualConsumption().getTendency() != null) {
            power.setTendencyIcon(powerMeter.getActualConsumption().getTendency().getIconCssClass());
        }

        if (pcd != null && !pcd.isEmpty()) {
            List<ChartEntry> dayViewModel = viewFormatter.fillPowerHistoryDayViewModel(pcd, false);
            if (!dayViewModel.isEmpty()) {
                power.setTodayConsumption(dayViewModel.get(0));
            }
        }

        if (power.getTodayConsumption() == null) {
            power.setElementTitleState("0" + ViewFormatter.K_W_H);
        } else {
            power.setElementTitleState(power.getTodayConsumption().getLabel().replace(ViewFormatter.SUM_SIGN, "").trim());
        }

        if (powerMeter.getDevice() == Device.STROMZAEHLER_WALLBOX) {
            power.setIcon("fas fa-charging-station");
        } else {
            power.setIcon("fas fa-bolt");
        }

        model.addAttribute(powerMeter.getDevice().programNamePrefix(), power);
    }

    private void formatLowBattery(Model model, List<String> lowBatteryDevices) {
        model.addAttribute("lowBattery", lowBatteryDevices);
    }

    private void formatWarnings(Model model, HouseModel houseModel, LightsModel lightsModel, WeatherForecastModel weatherForecastModel, HistoryModel historyModel) {

        List<String> copy = new ArrayList<>(houseModel.getWarnings());

        long diffHm = new Date().getTime() - houseModel.getDateTime();
        if (diffHm > 1000 * HomeAppConstants.MODEL_UPDATE_WARNING_SECONDS) {
            copy.add("Letzte Homematic Aktualisierung vor " + (diffHm / 1000 / 60) + " Min.");
        }

        if(lightsModel==null){
            copy.add("Hue Status unbekannt!");
        }else{
            long diffHue = new Date().getTime() - lightsModel.getTimestamp();
            if (diffHue > 1000 * HomeAppConstants.MODEL_UPDATE_WARNING_SECONDS) {
                copy.add("Letzte Hue Aktualisierung vor " + (diffHue / 1000 / 60) + " Min.");
            }
        }

        if(weatherForecastModel==null){
            copy.add("Wettervorhersage Status unbekannt!");
        }

        if(historyModel==null){
            copy.add("Historiendaten unbekannt!");
        }

        model.addAttribute("warnings", copy);
    }

    private void formatPlaceSubtitles(Model model, HouseModel house) {
        for (Map.Entry<Place, String> entry : house.getPlaceSubtitles().entrySet()) {
            model.addAttribute(PLACE_SUBTITLE_PREFIX + entry.getKey().name(), entry.getValue());
        }
    }

    private void formatWindowSensor(Model model, String viewKey, WindowSensor windowSensor) {

        WindowSensorView view = new WindowSensorView();
        view.setId(viewKey);
        view.setName(windowSensor.getDevice().getType().getTypeName());
        view.setShortName(windowSensor.getDevice().getType().getShortName());
        view.setPlaceEnum(windowSensor.getDevice().getPlace());
        view.setUnreach(Boolean.toString(windowSensor.isUnreach()));
        if (windowSensor.isUnreach()) {
            model.addAttribute(viewKey, view);
            return;
        }

        String stateSuffix = StringUtils.EMPTY;
        String stateDelimiter = StringUtils.EMPTY;

        if (windowSensor.getStateTimestamp() != null) {
            stateDelimiter = ", ";
            stateSuffix = viewFormatter.formatTimestamp(windowSensor.getStateTimestamp(), TimestampFormat.DATE_TIME);
            view.setElementTitleState(
                    "Seit " + viewFormatter.formatTimestamp(windowSensor.getStateTimestamp(), TimestampFormat.SHORT_WITH_TIME));
        }

        view.setState((windowSensor.isState() ? "Geöffnet" : "Geschlossen") + stateDelimiter);
        view.setStateSuffix(stateSuffix);
        view.setStateShort((windowSensor.isState() ? "Geöffnet" : "Geschlossen"));
        if (windowSensor.isState()) {
            view.setColorClass(ConditionColor.ORANGE.getUiClass());
        }
        view.setIcon(windowSensor.isState() ? "fas fa-folder-open" : "fas fa-folder");
        model.addAttribute(viewKey, view);
    }

    private void formatSwitch(Model model, String viewKey, Switch switchModel) {

        SwitchView view = new SwitchView();
        view.setId(viewKey);
        view.setName(switchModel.getDevice().getType().getShortName());
        view.setShortName(switchModel.getDevice().getType().getShortName());
        view.setPlaceEnum(switchModel.getDevice().getPlace());
        view.setUnreach(Boolean.toString(switchModel.isUnreach()));
        if (switchModel.isUnreach()) {
            model.addAttribute(viewKey, view);
            return;
        }

        view.setState(switchModel.isState() ? "Eingeschaltet" : "Ausgeschaltet");
        view.setStateShort(switchModel.isState() ? "Ein" : "Aus");
        formatSwitchColors(switchModel, view);

        formatSwitchAutomation(switchModel, view);

        view.setLabel(switchModel.isState() ? "ausschalten" : "einschalten");
        formatSwitchIcon(switchModel, view);
        if (switchModel.isState()) {
            view.setLinkOff(TOGGLE_STATE + switchModel.getDevice().name() + AND_VALUE_IS + !switchModel.isState());
        } else {
            view.setLinkOn(TOGGLE_STATE + switchModel.getDevice().name() + AND_VALUE_IS + !switchModel.isState());
        }
        model.addAttribute(viewKey, view);
    }

    private void formatSwitchColors(Switch switchModel, SwitchView view) {

        if (switchModel.isState()) {
            String stateColor;
            if (switchModel.getAutomation() != null && Boolean.TRUE.equals(switchModel.getAutomation())) {
                stateColor = ConditionColor.GREEN.getUiClass();
            } else {
                stateColor = ConditionColor.ORANGE.getUiClass();
            }
            view.setColorClass(stateColor);
            view.setActiveSwitchColorClass(stateColor);
        } else {
            view.setActiveSwitchColorClass(ConditionColor.ACTIVE_BUTTON.getUiClass());
        }
    }

    private void formatSwitchIcon(Switch switchModel, SwitchView view) {

        if (switchModel.getDevice().getType() == Type.SWITCH_VENTILATION) {
            view.setIcon("fas fa-fan");
        } else if (isLightSwitch(switchModel.getDevice())) {
            view.setIcon(switchModel.isState() ? "fas fa-lightbulb" : "far fa-lightbulb");
        } else {
            view.setIcon(switchModel.isState() ? "fas fa-toggle-on" : "fas fa-toggle-off");
        }
    }

    private void formatSwitchAutomation(Switch switchModel, SwitchView view) {

        String[] buttonCaptions = StringUtils.substringsBetween(switchModel.getAutomationInfoText(), "{", "}");

        if (ArrayUtils.isNotEmpty(buttonCaptions)) {
            view.setButtonCaptionAuto(buttonCaptions[0]);
            view.setButtonCaptionManual(buttonCaptions[1]);
        }

        if (switchModel.getAutomation() != null) {
            if (Boolean.TRUE.equals(switchModel.getAutomation())) {
                view.setLinkManual(
                        TOGGLE_AUTOMATION + switchModel.getDevice().name() + AND_VALUE_IS + AutomationState.MANUAL.name());
                if (ArrayUtils.isNotEmpty(buttonCaptions)) {
                    view.setStateSuffix(PROGRAMMGESTEUERT + ", " + buttonCaptions[0]);
                    view.setElementTitleState(buttonCaptions[0]);
                } else {
                    view.setStateSuffix(PROGRAMMGESTEUERT);
                    view.setElementTitleState(PROGRAMMGESTEUERT.replaceAll(REGEXP_NOT_ALPHANUMERIC, StringUtils.EMPTY));
                }
            } else {
                view.setLinkAuto(
                        TOGGLE_AUTOMATION + switchModel.getDevice().name() + AND_VALUE_IS + AutomationState.AUTOMATIC.name());
                if (ArrayUtils.isNotEmpty(buttonCaptions)) {
                    view.setStateSuffix(PROGRAMMGESTEUERT + ", " + buttonCaptions[1]);
                    view.setElementTitleState(buttonCaptions[1]);
                } else {
                    view.setStateSuffix(StringUtils.EMPTY);
                    view.setElementTitleState(MANUELL.replaceAll(REGEXP_NOT_ALPHANUMERIC, StringUtils.EMPTY));
                }
            }
            view.setAutoInfoText(
                    StringUtils.trimToEmpty(RegExUtils.removeAll(switchModel.getAutomationInfoText(), "[\\x7b\\x7d]")));
        }
    }

    private boolean isLightSwitch(Device device) {

        String name = device.getType().getTypeName();
        return StringUtils.containsIgnoreCase(name, "licht") || StringUtils.containsIgnoreCase(name, "lampe");
    }

    @SuppressWarnings("unused")
    private void formatWindow(Model model, String viewKey, Shutter windowModel) {

        ShutterView view = new ShutterView();
        view.setId(viewKey);
        view.setName(windowModel.getDevice().getType().getTypeName());
        view.setUnreach(Boolean.toString(windowModel.isUnreach()));
        if (windowModel.isUnreach()) {
            model.addAttribute(viewKey, view);
            return;
        }

        view.setState(windowModel.getShutterPosition().getText(windowModel.getShutterPositionPercentage()));

        if (windowModel.getShutterAutomation() != null) {
            if (Boolean.TRUE.equals(windowModel.getShutterAutomation())) {
                view.setStateSuffix(PROGRAMMGESTEUERT);
                view.setLinkManual(TOGGLE_AUTOMATION + windowModel.getDevice().name() + "&value=false");
            } else {
                view.setLinkAuto(TOGGLE_AUTOMATION + windowModel.getDevice().name() + "&value=true");
            }
            view.setAutoInfoText(windowModel.getShutterAutomationInfoText());
        }

        view.setIcon(windowModel.getShutterPosition().getIcon());
        view.setIconOpen(ShutterPosition.OPEN.getIcon());
        view.setIconHalf(ShutterPosition.HALF.getIcon());
        view.setIconSunshade(ShutterPosition.SUNSHADE.getIcon());
        view.setIconClose(ShutterPosition.CLOSE.getIcon());

        view.setLinkClose(shutterLink(windowModel, ShutterPosition.CLOSE));
        view.setLinkOpen(shutterLink(windowModel, ShutterPosition.OPEN));
        view.setLinkHalf(shutterLink(windowModel, ShutterPosition.HALF));
        view.setLinkSunshade(shutterLink(windowModel, ShutterPosition.SUNSHADE));

        model.addAttribute(viewKey, view);
    }

    private String shutterLink(Shutter windowModel, ShutterPosition shutterPosition) {
        if (shutterPosition == windowModel.getShutterPosition()) {
            return "#";
        } else {
            return MESSAGEPATH + TYPE_IS + MessageType.SHUTTERPOSITION + AND_DEVICE_IS + windowModel.getDevice().name()
                    + AND_VALUE_IS + shutterPosition.getControlPosition();
        }
    }


    private void formatWeatherForecast(Model model, WeatherForecastModel weatherForecastModel) {

        var df = new DecimalFormat("0");
        df.setRoundingMode(RoundingMode.HALF_UP);
        var unreach = weatherForecastModel == null || weatherForecastModel.getForecasts().isEmpty() || weatherForecastModel.getConclusion24to48hours() == null;

        var forecasts = new WeatherForecastsView();
        model.addAttribute("weatherForecasts", forecasts);

        forecasts.setName("2-Tage Wetter");
        forecasts.setPlaceEnum(Place.OUTSIDE);
        forecasts.setId("weatherForecasts");
        forecasts.setColorClass(ConditionColor.GRAY.getUiClass());
        forecasts.setUnreach(Boolean.toString(unreach));

        if(unreach){
            return;
        }

        final WeatherForecastConclusion conclusion24to48hours = weatherForecastModel.getConclusion24to48hours();
        final Map<Integer, String> textMap = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion24to48hours);

        forecasts.setSource(weatherForecastModel.getSourceText());
        forecasts.setColorClass(StringUtils.isNotBlank(textMap.get(WeatherForecastConclusionTextFormatter.SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS)) ? textMap.get(WeatherForecastConclusionTextFormatter.SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS) : ConditionColor.GRAY.getUiClass());
        mapWeatherForecastConditionsAfterSettingColorClass(conclusion24to48hours.getConditions(), forecasts, conclusion24to48hours.getMaxTemp(), conclusion24to48hours.getMinTemp());
        forecasts.setStateShort(textMap.get(WeatherForecastConclusionTextFormatter.FORMAT_FROM_TO_ONLY));
        forecasts.setStateTemperatureWatch(textMap.get(WeatherForecastConclusionTextFormatter.FORMAT_FROM_TO_ONLY));
        forecasts.setElementTitleState(textMap.get(WeatherForecastConclusionTextFormatter.FORMAT_FROM_TO_PLUS_1_MAX));
        forecasts.setIcon(StringUtils.isNotBlank(textMap.get(WeatherForecastConclusionTextFormatter.SIGNIFICANT_CONDITION_WEB_ICON)) ? textMap.get(WeatherForecastConclusionTextFormatter.SIGNIFICANT_CONDITION_WEB_ICON) : "fa-solid fa-clock");
        forecasts.setIconNativeClient(textMap.get(WeatherForecastConclusionTextFormatter.SIGNIFICANT_CONDITION_NATIVE_ICON));
        forecasts.setStateEventWatch(textMap.get(WeatherForecastConclusionTextFormatter.FORMAT_CONDITIONS_1_MAX)); // Watch App 'Ereignis'
        forecasts.setState(StringUtils.EMPTY); // setting state for every day instead

        final LocalDate[] lastDateAdded = {null};
        weatherForecastModel.getForecasts().forEach( fc -> {
            if(lastDateAdded[0] ==null || !lastDateAdded[0].equals(fc.getTime().toLocalDate())){
                var view = new WeatherForecastView();
                final WeatherForecastConclusion conclusionForHeader = weatherForecastModel.getConclusionForDate().get(fc.getTime().toLocalDate());
                final Map<Integer, String> textMapHeader = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusionForHeader);
                view.setHeader(viewFormatter.formatTimestamp(fc.getTime(), TimestampFormat.DATE) + " " + textMapHeader.get(WeatherForecastConclusionTextFormatter.FORMAT_FROM_TO_ALL_SIGNIFICANT_CONDITIONS));
                view.setColorClass(StringUtils.isNotBlank(textMap.get(WeatherForecastConclusionTextFormatter.SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS)) ? textMap.get(WeatherForecastConclusionTextFormatter.SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS) : ConditionColor.DEFAULT.getUiClass());
                mapWeatherForecastConditionsAfterSettingColorClass(conclusionForHeader.getConditions(), view, conclusionForHeader.getMaxTemp(), conclusionForHeader.getMinTemp());
                lastDateAdded[0] = fc.getTime().toLocalDate();
                forecasts.getForecasts().add(view);
            }
            var view = new WeatherForecastView();
            view.setStripeColorClass(fc.isDay() ? ConditionColor.ROW_STRIPE_ACCENT.getUiClass():ConditionColor.ROW_STRIPE_DEFAULT.getUiClass());
            view.setTime(fc.getTime().format(DateTimeFormatter.ofPattern("HH")) + " Uhr");
            view.setTemperature(fc.getTemperature()==null?"":df.format(fc.getTemperature()) + "°C");
            view.setWind(fc.getWind()==null?"":df.format(fc.getWind()) + " km/h");
            final Optional<WeatherConditions> firstSignificantCondition =
                    fc.getIcons().stream().filter(WeatherConditions::isSignificant).findFirst();
            if(firstSignificantCondition.isPresent() && firstSignificantCondition.get().getColor() != null){
                view.setColorClass(firstSignificantCondition.get().getColor().getUiClass());
            }
            mapWeatherForecastConditionsAfterSettingColorClass(fc.getIcons(), view, fc.getTemperature(), fc.getTemperature());
            fc.getIcons().forEach(i -> view.getIcons().add(i.getFontAwesomeID()));
            forecasts.getForecasts().add(view);
        });
    }

    private void mapWeatherForecastConditionsAfterSettingColorClass(List<WeatherConditions> conditions, View view, BigDecimal maxTemp, BigDecimal minTemp) {
        if(view.getColorClass().equals(ConditionColor.GRAY.getUiClass()) || view.getColorClass().equals(ConditionColor.DEFAULT.getUiClass())){
            formatTemperatureConditionColor(view, conditions, minTemp, maxTemp);
        }
    }

    private void formatLights(LightsModel lightsModel, Model model) {

        if (lightsModel != null) {
            lightsModel.getLightsMap().forEach((place, lightsInPlace) -> formatLightsInPlace(place, lightsInPlace, model));
        }

        // format all other places as unreachable
        Arrays.stream(Place.values())
                .filter(p -> lightsModel == null || !lightsModel.getLightsMap().containsKey(p))
                .forEach(p -> model.addAttribute("lights" + p.name(), unreachableLightsView(p)));
    }

    private LightsView unreachableLightsView(Place place) {

        var lights = new LightsView();
        lights.setPlaceEnum(place);
        lights.setUnreach(Boolean.TRUE.toString());
        return lights;
    }

    private void formatLightsInPlace(Place place, List<Light> lightsInPlace, Model model) {

        var lights = new LightsView();
        lights.setId("lights_" + place.name());
        lights.setPlaceEnum(place);

        int countLightsOn = 0;

        for (Light light : lightsInPlace) {

            if (light.getState() == LightState.ON) {
                countLightsOn++;
            }

            var lightView = new LightView();
            lightView.setId("light_" + place.name() + "_" + light.getId());
            lightView.setName(light.getName());
            lightView.setStateShort(light.getState().getCaption());
            lightView.setColorClass(light.getState() == LightState.ON ? ConditionColor.ORANGE.getUiClass() : ConditionColor.GRAY.getUiClass());
            if (light.getState() == LightState.ON) {
                lightView.setLinkOff(TOGGLE_LIGHT + light.getId() + AND_VALUE_IS + Boolean.FALSE);
            } else if (light.getState() == LightState.OFF) {
                lightView.setLinkOn(TOGGLE_LIGHT + light.getId() + AND_VALUE_IS + Boolean.TRUE);
            }

            lights.getLights().add(lightView);
        }

        lights.setColorClass(countLightsOn > 0 ? ConditionColor.ORANGE.getUiClass() : ConditionColor.GRAY.getUiClass());
        lights.setName("Licht");
        lights.setIcon(countLightsOn > 0 ? "fas fa-lightbulb" : "far fa-lightbulb");

        if (countLightsOn == lightsInPlace.size()) {
            lights.setElementTitleState(lightsInPlace.size()==1?StringUtils.capitalize(EINGESCHALTET):"Alle " + EINGESCHALTET);
        } else if (countLightsOn == 0) {
            lights.setElementTitleState(lightsInPlace.size()==1?StringUtils.capitalize(AUSGESCHALTET):"Alle " + AUSGESCHALTET);
        } else {
            lights.setElementTitleState(countLightsOn + "/" + lightsInPlace.size() + " " + EINGESCHALTET);
        }
        lights.setState(lights.getElementTitleState());

        model.addAttribute("lights" + place.name(), lights);
    }

    private void formatPresence(Model model, PresenceModel presenceModel) {

        var view = new PresenceView();
        model.addAttribute("presence", view);

        view.setName("Anwesenheit");
        view.setId("presence");
        view.setPlaceEnum(Place.HOUSE);
        view.setIcon("fa-solid fa-house-user");
        view.setUnreach(Boolean.toString(presenceModel == null));
        if(presenceModel == null){
            return;
        }

        var countKnownState = presenceModel.getPresenceStates().values().stream().filter(s -> s != PresenceState.UNKNOWN).count();
        var namesPresent = presenceModel.getPresenceStates().entrySet().stream().filter(e -> e.getValue() == PresenceState.PRESENT).map(e -> e.getKey()).collect(Collectors.toList());
        var shortText = namesPresent.size() + " / " + countKnownState;

        String longText;
        if(countKnownState == 0){
            longText = "Unbekannt";
        }else if(namesPresent.size() == 0){
            longText = "Keiner zu Hause";
        }else{
            longText = StringUtils.join(namesPresent, ",");
        }

        view.setColorClass(namesPresent.size() > 0 ? ConditionColor.GREEN.getUiClass() : ConditionColor.GRAY.getUiClass());
        view.setStateShort(shortText);
        view.setElementTitleState(shortText);
        view.setState(shortText + " - " + longText);
    }


    private void formatHeatpump(Model model) {

        var view = new HeatpumpView();
        model.addAttribute("heatpumpBedroom", view);

        view.setName("Wärmepumpe");
        view.setId("dummy");
        view.setPlaceEnum(Place.HOUSE);
        view.setIcon("aircon.png");
        view.setUnreach(Boolean.toString(false));

        view.setColorClass(ConditionColor.ORANGE.getUiClass());
        view.setActiveSwitchColorClass(ConditionColor.ORANGE.getUiClass());
        view.setStateShort("Kühlen");
        view.setElementTitleState("Kühlen, Leise");
        view.setState("Kühlen");
        view.setStateSuffix(", Leise");

        view.setLinkCoolAuto("...");
        view.setLinkCoolMin("#"); // active
        view.setLinkHeatAuto("...");
        view.setLinkHeatMin("...");
        view.setLinkFanAuto("...");
        view.setLinkFanMin("...");
        view.setLinkTimer("...");
        view.setLinkOff("...");

        view.setBusy(Boolean.FALSE.toString());
    }
}
