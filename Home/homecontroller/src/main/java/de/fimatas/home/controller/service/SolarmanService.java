package de.fimatas.home.controller.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.fimatas.home.controller.api.HomematicAPI;
import de.fimatas.home.controller.api.SolarmanAPI;
import de.fimatas.home.controller.command.HomematicCommand;
import de.fimatas.home.controller.command.HomematicCommandBuilder;
import de.fimatas.home.controller.dao.DaoUtils;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.PvBatteryMinCharge;
import de.fimatas.home.library.homematic.model.Datapoint;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.model.PhotovoltaicsStringsStatus;
import de.fimatas.home.library.model.PvAdditionalDataModel;
import de.fimatas.home.library.model.PvBatteryState;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
@CommonsLog
public class SolarmanService {

    @Autowired
    private SolarmanAPI solarmanAPI;

    @Autowired
    private HomematicAPI hmApi;

    @Autowired
    private HomematicCommandBuilder homematicCommandBuilder;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private Environment env;

    @Autowired
    private LiveActivityService liveActivityService;

    private static final BigDecimal STRING_CHECK_LOWER_LIMIT_AMPS = new BigDecimal("0.5");
    private static final BigDecimal _100 = new BigDecimal("100");
    private static final DecimalFormat ONE_DIGIT_FORMAT = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.GERMAN));

    private long lastCollectionTimeRead = 0;

    private int lastLoggedSOC = Integer.MIN_VALUE;

    protected static final String FIELD_PRODUCTION_COUNTER = "Et_ge0";
    protected static final String FIELD_CONSUMPTION_COUNTER = "Et_use1";
    protected static final String FIELD_PRODUCTION_ACTUAL = "PVTP";
    protected static final String FIELD_CONSUMPTION_ACTUAL = "E_Puse_t1";

    private static final Map<String, Device> SOLARMAN_KEY_TO_HM_DEVICE  = new HashMap<>() {{
        put(FIELD_PRODUCTION_COUNTER, Device.ELECTRIC_POWER_PRODUCTION_COUNTER_HOUSE); // Summe PV
        put(FIELD_CONSUMPTION_COUNTER, Device.ELECTRIC_POWER_CONSUMPTION_COUNTER_HOUSE); // Summe Verbrauch
        put(FIELD_PRODUCTION_ACTUAL, Device.ELECTRIC_POWER_PRODUCTION_ACTUAL_HOUSE); // produktion
        put(FIELD_CONSUMPTION_ACTUAL, Device.ELECTRIC_POWER_CONSUMPTION_ACTUAL_HOUSE); // verbrauch
    }};

    private static final Map<String, String> INVERTER_KEYS  = new HashMap<>() {{
        put("BRC", "Ah"); // Battery Rated Capacity
        put("B_ST1", ""); // Battery Status
        put("B_P1", "W"); // Battery Power
        put("B_left_cap1", "%"); // SoC
        put(FIELD_PRODUCTION_ACTUAL, "W");
        put(FIELD_CONSUMPTION_ACTUAL, "W");
    }};


    @Scheduled(cron = "0 7,22,37,52 * * * *")
    public void logBatterySOC() {
        if(ModelObjectDAO.getInstance().readPvAdditionalDataModel() != null) {
            var soc = ModelObjectDAO.getInstance().readPvAdditionalDataModel().getBatteryStateOfCharge();
            if(soc < 20 && soc != lastLoggedSOC){
                log.info("BATTERY_SOC: " + soc);
                lastLoggedSOC = soc;
            }
        }
    }

    @Scheduled(cron = "10 * * * * *")
    public void refresh() {

        Instant givenTime = Instant.ofEpochMilli(lastCollectionTimeRead);
        var seconds = Duration.between(givenTime, Instant.now()).toSeconds();
        if(seconds < (60 * 6)){ // inverter send update every 5 minutes + 1 minute delay
            return;
        }

        final JsonNode currentData = solarmanAPI.callForCurrentData();
        if (currentData == null){
            return;
        }

        List<HomematicCommand> updateCommands = new ArrayList<>();
        lastCollectionTimeRead = Long.parseLong(currentData.get("collectionTime").asText()) * 1000L;
        var stringAmps = new StringAmps();
        String alarm = null;
        updateCommands.add(homematicCommandBuilder.write(Device.ELECTRIC_POWER_ACTUAL_TIMESTAMP_HOUSE, Datapoint.SYSVAR_DUMMY, Long.toString(lastCollectionTimeRead / 1000)));
        Map<String, String> inverterKeysAndValues = new HashMap<>();

        final ArrayNode dataList = (ArrayNode) currentData.get("dataList");
        Iterator<JsonNode> dataListElements = dataList.elements();
        while (dataListElements.hasNext()) {
            JsonNode element = dataListElements.next();
            String key = element.get("key").asText();
            String value = element.get("value").asText();
            if(SOLARMAN_KEY_TO_HM_DEVICE.containsKey(key)){
                Device device = SOLARMAN_KEY_TO_HM_DEVICE.get(key);
                log.debug("   " + device + " = " + value);
                updateCommands.add(homematicCommandBuilder.write(device, Datapoint.SYSVAR_DUMMY, value));
            }else if (key.equalsIgnoreCase("DC1")) {
                stringAmps.string1 = new BigDecimal(value);
            }else if (key.equalsIgnoreCase("DC2")) {
                stringAmps.string2 = new BigDecimal(value);
            }else if (key.equalsIgnoreCase("ERR1")) {
                alarm = StringUtils.trimToNull(value);
            }
            if(INVERTER_KEYS.containsKey(key)){
                inverterKeysAndValues.put(key, value);
            }
        }

        hmApi.executeCommand(updateCommands.toArray(new HomematicCommand[0]));

        PvAdditionalDataModel pvAdditionalDataModel = processPvAdditionalDataModel(inverterKeysAndValues, stringAmps, alarm);
        if(pvAdditionalDataModel != null){
            pvAdditionalDataModel.setLastCollectionTimeReadMillis(lastCollectionTimeRead);
            ModelObjectDAO.getInstance().write(pvAdditionalDataModel);
            uploadService.uploadToClient(pvAdditionalDataModel);
            liveActivityService.newModel(PvAdditionalDataModel.class);
        }
    }

    private PvAdditionalDataModel processPvAdditionalDataModel(Map<String, String> inverterKeysAndValues, StringAmps stringAmps, String alarm) {

        String actualStateOfCharge = inverterKeysAndValues.get("B_left_cap1");

        if(INVERTER_KEYS.size() != inverterKeysAndValues.size()){
            log.warn("Battery keys and values do not match");
            return null;
        }

        //noinspection ConstantValue
        if(false) {
            String batteryFileLogPath = DaoUtils.getConfigRoot() + "pvBattery.log";
            String logEntry = LocalDateTime.now() + " - " + inverterKeysAndValues + "\n";
            try {
                FileUtils.writeStringToFile(new File(batteryFileLogPath), logEntry, StandardCharsets.UTF_8, true);
            } catch (IOException e) {
                log.warn("Unable to write battery log to " + batteryFileLogPath, e);
            }
        }

        var pvAdditionalDataModel = new PvAdditionalDataModel();
        pvAdditionalDataModel.setBatteryStateOfCharge(Integer.parseInt(actualStateOfCharge));
        pvAdditionalDataModel.setMaxChargeWattage(Integer.parseInt(Objects.requireNonNull(env.getProperty("solarman.maxChargeWattage"))));
        pvAdditionalDataModel.setMinChargingWattageForOverflowControl((int) ((double)pvAdditionalDataModel.getMaxChargeWattage() *
                Double.parseDouble(Objects.requireNonNull(env.getProperty("solarman.minChargingWattageForOverflowControl.factor")))));
        pvAdditionalDataModel.setBatteryPercentageEmptyForOverflowControl(Integer.parseInt(
                Objects.requireNonNull(env.getProperty("solarman.batteryPercentageEmptyForOverflowControl"))));
        if(pvAdditionalDataModel.getBatteryPercentageEmptyForOverflowControl() > PvBatteryMinCharge.getLowest().getPercentageSwitchOff()){
            log.error("PvBatteryMinCharge too low: " + PvBatteryMinCharge.getLowest().getPercentageSwitchOff());
        }
        pvAdditionalDataModel.setBatteryCapacity(new BigDecimal(Objects.requireNonNull(env.getProperty("solarman.batteryCapacityNetto")))
                .multiply(new BigDecimal(actualStateOfCharge))
                .divide(_100, 2, RoundingMode.HALF_UP));
        pvAdditionalDataModel.setPvBatteryState(solarmanBatteryStateToInternalBatteryState(inverterKeysAndValues.get("B_ST1")));
        pvAdditionalDataModel.setBatteryWattage(Math.abs(new BigDecimal(inverterKeysAndValues.get("B_P1")).intValue()));

        pvAdditionalDataModel.setProductionWattage(new BigDecimal(inverterKeysAndValues.get(FIELD_PRODUCTION_ACTUAL)).intValue());
        pvAdditionalDataModel.setConsumptionWattage(new BigDecimal(inverterKeysAndValues.get(FIELD_CONSUMPTION_ACTUAL)).intValue());

        // DetailInfo
        pvAdditionalDataModel.getDetailInfos().put("Strom obere Reihe", ONE_DIGIT_FORMAT.format(stringAmps.string2) + " Ampere");
        pvAdditionalDataModel.getDetailInfos().put("Strom untere Reihe", ONE_DIGIT_FORMAT.format(stringAmps.string1) + " Ampere");

        pvAdditionalDataModel.setAlarm(alarm);
        processFailures(stringAmps, pvAdditionalDataModel);

        return pvAdditionalDataModel;
    }

    private void processFailures(StringAmps stringAmps, PvAdditionalDataModel pvAdditionalDataModel) {

        boolean setPreviousStringStatus = false;

        if(stringAmps.string1 == null || stringAmps.string2 == null){
            pvAdditionalDataModel.setStringsStatus(PhotovoltaicsStringsStatus.ERROR_DETECTING);
            return;
        }

        if(stringAmps.string1.compareTo(BigDecimal.ZERO) > 0 && stringAmps.string2.compareTo(BigDecimal.ZERO) > 0) {
            pvAdditionalDataModel.setStringsStatus(PhotovoltaicsStringsStatus.OKAY);
            return;
        }

        if(stringAmps.string1.compareTo(BigDecimal.ZERO) == 0 && stringAmps.string2.compareTo(BigDecimal.ZERO) == 0) {
            // currently not detectable
            setPreviousStringStatus = true;
        }

        var min = stringAmps.string1.min(stringAmps.string2);
        var max = stringAmps.string1.max(stringAmps.string2);

        if(min.compareTo(BigDecimal.ZERO) == 0 && max.compareTo(STRING_CHECK_LOWER_LIMIT_AMPS) > 0
                && LocalTime.now().getHour() >= 11 && LocalTime.now().getHour() <= 16 ) {
            pvAdditionalDataModel.setStringsStatus(PhotovoltaicsStringsStatus.ONE_FAULTY);
        }else{
            setPreviousStringStatus = true;
        }

        if(setPreviousStringStatus && ModelObjectDAO.getInstance().readPvAdditionalDataModel() != null){
            pvAdditionalDataModel.setStringsStatus(ModelObjectDAO.getInstance().readPvAdditionalDataModel().getStringsStatus());
        }
    }

    private PvBatteryState solarmanBatteryStateToInternalBatteryState(String solarmanState){
        if(solarmanState == null){
            return PvBatteryState.STABLE;
        }else if(solarmanState.equalsIgnoreCase("Charging")){
            return PvBatteryState.CHARGING;
        }else if(solarmanState.equalsIgnoreCase("Discharging")){
            return PvBatteryState.DISCHARGING;
        }
        return PvBatteryState.STABLE;
    }

    private static class StringAmps {
        BigDecimal string1;
        BigDecimal string2;
    }
}
