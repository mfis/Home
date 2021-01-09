package de.fimatas.home.client.domain.service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import de.fimatas.home.client.domain.model.ChartEntry;
import de.fimatas.home.client.domain.model.ClimateView;
import de.fimatas.home.client.domain.model.FrontDoorView;
import de.fimatas.home.client.domain.model.LightView;
import de.fimatas.home.client.domain.model.LightsView;
import de.fimatas.home.client.domain.model.LockView;
import de.fimatas.home.client.domain.model.PowerView;
import de.fimatas.home.client.domain.model.ShutterView;
import de.fimatas.home.client.domain.model.SwitchView;
import de.fimatas.home.client.domain.model.WindowSensorView;
import de.fimatas.home.client.domain.service.ViewFormatter.PastTimestampFormat;
import de.fimatas.home.client.model.MessageQueue;
import de.fimatas.home.library.domain.model.AutomationState;
import de.fimatas.home.library.domain.model.Camera;
import de.fimatas.home.library.domain.model.CameraMode;
import de.fimatas.home.library.domain.model.Climate;
import de.fimatas.home.library.domain.model.Doorbell;
import de.fimatas.home.library.domain.model.Doorlock;
import de.fimatas.home.library.domain.model.Heating;
import de.fimatas.home.library.domain.model.HistoryModel;
import de.fimatas.home.library.domain.model.HouseModel;
import de.fimatas.home.library.domain.model.Intensity;
import de.fimatas.home.library.domain.model.Light;
import de.fimatas.home.library.domain.model.LightState;
import de.fimatas.home.library.domain.model.LightsModel;
import de.fimatas.home.library.domain.model.OutdoorClimate;
import de.fimatas.home.library.domain.model.Place;
import de.fimatas.home.library.domain.model.PowerConsumptionDay;
import de.fimatas.home.library.domain.model.PowerMeter;
import de.fimatas.home.library.domain.model.RoomClimate;
import de.fimatas.home.library.domain.model.Shutter;
import de.fimatas.home.library.domain.model.ShutterPosition;
import de.fimatas.home.library.domain.model.StateValue;
import de.fimatas.home.library.domain.model.Switch;
import de.fimatas.home.library.domain.model.WindowSensor;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.homematic.model.Type;
import de.fimatas.home.library.model.Message;
import de.fimatas.home.library.model.MessageType;
import de.fimatas.home.library.util.HomeAppConstants;

@Component
public class HouseViewService {

    private static final String REGEXP_NOT_ALPHANUMERIC = "[^a-zA-Z0-9]";

    private static final String UNBEKANNT = "unbekannt";

    private static final String EREIGNISGESTEUERT = ", Ereignissteuerung";

    private static final String PROGRAMMGESTEUERT = ", Automatik";

    private static final String MANUELL = ", Manuell";

    private static final String AND_VALUE_IS = "&value=";

    private static final String AND_DEVICE_IS = "&deviceName=";

    public static final String PIN = "securityPin";

    private static final String AND_PIN_IS = "&" + PIN + "=";

    private static final String TYPE_IS = "type=";

    public static final String COLOR_CLASS_RED = "danger";

    public static final String COLOR_CLASS_ORANGE = "warning";

    public static final String COLOR_CLASS_GREEN = "success";

    public static final String COLOR_CLASS_BLUE = "info";

    public static final String COLOR_CLASS_GRAY = "secondary";

    public static final String MESSAGEPATH = "/message?"; // NOSONAR

    private static final String TOGGLE_STATE = MESSAGEPATH + TYPE_IS + MessageType.TOGGLESTATE + AND_DEVICE_IS;

    private static final String TOGGLE_AUTOMATION = MESSAGEPATH + TYPE_IS + MessageType.TOGGLEAUTOMATION + AND_DEVICE_IS;

    private static final String OPEN_STATE = MESSAGEPATH + TYPE_IS + MessageType.OPEN + AND_DEVICE_IS;

    private static final BigDecimal HIGH_TEMP = new BigDecimal("25");

    private static final BigDecimal LOW_TEMP = new BigDecimal("18");

    private static final BigDecimal FROST_TEMP = new BigDecimal("3");

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

    public void fillViewModel(Model model, HouseModel house, HistoryModel historyModel, LightsModel lightsModel) {

        model.addAttribute("modelTimestamp", Long.toString(Long.max(house.getDateTime(), lightsModel.getTimestamp())));

        formatClimate(model, "tempBathroom", house.getClimateBathRoom(), house.getHeatingBathRoom(), false);
        formatClimate(model, "tempKids", house.getClimateKidsRoom(), null, true);
        formatClimate(model, "tempLivingroom", house.getClimateLivingRoom(), null, false);
        formatClimate(model, "tempBedroom", house.getClimateBedRoom(), null, true);
        formatClimate(model, "tempLaundry", house.getClimateLaundry(), null, true);

        // formatWindow(model, "leftWindowBedroom",
        // house.getLeftWindowBedRoom()); // NOSONAR

        formatFacadeTemperatures(model, "tempMinHouse", "tempMaxHouse", house);

        formatWindowSensor(model, "windowSensorGuestroom", house.getGuestRoomWindowSensor());

        formatSwitch(model, "switchKitchen", house.getKitchenWindowLightSwitch());
        formatSwitch(model, "switchWallbox", house.getWallboxSwitch());
        formatSwitch(model, "switchWorkshopVentilation", house.getWorkshopVentilationSwitch());

        formatFrontDoorBell(model, "frontDoor", house.getFrontDoorBell(), house.getFrontDoorCamera());
        formatFrontDoorLock(model, "frontDoorLock", house.getFrontDoorLock());
        formatPower(model, house.getTotalElectricalPowerConsumption(), historyModel.getTotalElectricPowerConsumptionDay());
        formatPower(model, house.getWallboxElectricalPowerConsumption(), historyModel.getWallboxElectricPowerConsumptionDay());

        formatLowBattery(model, house.getLowBatteryDevices());

        formatWarnings(model, house);

        formatLights(lightsModel, model);
    }

    public String lookupSunHeating(OutdoorClimate outdoorMaxClimate) {

        if (outdoorMaxClimate == null || outdoorMaxClimate.getSunBeamIntensity() == null
            || outdoorMaxClimate.getSunHeatingInContrastToShadeIntensity() == null) {
            return StringUtils.EMPTY;
        }

        if (outdoorMaxClimate.getSunBeamIntensity().ordinal() >= outdoorMaxClimate.getSunHeatingInContrastToShadeIntensity()
            .ordinal()) {
            return outdoorMaxClimate.getSunBeamIntensity().getSun();
        } else {
            return outdoorMaxClimate.getSunHeatingInContrastToShadeIntensity().getHeating();
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
                viewFormatter.formatPastTimestamp(doorbell.getTimestampLastDoorbell(), PastTimestampFormat.DATE_TIME)));
            frontDoorView.setElementTitleState(StringUtils
                .capitalize(viewFormatter.formatPastTimestamp(doorbell.getTimestampLastDoorbell(), PastTimestampFormat.SHORT)));
        } else {
            frontDoorView.setLastDoorbells(UNBEKANNT);
        }
        if (camera != null && camera.getDevice() != null) {
            frontDoorView.setIdLive("frontdoorcameralive");
            frontDoorView.setIdBell("frontdoorcamerabell");
            frontDoorView
                .setLinkLive("/cameraPicture?deviceName=" + camera.getDevice() + "&cameraMode=" + CameraMode.LIVE + "&ts=");
            frontDoorView.setLinkLiveRequest("/cameraPictureRequest?type=" + MessageType.CAMERAPICTUREREQUEST + AND_DEVICE_IS
                + camera.getDevice() + "&value=null");
            frontDoorView.setLinkBell("/cameraPicture?deviceName=" + camera.getDevice() + "&cameraMode=" + CameraMode.EVENT
                + "&ts=" + doorbell.getTimestampLastDoorbell());
        }

        model.addAttribute(id, frontDoorView);
    }

    private void formatFrontDoorLock(Model model, String id, Doorlock doorlock) {

        LockView view = new LockView();
        view.setId(id);
        view.setName(doorlock.getDevice().getType().getTypeName());
        view.setCaption(doorlock.getDevice().getPlace().getPlaceName());
        view.setPlace(doorlock.getDevice().getPlace().getPlaceName());
        view.setUnreach(Boolean.toString(doorlock.isUnreach()));
        if (doorlock.isUnreach()) {
            model.addAttribute(id, view);
            return;
        }

        view.setBusy(Boolean.toString(doorlock.isBusy()));
        boolean setButtonLock = false;
        boolean setButtonUnlock = false;
        boolean setButtonOpen = false;

        if (doorlock.getErrorcode() != 0) {
            view.setState("Mechanischer Fehler!");
            view.setIcon("fas fa-bug");
            view.setColorClass(COLOR_CLASS_RED);
            setButtonOpen = true;
            setButtonLock = true;
            setButtonUnlock = true;
        } else if (doorlock.isOpen()) {
            view.setState("Öffner aktiv");
            view.setIcon("fas fa-door-open");
            view.setColorClass(COLOR_CLASS_RED);
            setButtonLock = true;
        } else {
            setButtonOpen = true;
            if (doorlock.isLockStateUncertain()) {
                view.setState("Manuell betätigt");
                view.setIcon("fas fa-door-open");
                view.setColorClass(COLOR_CLASS_ORANGE);
                setButtonLock = true;
                setButtonUnlock = true;
            } else {
                if (doorlock.isLockState()) {
                    view.setState("Verriegelt");
                    view.setIcon("fas fa-lock");
                    view.setColorClass(COLOR_CLASS_GREEN);
                    setButtonUnlock = true;
                } else {
                    view.setState("Entriegelt");
                    view.setIcon("fas fa-lock-open");
                    view.setColorClass(COLOR_CLASS_ORANGE);
                    setButtonLock = true;
                }
            }
        }

        if (setButtonLock) {
            view.setLinkLock(OPEN_STATE + doorlock.getDevice().name() + AND_VALUE_IS + StateValue.LOCK.name() + AND_PIN_IS);
        }
        if (setButtonUnlock) {
            view.setLinkUnlock(OPEN_STATE + doorlock.getDevice().name() + AND_VALUE_IS + StateValue.UNLOCK.name() + AND_PIN_IS);
        }
        if (setButtonOpen) {
            view.setLinkOpen(OPEN_STATE + doorlock.getDevice().name() + AND_VALUE_IS + StateValue.OPEN.name() + AND_PIN_IS);
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

    private String format(BigDecimal val, boolean rounded, boolean onlyInteger) {

        if (val != null) {
            if (onlyInteger) {
                return new DecimalFormat(rounded ? "#" : "0").format(val);
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
        view.setPlace(climate.getDevice().getPlace().getPlaceName());
        view.setUnreach(Boolean.toString(climate.isUnreach() || (heating != null && heating.isUnreach())));
        if (climate.isUnreach() || (heating != null && heating.isUnreach())) {
            return view;
        }

        if (climateStateUnknown(climate)) {
            view.setStateTemperature(UNBEKANNT);
            return view;
        }

        if (history) {
            view.setHistoryKey(climate.getDevice().programNamePrefix());
        }

        if (climate.getTemperature() != null) {
            // Temperature and humidity
            view.setStateTemperature(format(climate.getTemperature().getValue(), false, false) + ViewFormatter.DEGREE + "C");
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
            formatClimateHeating(heating, view);

        } else {
            view.setStateTemperature("?");
        }

        formatClimateHints(climate, view);

        return view;
    }

    public void formatClimateIcons(Climate climate, ClimateView view) {

        if (climate instanceof RoomClimate && ((RoomClimate) climate).getHumidityWetterThanOutdoor() != null) {
            view.setAbsoluteHumidityIcon(
                ((RoomClimate) climate).getHumidityWetterThanOutdoor().booleanValue() ? "fas fa-tint" : "fas fa-tint-slash");
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

    private void formatClimateHeating(Heating heating, ClimateView view) {

        if (heating != null) {
            if (heating.isBoostActive()) {
                view.setLinkBoost(String.valueOf(heating.getBoostMinutesLeft()));
                view.setColorClassHeating(COLOR_CLASS_RED);
            } else {
                view.setLinkBoost(MESSAGEPATH + TYPE_IS + MessageType.HEATINGBOOST + AND_DEVICE_IS + heating.getDevice().name()
                    + "&value=null");
            }
            view.setLinkManual(MESSAGEPATH + TYPE_IS + MessageType.HEATINGMANUAL + AND_DEVICE_IS + heating.getDevice().name());
            view.setTargetTemp(format(heating.getTargetTemperature(), false, false));
            view.setHeaterElementTitleState(view.getTargetTemp() + ViewFormatter.DEGREE + "C");
            view.setHeatericon("fab fa-hotjar");
            view.setBusy(Boolean.toString(heating.isBusy()));
        }
    }

    private void formatClimateBackground(Climate climate, ClimateView view) {

        if (climate.getTemperature().getValue().compareTo(HIGH_TEMP) > 0) {
            view.setColorClass(COLOR_CLASS_RED);
            view.setIcon("fas fa-thermometer-full");
        } else if (climate.getTemperature().getValue().compareTo(LOW_TEMP) < 0) {
            view.setColorClass(COLOR_CLASS_BLUE);
            view.setIcon("fas fa-thermometer-empty");
        } else {
            view.setColorClass(COLOR_CLASS_GREEN);
            view.setIcon("fas fa-thermometer-half");
        }

        // for now only used in app
        if (climate instanceof RoomClimate && climate.getHumidity() != null) {
            if (climate.getHumidity().getValue().compareTo(HomeAppConstants.TARGET_HUMIDITY_MAX_INSIDE) > 0) {
                view.setColorClassHumidity(COLOR_CLASS_ORANGE);
            } else if (climate.getHumidity().getValue().compareTo(HomeAppConstants.TARGET_HUMIDITY_MIN_INSIDE) < 0) {
                view.setColorClassHumidity(COLOR_CLASS_ORANGE);
            } else {
                view.setColorClassHumidity(COLOR_CLASS_GREEN);
            }
        } else {
            view.setColorClassHumidity(COLOR_CLASS_GRAY);
        }
    }

    private void formatFacadeTemperatures(Model model, String viewKeyMin, String viewKeyMax, HouseModel house) {

        ClimateView viewMin = formatClimate(house.getConclusionClimateFacadeMin(), null, viewKeyMin, false);
        ClimateView viewMax = new ClimateView();
        viewMax.setId(viewKeyMax);

        if (!house.getConclusionClimateFacadeMin().isUnreach()) {
            viewMin
                .setStateSecondLine("Messpunkt: " + house.getConclusionClimateFacadeMin().getBase().getPlace().getPlaceName());
            viewMin.setHistoryKey(house.getConclusionClimateFacadeMin().getDevice().programNamePrefix());
        }

        if (!house.getConclusionClimateFacadeMax().isUnreach()) {
            viewMax.setStateTemperature(lookupSunHeating(house.getConclusionClimateFacadeMax()));
            viewMax.setElementTitleState(lookupSunHeating(house.getConclusionClimateFacadeMax()));
            viewMax.setName(house.getConclusionClimateFacadeMax().getDevice().getPlace().getPlaceName());

            switch (Intensity.max(house.getConclusionClimateFacadeMax().getSunBeamIntensity(),
                house.getConclusionClimateFacadeMax().getSunHeatingInContrastToShadeIntensity())) {
            case NO:
                viewMax.setIcon("fas fa-cloud");
                viewMax.setColorClass(COLOR_CLASS_GRAY);
                break;
            case LOW:
                viewMax.setColorClass(COLOR_CLASS_GREEN);
                viewMax.setIcon("fas fa-cloud-sun");
                break;
            case MEDIUM:
                viewMax.setColorClass(COLOR_CLASS_ORANGE);
                viewMax.setIcon("far fa-sun");
                break;
            case HIGH:
                viewMax.setColorClass(COLOR_CLASS_RED);
                viewMax.setIcon("fas fa-sun");
            }
        }

        model.addAttribute(viewKeyMin, viewMin);
        model.addAttribute(viewKeyMax, viewMax);
    }

    private void formatPower(Model model, PowerMeter powerMeter, List<PowerConsumptionDay> pcd) {

        PowerView power = new PowerView();
        power.setId(powerMeter.getDevice().programNamePrefix());
        power.setPlace(powerMeter.getDevice().getPlace().getPlaceName());
        power.setDescription(powerMeter.getDevice().getDescription());
        power.setUnreach(Boolean.toString(powerMeter.isUnreach()));
        if (powerMeter.isUnreach()) {
            model.addAttribute(powerMeter.getDevice().programNamePrefix(), power);
            return;
        }

        power.setHistoryKey(powerMeter.getDevice().programNamePrefix());
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

    private void formatWarnings(Model model, HouseModel houseModel) {

        List<String> copy = new ArrayList<>(houseModel.getWarnings());
        long diff = new Date().getTime() - houseModel.getDateTime();
        if (diff > 1000 * 60 * 20) {
            copy.add("Letzte Aktualisierung vor " + (diff / 1000 / 60) + " Min.");
        }

        model.addAttribute("warnings", copy);
    }

    private void formatWindowSensor(Model model, String viewKey, WindowSensor windowSensor) {

        WindowSensorView view = new WindowSensorView();
        view.setId(viewKey);
        view.setName(windowSensor.getDevice().getType().getTypeName());
        view.setShortName(windowSensor.getDevice().getType().getShortName());
        view.setPlace(windowSensor.getDevice().getPlace().getPlaceName());
        view.setUnreach(Boolean.toString(windowSensor.isUnreach()));
        if (windowSensor.isUnreach()) {
            model.addAttribute(viewKey, view);
            return;
        }

        String stateSuffix = StringUtils.EMPTY;
        String stateDelimiter = StringUtils.EMPTY;

        if (windowSensor.getStateTimestamp() != null) {
            stateDelimiter = ", ";
            stateSuffix = viewFormatter.formatPastTimestamp(windowSensor.getStateTimestamp(), PastTimestampFormat.DATE_TIME);
            view.setElementTitleState(
                "Seit " + viewFormatter.formatPastTimestamp(windowSensor.getStateTimestamp(), PastTimestampFormat.SHORT));
        }

        view.setState((windowSensor.isState() ? "Geöffnet" : "Geschlossen") + stateDelimiter);
        view.setStateSuffix(stateSuffix);
        view.setStateShort((windowSensor.isState() ? "Geöffnet" : "Geschlossen"));
        if (windowSensor.isState()) {
            view.setColorClass(COLOR_CLASS_ORANGE);
        }
        view.setIcon(windowSensor.isState() ? "fas fa-folder-open" : "fas fa-folder");
        model.addAttribute(viewKey, view);
    }

    private void formatSwitch(Model model, String viewKey, Switch switchModel) {

        SwitchView view = new SwitchView();
        view.setId(viewKey);
        view.setName(switchModel.getDevice().getType().getShortName());
        view.setShortName(switchModel.getDevice().getType().getShortName());
        view.setPlace(switchModel.getDevice().getPlace().getPlaceName());
        view.setUnreach(Boolean.toString(switchModel.isUnreach()));
        if (switchModel.isUnreach()) {
            model.addAttribute(viewKey, view);
            return;
        }

        view.setState(switchModel.isState() ? "Eingeschaltet" : "Ausgeschaltet");
        view.setStateShort(switchModel.isState() ? "Ein" : "Aus");
        if (switchModel.isState()) {
            view.setColorClass(COLOR_CLASS_ORANGE);
            view.setActiveSwitchColorClass(COLOR_CLASS_ORANGE);
        } else {
            view.setActiveSwitchColorClass("active-primary");
        }

        formatSwitchAutomation(switchModel, view);

        view.setLabel(switchModel.isState() ? "ausschalten" : "einschalten");
        if (switchModel.getDevice().getType() == Type.SWITCH_VENTILATION) {
            view.setIcon("fas fa-fan");
        } else if (isLightSwitch(switchModel.getDevice())) {
            view.setIcon(switchModel.isState() ? "fas fa-lightbulb" : "far fa-lightbulb");
        } else {
            view.setIcon(switchModel.isState() ? "fas fa-toggle-on" : "fas fa-toggle-off");
        }
        if (switchModel.isState()) {
            view.setLinkOff(TOGGLE_STATE + switchModel.getDevice().name() + AND_VALUE_IS + !switchModel.isState());
        } else {
            view.setLinkOn(TOGGLE_STATE + switchModel.getDevice().name() + AND_VALUE_IS + !switchModel.isState());
        }
        model.addAttribute(viewKey, view);
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

    private void formatLights(LightsModel lightsModel, Model model) {
        
        lightsModel.getLightsMap().forEach((place, lightsInPlace) -> formatLightsInPlace(place, lightsInPlace, model));
        // format all other places as unreachable
        Arrays.asList(Place.values()).stream().filter(p -> !lightsModel.getLightsMap().keySet().contains(p))
            .forEach(p -> model.addAttribute("lights" + p.getPlaceName(), unreachableLightsView(p)));
    }

    private LightsView unreachableLightsView(Place place) {

        var lights = new LightsView();
        lights.setPlace(place.getPlaceName());
        lights.setUnreach(Boolean.TRUE.toString());
        return lights;
    }

    private void formatLightsInPlace(Place place, List<Light> lightsInPlace, Model model) {

        var lights = new LightsView();
        lights.setId("lights_" + place.getPlaceName());
        lights.setPlace(place.getPlaceName());

        int countLightsOn = 0;

        for (Light light : lightsInPlace) {

            if (light.getState() == LightState.ON) {
                countLightsOn++;
            }

            var lightView = new LightView();
            lightView.setId("light_" + place.getPlaceName() + "_" + light.getId());
            lightView.setName(light.getName());
            lightView.setStateShort(light.getState().getCaption());
            lightView.setColorClass(light.getState() == LightState.ON ? COLOR_CLASS_ORANGE : COLOR_CLASS_GRAY);
            if (light.getState() == LightState.ON) {
                lightView.setLinkOff("/...");
            } else if (light.getState() == LightState.OFF) {
                lightView.setLinkOn("/...");
            }

            lights.getLights().add(lightView);
        }

        lights.setColorClass(countLightsOn > 0 ? COLOR_CLASS_ORANGE : COLOR_CLASS_GRAY);
        lights.setName("Licht");
        lights.setIcon(countLightsOn > 0 ? "fas fa-lightbulb" : "far fa-lightbulb");

        if (countLightsOn == lightsInPlace.size()) {
            lights.setElementTitleState("Alle eingeschaltet");
        } else if (countLightsOn == 0) {
            lights.setElementTitleState("Alle ausgeschaltet");
        } else {
            lights.setElementTitleState(countLightsOn + "/" + lightsInPlace.size() + " eingeschaltet");
        }

        model.addAttribute("lights" + place.getPlaceName(), lights);
    }

}
