package de.fimatas.home.client.domain.service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
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
import de.fimatas.home.client.domain.model.LockView;
import de.fimatas.home.client.domain.model.PowerView;
import de.fimatas.home.client.domain.model.ShutterView;
import de.fimatas.home.client.domain.model.SwitchView;
import de.fimatas.home.client.domain.model.WindowSensorView;
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
import de.fimatas.home.library.domain.model.OutdoorClimate;
import de.fimatas.home.library.domain.model.PowerConsumptionDay;
import de.fimatas.home.library.domain.model.PowerMeter;
import de.fimatas.home.library.domain.model.RoomClimate;
import de.fimatas.home.library.domain.model.ShutterPosition;
import de.fimatas.home.library.domain.model.StateValue;
import de.fimatas.home.library.domain.model.Switch;
import de.fimatas.home.library.domain.model.Window;
import de.fimatas.home.library.domain.model.WindowSensor;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.homematic.model.Type;
import de.fimatas.home.library.model.Message;
import de.fimatas.home.library.model.MessageType;
import de.fimatas.home.library.util.HomeAppConstants;

@Component
public class HouseViewService {

    private static final String EREIGNISGESTEUERT = ", Ereignissteuerung";

    private static final String PROGRAMMGESTEUERT = ", Automatik";

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

    public void fillViewModel(Model model, HouseModel house, HistoryModel historyModel) {

        model.addAttribute("modelTimestamp", Long.toString(house.getDateTime()));

        formatClimate(model, "tempBathroom", house.getClimateBathRoom(), house.getHeatingBathRoom(), false);
        formatClimate(model, "tempKids", house.getClimateKidsRoom(), null, true);
        formatClimate(model, "tempLivingroom", house.getClimateLivingRoom(), null, false);
        formatClimate(model, "tempBedroom", house.getClimateBedRoom(), null, true);
        formatClimate(model, "tempLaundry", house.getClimateLaundry(), null, true);

        // formatWindow(model, "leftWindowBedroom",
        // house.getLeftWindowBedRoom()); // NOSONAR

        formatFacadeTemperatures(model, "tempMinHouse", "tempMaxHouse", house);

        formatWindowSensor(model, "windowSensorGuestroom", house.getGuestRoomWindowSensor());

        formatSwitch(model, "switchKitchen", house.getKitchenWindowLightSwitch(), false);
        formatSwitch(model, "switchWallbox", house.getWallboxSwitch(), true);
        formatSwitch(model, "switchWorkshopVentilation", house.getWorkshopVentilationSwitch(), true);

        formatFrontDoorBell(model, house.getFrontDoorBell(), house.getFrontDoorCamera());
        formatFrontDoorLock(model, house.getFrontDoorLock());
        formatPower(model, house.getTotalElectricalPowerConsumption(), historyModel.getTotalElectricPowerConsumptionDay());
        formatPower(model, house.getWallboxElectricalPowerConsumption(), historyModel.getWallboxElectricPowerConsumptionDay());

        formatLowBattery(model, house.getLowBatteryDevices());

        formatWarnings(model, house);
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

    private void formatFrontDoorBell(Model model, Doorbell doorbell, Camera camera) {

        FrontDoorView frontDoorView = new FrontDoorView();
        frontDoorView.setUnreach(Boolean.toString(doorbell.isUnreach()));

        if (doorbell.getTimestampLastDoorbell() != null) {
            frontDoorView.setLastDoorbells(viewFormatter.formatPastTimestamp(doorbell.getTimestampLastDoorbell(), true));
        } else {
            frontDoorView.setLastDoorbells("unbekannt");
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

        model.addAttribute("frontDoor", frontDoorView);
    }

    private void formatFrontDoorLock(Model model, Doorlock doorlock) {

        LockView view = new LockView();
        view.setId("frontDoorLock");
        view.setName(doorlock.getDevice().getType().getTypeName());
        view.setCaption(doorlock.getDevice().getPlace().getPlaceName());
        view.setPlace(doorlock.getDevice().getPlace().getPlaceName());
        view.setBusy(Boolean.toString(doorlock.isBusy()));
        view.setUnreach(Boolean.toString(doorlock.isUnreach()));
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

        if (Boolean.TRUE.equals(doorlock.getLockAutomationEvent())) {
            view.setLinkManual(TOGGLE_AUTOMATION + doorlock.getDevice().name() + AND_VALUE_IS + AutomationState.MANUAL.name());
            view.setLinkAuto(TOGGLE_AUTOMATION + doorlock.getDevice().name() + AND_VALUE_IS + AutomationState.AUTOMATIC.name());
            if (doorlock.getErrorcode() == 0) {
                view.setStateSuffix(EREIGNISGESTEUERT);
            }
        } else if (Boolean.TRUE.equals(doorlock.getLockAutomation())) {
            view.setLinkManual(TOGGLE_AUTOMATION + doorlock.getDevice().name() + AND_VALUE_IS + AutomationState.MANUAL.name());
            view.setLinkAutoEvent(
                TOGGLE_AUTOMATION + doorlock.getDevice().name() + AND_VALUE_IS + AutomationState.AUTOMATIC_PLUS_EVENT.name());
            if (doorlock.getErrorcode() == 0) {
                view.setStateSuffix(PROGRAMMGESTEUERT);
            }
        } else {
            view.setLinkAuto(TOGGLE_AUTOMATION + doorlock.getDevice().name() + AND_VALUE_IS + AutomationState.AUTOMATIC.name());
            view.setLinkAutoEvent(
                TOGGLE_AUTOMATION + doorlock.getDevice().name() + AND_VALUE_IS + AutomationState.AUTOMATIC_PLUS_EVENT.name());
        }
        view.setAutoInfoText(StringUtils.trimToEmpty(doorlock.getLockAutomationInfoText()));

        model.addAttribute("frontDoorLock", view);
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
        view.setUnreach(Boolean.toString(climate.isUnreach() || (heating != null && heating.isUnreach())));

        if ((climate == null || climate.getTemperature() == null || climate.getTemperature().getValue() == null)
            && (climate == null || climate.getHumidity() == null || climate.getHumidity().getValue() == null)) {
            view.setStateTemperature("unbekannt");
            return view;
        }

        view.setId(viewKey);
        view.setPlace(climate.getDevice().getPlace().getPlaceName());
        if (history) {
            view.setHistoryKey(climate.getDevice().programNamePrefix());
        }

        if (climate.getTemperature() != null) {
            // Temperature and humidity
            view.setStateTemperature(format(climate.getTemperature().getValue(), false, false) + ViewFormatter.DEGREE + "C");
            if (climate.getHumidity() != null) {
                view.setStateHumidity(format(climate.getHumidity().getValue(), true, true) + "%rH");
            }
            if (climate instanceof RoomClimate && ((RoomClimate) climate).getHumidityWetterThanOutdoor() != null) {
                view.setAbsoluteHumidityIcon(((RoomClimate) climate).getHumidityWetterThanOutdoor().booleanValue()
                    ? "fas fa-tint" : "fas fa-tint-slash");
            }
            if (climate.getTemperature().getValue().compareTo(FROST_TEMP) < 0) {
                view.setStatePostfixIconTemperature("far fa-snowflake");
            }

            // Background color
            formatClimateBackground(climate, view);

            // Tendency icons
            formatClimateTendency(climate, view);

            // Heating
            formatClimateHeating(heating, view);

        } else {
            view.setStateTemperature("?");
        }

        if (climate instanceof RoomClimate) {
            for (String hintText : ((RoomClimate) climate).getHints().formatAsText(false, false, null)) {
                view.getHints().add(hintText);
            }
        }

        return view;
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

        if (house.getConclusionClimateFacadeMin() != null) {
            viewMin.setPostfix(" (" + house.getConclusionClimateFacadeMin().getBase().getPlace().getPlaceName() + ")");
            viewMin.setHistoryKey(house.getConclusionClimateFacadeMin().getDevice().programNamePrefix());
        }

        if (house.getConclusionClimateFacadeMax() != null) {
            viewMax.setStateTemperature(lookupSunHeating(house.getConclusionClimateFacadeMax()));
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
        power.setUnreach(Boolean.toString(powerMeter.isUnreach()));
        power.setId(powerMeter.getDevice().programNamePrefix());
        power.setPlace(powerMeter.getDevice().getPlace().getPlaceName());
        power.setDescription(powerMeter.getDevice().getDescription());
        power.setHistoryKey(powerMeter.getDevice().programNamePrefix());
        power.setState(powerMeter.getActualConsumption().getValue().intValue() + " W");
        power.setName(powerMeter.getDevice().getType().getTypeName());
        if (powerMeter.getDevice() == Device.STROMZAEHLER_WALLBOX) {
            power.setIcon("fas fa-charging-station");
        } else {
            power.setIcon("fas fa-bolt");
        }
        if (powerMeter.getActualConsumption().getTendency() != null) {
            power.setTendencyIcon(powerMeter.getActualConsumption().getTendency().getIconCssClass());
        }

        if (pcd != null && !pcd.isEmpty()) {
            List<ChartEntry> dayViewModel = viewFormatter.fillPowerHistoryDayViewModel(pcd, false);
            if (!dayViewModel.isEmpty()) {
                power.setTodayConsumption(dayViewModel.get(0));
            }
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
        view.setUnreach(Boolean.toString(windowSensor.isUnreach()));
        view.setId(viewKey);
        view.setName(windowSensor.getDevice().getType().getTypeName());
        view.setShortName(windowSensor.getDevice().getType().getShortName());
        view.setPlace(windowSensor.getDevice().getPlace().getPlaceName());

        String stateSuffix = StringUtils.EMPTY;
        String stateDelimiter = StringUtils.EMPTY;

        if (windowSensor.getStateTimestamp() != null) {
            stateDelimiter = ", ";
            stateSuffix =
                "seit " + StringUtils.uncapitalize(viewFormatter.formatPastTimestamp(windowSensor.getStateTimestamp(), true));
        }

        view.setState((windowSensor.isState() ? "Geöffnet" : "Geschlossen") + stateDelimiter);
        view.setStateSuffix(stateSuffix);
        view.setStateShort(windowSensor.isState() ? "Geöffnet" : "Geschlossen");
        if (windowSensor.isState()) {
            view.setColorClass(COLOR_CLASS_ORANGE);
        }
        view.setIcon(windowSensor.isState() ? "fas fa-folder-open" : "fas fa-folder");
        model.addAttribute(viewKey, view);
    }

    private void formatSwitch(Model model, String viewKey, Switch switchModel, boolean highlightStateOn) {

        SwitchView view = new SwitchView();
        view.setUnreach(Boolean.toString(switchModel.isUnreach()));
        view.setId(viewKey);
        view.setName(switchModel.getDevice().getType().getTypeName());
        view.setShortName(switchModel.getDevice().getType().getShortName());
        view.setPlace(switchModel.getDevice().getPlace().getPlaceName());
        view.setState(switchModel.isState() ? "Eingeschaltet" : "Ausgeschaltet");
        view.setStateShort(switchModel.isState() ? "Ein" : "Aus");
        if (switchModel.isState() && highlightStateOn) {
            view.setColorClass(COLOR_CLASS_ORANGE);
        }

        String[] buttonCaptions = StringUtils.substringsBetween(switchModel.getAutomationInfoText(), "{", "}");

        String suffixAuto = PROGRAMMGESTEUERT;
        String suffixManual = "";
        if (ArrayUtils.isNotEmpty(buttonCaptions)) {
            view.setButtonCaptionAuto(buttonCaptions[0]);
            view.setButtonCaptionManual(buttonCaptions[1]);
            suffixAuto += ", " + buttonCaptions[0];
            suffixManual = PROGRAMMGESTEUERT + ", " + buttonCaptions[1];
        }

        if (switchModel.getAutomation() != null) {
            if (Boolean.TRUE.equals(switchModel.getAutomation())) {
                view.setStateSuffix(suffixAuto);
                view.setLinkManual(
                    TOGGLE_AUTOMATION + switchModel.getDevice().name() + AND_VALUE_IS + AutomationState.MANUAL.name());
            } else {
                view.setStateSuffix(suffixManual);
                view.setLinkAuto(
                    TOGGLE_AUTOMATION + switchModel.getDevice().name() + AND_VALUE_IS + AutomationState.AUTOMATIC.name());
            }
            view.setAutoInfoText(
                StringUtils.trimToEmpty(RegExUtils.removeAll(switchModel.getAutomationInfoText(), "[\\x7b\\x7d]")));
        }

        view.setLabel(switchModel.isState() ? "ausschalten" : "einschalten");
        if (switchModel.getDevice().getType() == Type.SWITCH_VENTILATION) {
            view.setIcon("fas fa-fan");
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

    @SuppressWarnings("unused")
    private void formatWindow(Model model, String viewKey, Window windowModel) {

        ShutterView view = new ShutterView();
        view.setUnreach(Boolean.toString(windowModel.isUnreach()));
        view.setId(viewKey);
        view.setName(windowModel.getDevice().getType().getTypeName());
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

    private String shutterLink(Window windowModel, ShutterPosition shutterPosition) {
        if (shutterPosition == windowModel.getShutterPosition()) {
            return "#";
        } else {
            return MESSAGEPATH + TYPE_IS + MessageType.SHUTTERPOSITION + AND_DEVICE_IS + windowModel.getDevice().name()
                + AND_VALUE_IS + shutterPosition.getControlPosition();
        }
    }

}
