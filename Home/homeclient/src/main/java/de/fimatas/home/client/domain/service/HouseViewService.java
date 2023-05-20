package de.fimatas.home.client.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

import static de.fimatas.home.library.util.WeatherForecastConclusionTextFormatter.*;

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

    private static final String AND_PLACE_IS = "&placeName=";

    private static final String AND_ADD_DATA_ARE = "&additionalData=";

    private static final String AND_DEVICE_ID_IS = "&deviceId=";

    private static final String NEEDS_PIN = "&needsPin";

    private static final String TYPE_IS = "type=";

    public static final String MESSAGEPATH = "/message?"; // NOSONAR

    private static final String TOGGLE_STATE = MESSAGEPATH + TYPE_IS + MessageType.TOGGLESTATE + AND_DEVICE_IS;

    private static final String TOGGLE_AUTOMATION = MESSAGEPATH + TYPE_IS + MessageType.TOGGLEAUTOMATION + AND_DEVICE_IS;

    private static final String OPEN_STATE = MESSAGEPATH + TYPE_IS + MessageType.OPEN + AND_DEVICE_IS;

    private static final String TOGGLE_LIGHT = MESSAGEPATH + TYPE_IS + MessageType.TOGGLELIGHT + AND_DEVICE_ID_IS;

    private static final String SET_HEATPUMP = MESSAGEPATH + TYPE_IS + MessageType.CONTROL_HEATPUMP + AND_PLACE_IS;

    private static final BigDecimal HIGH_TEMP = new BigDecimal("25.5");

    private static final BigDecimal MEDIUM_HIGH_TEMP = new BigDecimal("23.5");

    private static final BigDecimal LOW_TEMP = new BigDecimal("17.5");

    private static final BigDecimal FROST_TEMP = new BigDecimal("3.5");

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

    public void fillViewModel(Model model, String username, HouseModel house, HistoryModel historyModel, LightsModel lightsModel, WeatherForecastModel weatherForecastModel, PresenceModel presenceModel, HeatpumpModel heatpumpModel, ElectricVehicleModel electricVehicleModel, PushMessageModel pushMessageModel) {

        model.addAttribute("modelTimestamp", ModelObjectDAO.getInstance().calculateModelTimestamp());

        formatViewCorrelations(model);

        formatClimate(model, "tempBathroom", house.getClimateBathRoom(), house.getHeatingBathRoom(), false);
        formatClimate(model, "tempKids1", house.getClimateKidsRoom1(), null, true);
        formatClimate(model, "tempKids2", house.getClimateKidsRoom2(), null, true);
        formatClimate(model, "tempLivingroom", house.getClimateLivingRoom(), null, false);
        formatClimate(model, "tempBedroom", house.getClimateBedRoom(), null, true);
        formatClimate(model, "tempLaundry", house.getClimateLaundry(), null, true);
        formatClimate(model, "tempGuestroom", house.getClimateGuestRoom(), house.getHeatingGuestRoom(), false);
        formatClimate(model, "tempWorkshop", house.getClimateWorkshop(), null, false);

        // formatWindow(model, "leftWindowBedroom", // NOSONAR
        // house.getLeftWindowBedRoom()); // NOSONAR

        formatFacadeTemperatures(model, "tempMinHouse", "tempMaxHouse", house);

        formatWindowSensor(model, "windowSensorGuestroom", house.getGuestRoomWindowSensor());
        formatWindowSensor(model, "windowSensorWorkshop", house.getWorkshopWindowSensor());
        formatWindowSensor(model, "windowSensorLaundry", house.getLaundryWindowSensor());

        formatSwitch(model, "switchKitchen", house.getKitchenWindowLightSwitch());
        formatSwitch(model, "switchWorkshopVentilation", house.getWorkshopVentilationSwitch());

        formatFrontDoorBell(model, "frontDoor", house.getFrontDoorBell(), house.getFrontDoorCamera());
        formatFrontDoorLock(model, "frontDoorLock", house.getFrontDoorLock());

        formatOverallElectricPowerHouse(model, house, historyModel);
        formatPower(model, house.getGasConsumption(), historyModel==null?null:historyModel.getGasConsumptionDay());
        formatWallboxSwitch(model, lookupWallboxId(), house.getWallboxSwitch(), house.getWallboxElectricalPowerConsumption(), electricVehicleModel);
        formatPower(model, house.getWallboxElectricalPowerConsumption(), historyModel==null?null:historyModel.getWallboxElectricPowerConsumptionDay());
        formatEVCharge(model, electricVehicleModel, house.getWallboxElectricalPowerConsumption());

        formatHeatpump(model, house, heatpumpModel, Place.BEDROOM);
        formatHeatpump(model, house, heatpumpModel, Place.KIDSROOM_1);
        formatHeatpump(model, house, heatpumpModel, Place.KIDSROOM_2);

        formatLowBattery(model, house.getLowBatteryDevices());
        formatWarnings(model, house, lightsModel, weatherForecastModel, historyModel);
        formatPushMessages(model, username, pushMessageModel);

        formatPlaceSubtitles(model, house);

        formatLights(lightsModel, model);

        formatWeatherForecast(model, weatherForecastModel);

        formatPresence(model, presenceModel);

        // widget
        formatUpperFloorGroup(model, "widgetUpperFloor", Place.WIDGET_UPPER_FLOOR_TEMPERATURE, house);
        formatGridsGroup(model, "widgetGrids", Place.WIDGET_GRIDS, house, historyModel==null?null:historyModel.getPurchasedElectricPowerConsumptionDay(), historyModel==null?null:historyModel.getGasConsumptionDay());
        formatEnergyGroup(model, "widgetEnergy", Place.WIDGET_ENERGY, electricVehicleModel, house.getProducedElectricalPower(), house.getGridElectricalPower());
        formatSymbolsGroup(model, "widgetSymbols", Place.WIDGET_SYMBOLS, presenceModel, heatpumpModel);
    }

    private void formatUpperFloorGroup(Model model, String viewKey, Place place, HouseModel house) {

        var subPlaces = house.lookupFields(RoomClimate.class).values().stream()
                .filter(c -> place.getSubPlaces().contains(c.getDevice().getPlace())).collect(Collectors.toList());

        if (subPlaces.stream().anyMatch(AbstractDeviceModel::isUnreach)) {
            return;
        }

        WidgetGroupView view = new WidgetGroupView(viewKey, place, subPlaces);
        model.addAttribute(viewKey, view);
        if (view.isUnreach()) {
            return;
        }

        subPlaces.forEach(sp -> {
            if(!sp.isBusy() && !sp.isUnreach()){
                var singlePlace = sp.getDevice().getPlace();
                var key = house.getPlaceSubtitles().containsKey(singlePlace) ? house.getPlaceSubtitles().get(singlePlace) : singlePlace.getPlaceName();
                key = lookupShortenedRoomName(key);
                var sv = new View();
                sv.setId(lookupClimateId(singlePlace, true));
                sv.setState(format(sp.getTemperature().getValue(), true, true) + ViewFormatter.DEGREE + "C");
                formatClimateBackground(sp, sv);
                view.getCaptionAndValue().put(key, sv);
            }
        });
    }

    private void formatGridsGroup(Model model, String viewKey, Place pseudo, HouseModel house, List<PowerConsumptionDay> pcdElectric, List<PowerConsumptionDay> pcdGas) {

        WidgetGroupView view = new WidgetGroupView(viewKey, pseudo, pcdElectric);
        model.addAttribute(viewKey, view);
        if (view.isUnreach()) {
            return;
        }

        var electric = new View();
        electric.setId(lookupTodayPowerId(Device.STROMZAEHLER_BEZUG, true));
        electric.setState("0" + ViewFormatter.powerConsumptionUnit(house.getGridElectricalPower().getDevice()));
        if (pcdElectric != null &&!pcdElectric.isEmpty()) {
            List<ChartEntry> dayViewModel = viewFormatter.fillPowerHistoryDayViewModel(house.getGridElectricalPower().getDevice(), pcdElectric, false, true);
            if (dayViewModel != null && !dayViewModel.isEmpty()) {
                electric.setState(dayViewModel.get(0).getLabel().replace(ViewFormatter.SUM_SIGN, "").trim());
                electric.setColorClass(ConditionColor.ORANGE.getUiClass());
            }
        }
        view.getCaptionAndValue().put("Strom", electric);

        var gas = new View();
        gas.setId(lookupTodayPowerId(house.getGasConsumption().getDevice(), true));
        gas.setState("0" + ViewFormatter.powerConsumptionUnit(house.getGasConsumption().getDevice()));
        if (pcdGas != null &&!pcdGas.isEmpty()) {
            List<ChartEntry> dayViewModel = viewFormatter.fillPowerHistoryDayViewModel(house.getGasConsumption().getDevice(), pcdGas, false, true);
            if (dayViewModel != null && !dayViewModel.isEmpty()) {
                gas.setState(dayViewModel.get(0).getLabel().replace(ViewFormatter.SUM_SIGN, "").trim());
                gas.setColorClass(calculateViewConditionColorGridPowerActualDayDay(house.getGasConsumption().getDevice(), dayViewModel.get(0).getNumericValue()).getUiClass());
            }
        }
        view.getCaptionAndValue().put("Gas", gas);

    }

    private void formatEnergyGroup(Model model, String viewKey, Place place, ElectricVehicleModel electricVehicleModel, PowerMeter producedElectricalPower, PowerMeter gridElectricalPower) {

        WidgetGroupView view = new WidgetGroupView(viewKey, place, electricVehicleModel);
        model.addAttribute(viewKey, view);
        if (view.isUnreach()) {
            return;
        }

        var pv = new View();
        pv.setId(lookupTodayPowerId(Device.STROMZAEHLER_BEZUG, true) + "2" /* FIXME */);
        pv.setState("?");
        if(producedElectricalPower != null && !producedElectricalPower.isUnreach() && gridElectricalPower != null && !gridElectricalPower.isUnreach()){
            int feed = gridElectricalPower.getActualConsumption().getValue().intValue();
            if(feed < 0){
                feed = 0;
            }
            int production = producedElectricalPower.getActualConsumption().getValue().intValue();
            if(production < 0){
                production = 0;
            }
            pv.setState(feed + "/" + production + ViewFormatter.actualPowerUnit(Device.STROMZAEHLER_BEZUG));
            pv.setColorClass(feed > 0 ? ConditionColor.GREEN.getUiClass() : ConditionColor.ORANGE.getUiClass());
        }
        view.getCaptionAndValue().put("Überschuss", pv);

        electricVehicleModel.getEvMap().entrySet().stream().filter(e -> !e.getKey().isOther()).forEach(e -> {
            if(!e.getValue().getElectricVehicle().isOther()){
                var ev = new View();
                ev.setId(lookupEvChargeId(e.getKey(), true));
                ev.setState(calculateViewFormattedPercentageEv(e));
                ev.setColorClass(calculateViewConditionColorEv(calculateViewPercentageEv(e)).getUiClass());
                if(e.getValue().isActiveCharging()){
                    ev.setIconNativeClient("bolt"); // TODO: centralize
                }
                view.getCaptionAndValue().put(e.getKey().getCaption(), ev);
            }
        });
    }

    private void formatSymbolsGroup(Model model, String viewKey, Place place, PresenceModel presenceModel, HeatpumpModel heatpumpModel) {

        WidgetGroupView view = new WidgetGroupView(viewKey, place);
        model.addAttribute(viewKey, view);

        if(heatpumpModel != null){
            var countUnknwn = heatpumpModel.getHeatpumpMap().values().stream().filter(h -> h.getHeatpumpPreset() == HeatpumpPreset.UNKNOWN).count();
            var countOn = heatpumpModel.getHeatpumpMap().values().stream().filter(h -> h.getHeatpumpPreset() != HeatpumpPreset.OFF).count();
            var v = new View();
            v.setId("symbols-heatpump" + lookupGroupitemIdPostfix(true));
            if(countUnknwn>0){
                v.setIconNativeClient("questionmark.circle.fill"); // TODO: centralize
            } else if(countOn > 0){
                v.setIconNativeClient("asset-aircon"); // TODO: centralize
            }
            if(StringUtils.isNotBlank(v.getIconNativeClient())){
                view.getCaptionAndValue().put("heatpump", v);
            }
        }

        if(presenceModel != null){
            var presenceCounter = presenceModel.getPresenceStates().entrySet().stream().filter(e -> e.getValue() == PresenceState.PRESENT).map(e -> e.getKey()).collect(Collectors.toList()).size();
            var v = new View();
            v.setId("symbols-presence" + lookupGroupitemIdPostfix(true));
            v.setIconNativeClient(Integer.toString(presenceCounter) + ".circle"); // TODO: centralize
            view.getCaptionAndValue().put("presence", v);
        }
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
        view.setId(lookupClimateId(climate.getDevice().getPlace(), false));
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

    private void formatClimateBackground(Climate climate, View view) {

        formatTemperatureConditionColor(view, Set.of(), climate.getTemperature().getValue(), climate.getTemperature().getValue());

        // for now only used in app
        if(view instanceof ClimateView){
            ClimateView cv = (ClimateView) view;
            if (climate instanceof RoomClimate && climate.getHumidity() != null) {
                if (climate.getHumidity().getValue().compareTo(HomeAppConstants.TARGET_HUMIDITY_MAX_INSIDE) > 0) {
                    cv.setColorClassHumidity(ConditionColor.ORANGE.getUiClass());
                } else if (climate.getHumidity().getValue().compareTo(HomeAppConstants.TARGET_HUMIDITY_MIN_INSIDE) < 0) {
                    cv.setColorClassHumidity(ConditionColor.ORANGE.getUiClass());
                } else {
                    cv.setColorClassHumidity(ConditionColor.GREEN.getUiClass());
                }
            } else {
                cv.setColorClassHumidity(ConditionColor.GRAY.getUiClass());
            }
        }
    }

    private void formatTemperatureConditionColor(View view, Set<WeatherConditions> conditions, BigDecimal temperatureMin, BigDecimal temperatureMax) {

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

    private void formatOverallElectricPowerHouse(Model model, HouseModel houseModel, HistoryModel historyModel) {

        OverallElectricPowerHouseView overallElectricPowerHouseView = new OverallElectricPowerHouseView();
        Device baseDevice = Device.STROMZAEHLER_BEZUG;
        overallElectricPowerHouseView.setId(lookupTodayPowerId(baseDevice, false));
        overallElectricPowerHouseView.setPlaceEnum(baseDevice.getPlace());
        boolean unreach = houseModel.getGridElectricalPower().isUnreach() || houseModel.getProducedElectricalPower() == null;
        overallElectricPowerHouseView.setUnreach(Boolean.toString(unreach));
        if (unreach) {
            model.addAttribute("overallElectricPowerHouse", overallElectricPowerHouseView);
            return;
        }

        // clear inverter consumption
        BigDecimal offsetConsumption = BigDecimal.ZERO;
        if(houseModel.getProducedElectricalPower().getActualConsumption().getValue() != null && houseModel.getProducedElectricalPower().getActualConsumption().getValue().intValue() < 0){
            offsetConsumption = houseModel.getProducedElectricalPower().getActualConsumption().getValue().abs();
        }

        // sources / targets
        overallElectricPowerHouseView.setConsumption(formatPowerView(model, houseModel.getConsumedElectricalPower(), historyModel==null?null:historyModel.getSelfusedElectricPowerConsumptionDay(), offsetConsumption, false));
        overallElectricPowerHouseView.setPv(formatPowerView(model, houseModel.getProducedElectricalPower(), historyModel==null?null:historyModel.getProducedElectricPowerDay(), offsetConsumption, false));
        overallElectricPowerHouseView.setGridPurchase(formatPowerView(model, houseModel.getGridElectricalPower(), historyModel==null?null:historyModel.getPurchasedElectricPowerConsumptionDay(), BigDecimal.ZERO, true));
        overallElectricPowerHouseView.setGridFeed(formatPowerView(model, houseModel.getGridElectricalPower(), historyModel==null?null:historyModel.getFeedElectricPowerConsumptionDay(), BigDecimal.ZERO, false));

        // history keys
        overallElectricPowerHouseView.getGridPurchase().setHistoryKey(Device.STROMZAEHLER_BEZUG.historyKeyPrefix());
        overallElectricPowerHouseView.getGridFeed().setHistoryKey(Device.STROMZAEHLER_EINSPEISUNG.historyKeyPrefix());
        overallElectricPowerHouseView.getConsumption().setHistoryKey(Device.ELECTRIC_POWER_CONSUMPTION_COUNTER_HOUSE.historyKeyPrefix());
        overallElectricPowerHouseView.getPv().setHistoryKey(Device.ELECTRIC_POWER_PRODUCTION_COUNTER_HOUSE.historyKeyPrefix());

        // grid direction
        overallElectricPowerHouseView.getGridPurchase().setDirectionIcon("fa-solid fa-angles-left");
        overallElectricPowerHouseView.getGridPurchase().setColorClass(ConditionColor.ORANGE.getUiClass());
        if(houseModel.getGridElectricalPower().getActualConsumption().getValue() != null){
            int val = houseModel.getGridElectricalPower().getActualConsumption().getValue().intValue();
            if(val > 0){
                overallElectricPowerHouseView.setGridActualDirection(overallElectricPowerHouseView.getGridFeed());
            }else{
                overallElectricPowerHouseView.setGridActualDirection(overallElectricPowerHouseView.getGridPurchase());
            }
        }

        // color classes pv and grid
        overallElectricPowerHouseView.getGridFeed().setDirectionIcon("fa-solid fa-angles-right");
        overallElectricPowerHouseView.getGridFeed().setColorClass(ConditionColor.GREEN.getUiClass());
        overallElectricPowerHouseView.getPv().setColorClass(houseModel.getProducedElectricalPower().getActualConsumption() != null &&
                houseModel.getProducedElectricalPower().getActualConsumption().getValue() != null ?
                houseModel.getProducedElectricalPower().getActualConsumption().getValue().compareTo(BigDecimal.TEN) > 0 ? ConditionColor.GREEN.getUiClass() :
                        ConditionColor.GRAY.getUiClass() : ConditionColor.GRAY.getUiClass());

        // color classes consumption
        if(houseModel.getProducedElectricalPower().getActualConsumption() != null &&
                houseModel.getProducedElectricalPower().getActualConsumption().getValue() != null &&
                houseModel.getProducedElectricalPower().getActualConsumption().getValue().compareTo(BigDecimal.ZERO) <= 0){
            overallElectricPowerHouseView.getConsumption().setColorClass(ConditionColor.ORANGE.getUiClass());
        }else if (houseModel.getGridElectricalPower().getActualConsumption() != null
                && houseModel.getGridElectricalPower().getActualConsumption().getValue() != null
                && houseModel.getGridElectricalPower().getActualConsumption().getValue().compareTo(BigDecimal.ZERO) >= 0){
            overallElectricPowerHouseView.getConsumption().setColorClass(ConditionColor.GREEN.getUiClass());
        }else{
            overallElectricPowerHouseView.getConsumption().setColorClass(ConditionColor.LIGHT.getUiClass());
        }

        // color class callout
        overallElectricPowerHouseView.setColorClass(overallElectricPowerHouseView.getConsumption().getColorClass().equals(ConditionColor.LIGHT.getUiClass())?
                ConditionColor.GRAY.getUiClass() : overallElectricPowerHouseView.getConsumption().getColorClass());

        // status time
        if(houseModel.getPvStatusTime() > 0){
            LocalDateTime timestamp = Instant.ofEpochMilli(houseModel.getPvStatusTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            long diff = ChronoUnit.MINUTES.between(timestamp, LocalDateTime.now());
            overallElectricPowerHouseView.setTimestampState(diff==0?"jetzt" : "vor " + diff + " Minute" + (diff ==1?"":"n"));
        }

        // indicators
        overallElectricPowerHouseView.setState("");
        overallElectricPowerHouseView.setName("Strom");
        overallElectricPowerHouseView.setIcon("fas fa-bolt");
        overallElectricPowerHouseView.setElementTitleState(overallElectricPowerHouseView.getConsumption().getElementTitleState());

        // set model
        model.addAttribute("overallElectricPowerHouse", overallElectricPowerHouseView);
    }

    private void formatPower(Model model, PowerMeter powerMeter, List<PowerConsumptionDay> pcd) {
        PowerView power = formatPowerView(model, powerMeter, pcd, BigDecimal.ZERO, false);
        model.addAttribute(powerMeter.getDevice().programNamePrefix(), power);
    }

    private PowerView formatPowerView(Model model, PowerMeter powerMeter, List<PowerConsumptionDay> pcd, BigDecimal valueOffset, boolean abs) {

        PowerView power = new PowerView();
        power.setId(lookupTodayPowerId(powerMeter.getDevice(), false));
        power.setPlaceEnum(powerMeter.getDevice().getPlace());
        power.setDevice(powerMeter.getDevice());
        power.setDescription(powerMeter.getDevice().getDescription());
        power.setUnreach(Boolean.toString(powerMeter.isUnreach()));
        if (powerMeter.isUnreach()) {
            return power;
        }

        power.setHistoryKey(powerMeter.getDevice().historyKeyPrefix());
        BigDecimal val = powerMeter.getActualConsumption().getValue() == null? BigDecimal.ZERO : powerMeter.getActualConsumption().getValue().add(valueOffset);
        if(abs){
            val = val.abs();
        }
        power.setState(powerMeter.getActualConsumption().getValue() == null ? UNBEKANNT
                : ViewFormatter.actualPowerConsumptionValueForView(powerMeter.getDevice(), val) + ViewFormatter.actualPowerUnit(power.getDevice()));
        power.setName(powerMeter.getDevice().getType().getTypeName());
        if (powerMeter.getActualConsumption().getTendency() != null) {
            power.setTendencyIcon(powerMeter.getActualConsumption().getTendency().getIconCssClass());
        }

        if (pcd != null && !pcd.isEmpty()) {
            List<ChartEntry> dayViewModel = viewFormatter.fillPowerHistoryDayViewModel(power.getDevice(), pcd, false, false);
            if (!dayViewModel.isEmpty()) {
                power.setTodayConsumption(dayViewModel.get(0));
            }
        }

        if (power.getTodayConsumption() == null) {
            power.setElementTitleState("0" + ViewFormatter.powerConsumptionUnit(power.getDevice()));
        } else {
            power.setElementTitleState(power.getTodayConsumption().getLabel().replace(ViewFormatter.SUM_SIGN, "").trim());
        }

        if (powerMeter.getDevice() == Device.STROMZAEHLER_WALLBOX) {
            power.setIcon("fas fa-charging-station");
        } else if (powerMeter.getDevice() == Device.GASZAEHLER){
            power.setIcon("fa-solid fa-fire-flame-simple");
        } else if (powerMeter.getDevice() == Device.STROMZAEHLER_BEZUG
                || powerMeter.getDevice() == Device.STROMZAEHLER_EINSPEISUNG
                || powerMeter.getDevice() == Device.ELECTRIC_POWER_GRID_ACTUAL_HOUSE){
            power.setIcon("fas fa-bolt");
        } else if (powerMeter.getDevice() == Device.ELECTRIC_POWER_CONSUMPTION_COUNTER_HOUSE
                || powerMeter.getDevice() == Device.ELECTRIC_POWER_CONSUMPTION_ACTUAL_HOUSE) {
            power.setIcon("fa-solid fa-plug");
        } else if (powerMeter.getDevice() == Device.ELECTRIC_POWER_PRODUCTION_COUNTER_HOUSE
                || powerMeter.getDevice() == Device.ELECTRIC_POWER_PRODUCTION_ACTUAL_HOUSE) {
            power.setIcon("fa-solid fa-solar-panel");
        }
        return power;
    }


    private void formatViewCorrelations(Model model) {
        List<ViewCorrelationView> list = new LinkedList<>();
        list.add(new ViewCorrelationView(lookupClimateId(Place.BEDROOM, false), lookupHeatpumpId(Place.BEDROOM, false)));
        list.add(new ViewCorrelationView(lookupClimateId(Place.KIDSROOM_1, false), lookupHeatpumpId(Place.KIDSROOM_1, false)));
        list.add(new ViewCorrelationView(lookupClimateId(Place.KIDSROOM_2, false), lookupHeatpumpId(Place.KIDSROOM_2, false)));
        list.add(new ViewCorrelationView(lookupEvChargeId(ElectricVehicle.EUP, false), lookupWallboxId()));
        model.addAttribute("viewCorrelations", list);
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

    private void formatPushMessages(Model model, String user, PushMessageModel pushMessageModel) {

        var list = pushMessageModel == null ? Collections.EMPTY_LIST :
                pushMessageModel.getList().stream()
                        .filter(msg -> {
                            long minutesOld = Duration
                                    .between(Instant.ofEpochMilli(msg.getTimestamp())
                                            .atZone(ZoneId.systemDefault()).toLocalDateTime(), LocalDateTime.now()).toMinutes();
                            return minutesOld < 60 && msg.getUsername().equalsIgnoreCase(user);
                        })
                        .map(msg -> {
                    var ts = StringUtils.capitalize(viewFormatter.formatTimestamp(msg.getTimestamp(), TimestampFormat.DATE_TIME));
                    return new PushMessageView("id_pm_" + ts, ts, msg.getTitle(), msg.getTextMessage());
                }).collect(Collectors.toList());

        model.addAttribute("pushMessages", list);
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
        formatSwitchInternal(model, viewKey, switchModel, view);
    }

    private void formatWallboxSwitch(Model model, String viewKey, WallboxSwitch switchModel, PowerMeter wallboxElectricalPowerConsumption, ElectricVehicleModel electricVehicleModel) {

        WallboxSwitchView view = new WallboxSwitchView();
        formatSwitchInternal(model, viewKey, switchModel, view);
        if(wallboxElectricalPowerConsumption.isUnreach()){
            view.setUnreach(Boolean.toString(wallboxElectricalPowerConsumption.isUnreach()));
        }
        Arrays.stream(ElectricVehicle.values()).forEach(ev -> {
            ElectroVehicleView evView = new ElectroVehicleView();
            evView.setCaption(ev.getCaption());
            if(electricVehicleModel.getEvMap().get(ev).isConnectedToWallbox()){
               evView.setActiveSwitchColorClass(ConditionColor.ACTIVE_BUTTON.getUiClass());
            }else{
                evView.setLink(MESSAGEPATH + TYPE_IS + MessageType.WALLBOX_SELECTED_EV + AND_DEVICE_ID_IS + ev.name() + AND_VALUE_IS + Boolean.TRUE.toString());
                evView.setActiveSwitchColorClass(view.getColorClass());
            }
            view.getEvs().add(evView);
        });
    }

    private void formatSwitchInternal(Model model, String viewKey, Switch switchModel, SwitchView view) {

        model.addAttribute(viewKey, view);
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
                if(switchModel.getDevice() == Device.SCHALTER_WALLBOX){
                    view.setLinkAuto(StringUtils.EMPTY);
                }else{
                    view.setLinkAuto(
                            TOGGLE_AUTOMATION + switchModel.getDevice().name() + AND_VALUE_IS + AutomationState.AUTOMATIC.name());
                }
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

        forecasts.setName("2-Tage");
        forecasts.setPlaceEnum(Place.OUTSIDE);
        forecasts.setId(lookupWeatherForecastId(Place.OUTSIDE, false));
        forecasts.setColorClass(ConditionColor.GRAY.getUiClass());
        forecasts.setUnreach(Boolean.toString(unreach));

        if(unreach){
            return;
        }

        final WeatherForecastConclusion conclusion24to48hours = weatherForecastModel.getConclusion24to48hours();
        final Map<Integer, String> textMap48h = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion24to48hours);

        final WeatherForecastConclusion conclusion3hours = weatherForecastModel.getConclusion3hours();
        final Map<Integer, String> textMap3h = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion3hours);

        forecasts.setSource(weatherForecastModel.getSourceText());
        forecasts.setColorClass(StringUtils.isNotBlank(textMap48h.get(SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS)) ? textMap48h.get(SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS) : ConditionColor.GRAY.getUiClass());
        mapWeatherForecastConditionsAfterSettingColorClass(conclusion24to48hours.getConditions(), forecasts, conclusion24to48hours.getMaxTemp(), conclusion24to48hours.getMinTemp());
        forecasts.setStateShort(textMap48h.get(FORMAT_FROM_TO_ONLY));
        forecasts.setStateTemperatureWatch(textMap48h.get(FORMAT_FROM_TO_ONLY));
        forecasts.setElementTitleState(textMap48h.get(WeatherForecastConclusionTextFormatter.FORMAT_FROM_TO_PLUS_1_MAX));
        forecasts.setIcon(StringUtils.isNotBlank(textMap48h.get(SIGNIFICANT_CONDITION_WEB_ICON)) ? textMap48h.get(SIGNIFICANT_CONDITION_WEB_ICON) : "fa-solid fa-clock");
        forecasts.setIconNativeClient(textMap48h.get(WeatherForecastConclusionTextFormatter.SIGNIFICANT_CONDITION_NATIVE_ICON));
        forecasts.setStateEventWatch(textMap48h.get(WeatherForecastConclusionTextFormatter.FORMAT_CONDITIONS_SHORT_1_MAX)); // Watch App 'Ereignis'
        forecasts.setState(StringUtils.EMPTY); // setting state for every day instead

        forecasts.setShortTermText(textMap3h.get(FORMAT_CONDITIONS_SHORT_1_MAX_INCL_UNSIGNIFICANT));
        forecasts.setShortTermColorClass(StringUtils.isNotBlank(textMap3h.get(SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS)) ? textMap3h.get(SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS) : ConditionColor.GRAY.getUiClass());

        final LocalDate[] lastDateAdded = {null};
        final String[] lastDayNight = {null};
        weatherForecastModel.getForecasts().forEach( fc -> {
            if(lastDateAdded[0] ==null || !lastDateAdded[0].equals(fc.getTime().toLocalDate())){
                var view = new WeatherForecastView();
                final WeatherForecastConclusion conclusionForHeader = weatherForecastModel.getConclusionForDate().get(fc.getTime().toLocalDate());
                final Map<Integer, String> textMapHeader = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusionForHeader);
                view.setHeader(viewFormatter.formatTimestamp(fc.getTime(), TimestampFormat.DATE) + " " + textMapHeader.get(WeatherForecastConclusionTextFormatter.FORMAT_FROM_TO_ALL_SIGNIFICANT_CONDITIONS));
                view.setColorClass(StringUtils.isNotBlank(textMapHeader.get(SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS)) ? textMapHeader.get(SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS) : ConditionColor.DEFAULT.getUiClass());
                mapWeatherForecastConditionsAfterSettingColorClass(conclusionForHeader.getConditions(), view, conclusionForHeader.getMaxTemp(), conclusionForHeader.getMinTemp());
                lastDateAdded[0] = fc.getTime().toLocalDate();
                forecasts.getForecasts().add(view);
            }
            var view = new WeatherForecastView();
            final Map<Integer, String> textMapSingleEntry = WeatherForecastConclusionTextFormatter.formatConclusionText(WeatherForecastConclusion.fromWeatherForecast(fc));
            view.setStripeColorClass(ConditionColor.ROW_STRIPE_DEFAULT.getUiClass());
            view.setDayNight(fc.isDay() ? "day" : "night");
            view.setDayNightSwitch(Boolean.toString(lastDayNight[0] != null && !lastDayNight[0].equals(view.getDayNight())));
            lastDayNight[0] = view.getDayNight();
            view.setTime(fc.getTime().format(DateTimeFormatter.ofPattern("HH")) + " Uhr");
            view.setTemperature(fc.getTemperature()==null?"":df.format(fc.getTemperature()) + "°C");
            view.setColorClass(StringUtils.isNotBlank(textMapSingleEntry.get(SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS)) ? textMapSingleEntry.get(SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS) : ConditionColor.DEFAULT.getUiClass());
            mapWeatherForecastConditionsAfterSettingColorClass(fc.getIcons(), view, fc.getTemperature(), fc.getTemperature());
            fc.getIcons().forEach(i -> view.getIcons().add(new ValueWithCaption(i.getFontAwesomeID(), i.conditionValue(textMapSingleEntry), null)));
            forecasts.getForecasts().add(view);
        });

        var viewH = new WeatherForecastView();
        viewH.setHeader("ab " + weatherForecastModel.getFurtherDays().keySet().iterator().next().format(DateTimeFormatter.ofPattern("EEEE", Locale.GERMAN)));
        viewH.setColorClass(ConditionColor.DEFAULT.getUiClass());
        forecasts.getForecasts().add(viewH);

        weatherForecastModel.getFurtherDays().forEach((date, conclusion) -> {
            final Map<Integer, String> textMapHeader = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion);
            var view = new WeatherForecastView();
            view.setStripeColorClass(ConditionColor.ROW_STRIPE_DEFAULT.getUiClass());
            view.setTime(date.format(DateTimeFormatter.ofPattern("EEEE", Locale.GERMAN)));
            view.setTemperature(textMapHeader.get(FORMAT_FROM_TO_ONLY));
            if(StringUtils.isNotBlank(textMapHeader.get(SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS))){
                view.setColorClass(textMapHeader.get(SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS));
            }
            mapWeatherForecastConditionsAfterSettingColorClass(conclusion.getConditions(), view, conclusion.getMaxTemp(), conclusion.getMinTemp());
            conclusion.getConditions().stream().filter(WeatherConditions::isSignificant).forEach(i -> view.getIcons().add(new ValueWithCaption(i.getFontAwesomeID(), i.conditionValue(textMapHeader), null)));
            forecasts.getForecasts().add(view);
        });
    }

    private void mapWeatherForecastConditionsAfterSettingColorClass(Set<WeatherConditions> conditions, View view, BigDecimal maxTemp, BigDecimal minTemp) {
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

        var stateShort = lights.getElementTitleState();
        stateShort = StringUtils.replaceIgnoreCase(stateShort, EINGESCHALTET, "ein");
        stateShort = StringUtils.replaceIgnoreCase(stateShort, AUSGESCHALTET, "aus");
        lights.setStateShort(StringUtils.capitalize(stateShort));

        model.addAttribute("lights" + place.name(), lights);
    }

    private void formatPresence(Model model, PresenceModel presenceModel) {

        var view = new PresenceView();
        model.addAttribute("presence", view);

        view.setName("Anwesenheit");
        view.setId("presence");
        view.setPlaceEnum(Place.FRONTDOOR);
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
            longText = StringUtils.join(namesPresent, ", ");
        }

        view.setColorClass(namesPresent.size() > 0 ? ConditionColor.GREEN.getUiClass() : ConditionColor.GRAY.getUiClass());
        view.setStateShort(shortText);
        view.setElementTitleState(shortText);
        view.setState(shortText + " - " + longText);
    }


    private void formatHeatpump(Model model, HouseModel house, HeatpumpModel heatpumpModel, Place place) {

        var isUnreachable = heatpumpModel == null ||
                heatpumpModel.getHeatpumpMap() == null || heatpumpModel.getHeatpumpMap().get(place) == null;

        var view = new HeatpumpView();
        model.addAttribute("heatpump" + place.name(), view);
        view.setName("Wärmepumpe");
        view.setIcon("aircon.png");
        view.setPlaceEnum(place);
        view.setPlaceSubtitle(house.getPlaceSubtitles().containsKey(place) ? house.getPlaceSubtitles().get(place) : place.getPlaceName());
        view.setId(lookupHeatpumpId(place, false));
        view.setUnreach(Boolean.toString(isUnreachable));

        List.of(Place.KIDSROOM_1, Place.KIDSROOM_2, Place.BEDROOM).stream().filter(p -> p != place).forEach(a -> {
            String title = house.getPlaceSubtitles().containsKey(a) ? house.getPlaceSubtitles().get(a) : a.getPlaceName();
            view.getOtherPlaces().add(new ValueWithCaption(a.name(), title, null));
        });

        HeatpumpPreset actualPreset = isUnreachable ? null : heatpumpModel.getHeatpumpMap().get(place).getHeatpumpPreset();

        if(actualPreset==HeatpumpPreset.UNKNOWN){
            view.setLinkRefresh(buildHeatpumpPresetLink(place, HeatpumpPreset.UNKNOWN, actualPreset));
        }else{
            view.setLinkCoolAuto(buildHeatpumpPresetLink(place, HeatpumpPreset.COOL_AUTO, actualPreset));
            view.setLinkCoolMin(buildHeatpumpPresetLink(place, HeatpumpPreset.COOL_MIN, actualPreset));
            view.setLinkCoolTimer1(buildHeatpumpPresetLink(place, HeatpumpPreset.COOL_TIMER1, actualPreset));
            view.setLinkCoolTimer2(buildHeatpumpPresetLink(place, HeatpumpPreset.COOL_TIMER2, actualPreset));
            view.setLinkHeatAuto(buildHeatpumpPresetLink(place, HeatpumpPreset.HEAT_AUTO, actualPreset));
            view.setLinkHeatMin(buildHeatpumpPresetLink(place, HeatpumpPreset.HEAT_MIN, actualPreset));
            view.setLinkHeatTimer1(buildHeatpumpPresetLink(place, HeatpumpPreset.HEAT_TIMER1, actualPreset));
            view.setLinkHeatTimer2(buildHeatpumpPresetLink(place, HeatpumpPreset.HEAT_TIMER2, actualPreset));
            view.setLinkFanAuto(buildHeatpumpPresetLink(place, HeatpumpPreset.FAN_AUTO, actualPreset));
            view.setLinkFanMin(buildHeatpumpPresetLink(place, HeatpumpPreset.FAN_MIN, actualPreset));
            view.setLinkDryTimer(buildHeatpumpPresetLink(place, HeatpumpPreset.DRY_TIMER, actualPreset));
            view.setLinkOff(buildHeatpumpPresetLink(place, HeatpumpPreset.OFF, actualPreset));
        }

        if(isUnreachable){
            return;
        }

        view.setBusy(Boolean.toString(heatpumpModel.isBusy()));

        var timerInfo = "";
        if(heatpumpModel.getHeatpumpMap().get(place).getTimer() != null){
            timerInfo = " bis " + viewFormatter.formatTimestamp(heatpumpModel.getHeatpumpMap().get(place).getTimer(), TimestampFormat.ONLY_TIME);;
        }

        ConditionColor color = actualPreset == null ? ConditionColor.RED: actualPreset.getConditionColor();
        view.setColorClass(color.getUiClass());
        view.setActiveSwitchColorClass(color.getUiClass());
        view.setStateShort(actualPreset.getShortText());
        view.setElementTitleState(heatpumpModel.isBusy()? "Ansteuerung..." : actualPreset.getMode() + (actualPreset.getIntensity()!=null ? ", " + actualPreset.getIntensity() : ""));
        view.setState(actualPreset.getMode());
        view.setStateSuffix(actualPreset.getIntensity()!=null ? (", " + actualPreset.getIntensity() + timerInfo) : "");
    }

    private static String lookupHeatpumpId(Place place, boolean isGroupItem) {
        return place.name() + "_heatpump" + lookupGroupitemIdPostfix(isGroupItem);
    }//_groupitem

    private static String lookupClimateId(Place place, boolean isGroupItem) {
        return place.name() + "_temp" + lookupGroupitemIdPostfix(isGroupItem);
    }

    private static String lookupTodayPowerId(Device device, boolean isGroupItem) {
        return device.name() + "_" + device.getPlace().name() + "_todayPowerSum" + lookupGroupitemIdPostfix(isGroupItem);
    }


    private static String lookupEvChargeId(ElectricVehicle ev, boolean isGroupItem) {
        return ev.name() + "_evcharge" + lookupGroupitemIdPostfix(isGroupItem);
    }

    private static String lookupWeatherForecastId(Place place, boolean isGroupItem) {
        return place.name() + "_fcTemp" + lookupGroupitemIdPostfix(isGroupItem);
    }

    private static String lookupGroupitemIdPostfix(boolean isGroupItem) {
        return isGroupItem? "_groupitem" : "";
    }

    private String lookupShortenedRoomName(String name){
        return StringUtils.remove(name, "zimmer");
    }

    private String lookupWallboxId() {
        return "switchWallbox";
    }

    private String buildHeatpumpPresetLink(Place place, HeatpumpPreset targetPreset, HeatpumpPreset actualPreset){
        if(targetPreset==actualPreset && targetPreset != HeatpumpPreset.UNKNOWN){
            return "#";
        }else{
            return SET_HEATPUMP + place.name() + AND_VALUE_IS + targetPreset.name() + AND_ADD_DATA_ARE;
        }
    }

    private void formatEVCharge(Model model, ElectricVehicleModel electricVehicleModel, PowerMeter wallboxPowerMeter) {

        electricVehicleModel.getEvMap().entrySet().stream().filter(e -> !e.getKey().isOther()).forEach(e -> {

            var view = new ChargingView();
            model.addAttribute("evcharge_" + e.getKey().name(), view);

            view.setName(e.getKey().getCaption());
            view.setId(lookupEvChargeId(e.getKey(), false));
            view.setPlaceEnum(Place.ELECTROVEHICLE);
            view.setIcon("fa-solid fa-car");
            view.setUnreach(Boolean.toString(false));
            if(new Object() == null){
                return;
            }

            var isChargedSinceReading = e.getValue().getChargingTimestamp() != null;
            var isStateNew = ChronoUnit.MINUTES.between(e.getValue().getBatteryPercentageTimestamp(), LocalDateTime.now()) < 2;
            short percentage = calculateViewPercentageEv(e);
            LocalDateTime timestamp = calculateViewTimestampEv(e);

            var tsFormatted = StringUtils.capitalize(viewFormatter.formatTimestamp(timestamp, TimestampFormat.SHORT_WITH_TIME));

            view.setLinkUpdate(MESSAGEPATH + TYPE_IS + MessageType.SLIDERVALUE + AND_DEVICE_ID_IS + e.getKey().name() + AND_VALUE_IS);
            view.setColorClass(calculateViewConditionColorEv(percentage).getUiClass());
            view.setActiveSwitchColorClass(calculateViewConditionColorEv(percentage).getUiClass());
            view.setStateShort(calculateViewFormattedPercentageEv(e)); // watch etc
            view.setStateShortLabel(tsFormatted);
            view.setElementTitleState(StringUtils.capitalize(tsFormatted) + " " + calculateViewFormattedPercentageEv(e)); // collapsed top right
            if(e.getValue().isActiveCharging()){
                if(wallboxPowerMeter.getActualConsumption().getValue().intValue() > 0){
                    view.setState("Lädt gerade");
                }else{
                    if(isChargedSinceReading){
                        view.setState("Bereit zum Laden...");
                    }else{
                        view.setState("Laden starten...");
                    }
                }
            }else if(isChargedSinceReading){
                view.setState("Geladen " + tsFormatted);
            }else{
                view.setState("Gesetzt " + tsFormatted);
            }
            view.setNumericValue(Short.toString(percentage));
            view.setStateActualFlag(Boolean.toString(isStateNew && !isChargedSinceReading));

            view.setChargeLimitLink(MESSAGEPATH + TYPE_IS + MessageType.CHARGELIMIT + AND_DEVICE_ID_IS + e.getKey().name() + AND_VALUE_IS);
            Stream.of(ChargeLimit.values()).forEach(cl -> {
                var value = cl==e.getValue().getChargeLimit() ? "#" : cl.name();
                view.getChargeLimits().add(new ValueWithCaption(value, cl.getCaption(), null));
            });
        });
    }

    private short calculateViewPercentageEv(Map.Entry<ElectricVehicle, ElectricVehicleState> e) {
        if (e.getValue().getChargingTimestamp() != null) {
            var s = (short) (e.getValue().getBatteryPercentage() + e.getValue().getAdditionalChargingPercentage());
            return s>100?100:s;
        } else {
            return e.getValue().getBatteryPercentage();
        }
    }

    private String calculateViewFormattedPercentageEv(Map.Entry<ElectricVehicle, ElectricVehicleState> e) {
        var isChargedSinceReading = e.getValue().getChargingTimestamp() != null;
        var percentagePrefix = isChargedSinceReading?"~":"";
        var percentage = calculateViewPercentageEv(e);
        return percentagePrefix + percentage + "%";
    }

    private LocalDateTime calculateViewTimestampEv(Map.Entry<ElectricVehicle, ElectricVehicleState> e) {
        if (e.getValue().getChargingTimestamp() != null) {
            return e.getValue().getChargingTimestamp();
        } else {
            return e.getValue().getBatteryPercentageTimestamp();
        }
    }

    private ConditionColor calculateViewConditionColorEv(short percentage) {
        return percentage > 89 ? ConditionColor.ORANGE:percentage<21?ConditionColor.RED:ConditionColor.GREEN; // TODO: constant
    }

    private ConditionColor calculateViewConditionColorGridPowerActualDayDay(Device device, BigDecimal kwhDay) {
        var upRoundedHours = LocalTime.now().getHour() + 1;
        var maxKwhPerHourForGreen = device.getType() == Type.GAS_POWER ? new BigDecimal("0.1") : new BigDecimal("0.7"); // TODO: constant
        return kwhDay.compareTo(maxKwhPerHourForGreen.multiply(new BigDecimal(upRoundedHours))) < 0 ? ConditionColor.GREEN : ConditionColor.ORANGE;
    }
}
