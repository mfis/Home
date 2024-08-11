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
import de.fimatas.home.library.util.HomeAppConstants;
import lombok.Getter;
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

    @Getter
    private PhotovoltaicsStringsStatus stringsStatus = PhotovoltaicsStringsStatus.UNKNOWN;

    @Getter
    private String alarm = null;

    private static final BigDecimal STRING_CHECK_LOWER_LIMIT_AMPS = new BigDecimal("0.5");
    private static final BigDecimal _100 = new BigDecimal("100");

    private String lastStateOfCharge = "";

    private static final Map<String, Device> SOLARMAN_KEY_TO_HM_DEVICE  = new HashMap<>() {{
        put("Et_ge0", Device.ELECTRIC_POWER_PRODUCTION_COUNTER_HOUSE); // Summe PV
        put("Et_use1", Device.ELECTRIC_POWER_CONSUMPTION_COUNTER_HOUSE); // Summe Verbrauch
        put("T_AC_OP", Device.ELECTRIC_POWER_PRODUCTION_ACTUAL_HOUSE); // produktion
        put("E_Puse_t1", Device.ELECTRIC_POWER_CONSUMPTION_ACTUAL_HOUSE); // verbrauch
    }};

    private static final Map<String, String> BATTERY_KEYS  = new HashMap<>() {{
        put("BRC", "Ah"); // Battery Rated Capacity
        put("B_ST1", ""); // Battery Status
        put("B_P1", "W"); // Battery Power
        put("B_left_cap1", "%"); // SoC
        put("t_cg_n1", "kWh"); // Total Charging Energy
        put("t_dcg_n1", "kWh"); // Total Discharging Energy
    }};

    @Scheduled(fixedDelay = (1000 * HomeAppConstants.SOLARMAN_INTERVAL_SECONDS) + 111, initialDelay = 12000)
    public void refresh() {

        final JsonNode currentData = solarmanAPI.callForCurrentData();
        if (currentData == null){
            return;
        }

        List<HomematicCommand> updateCommands = new ArrayList<>();
        long millis = Long.parseLong(currentData.get("collectionTime").asText()) * 1000L;
        var stringAmps = new StringAmps();
        updateCommands.add(homematicCommandBuilder.write(Device.ELECTRIC_POWER_ACTUAL_TIMESTAMP_HOUSE, Datapoint.SYSVAR_DUMMY, Long.toString(millis / 1000)));
        Map<String, String> batteryKeysAndValues = new HashMap<>();

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
            } else if(BATTERY_KEYS.containsKey(key)){
                batteryKeysAndValues.put(key, value);
            }
        }

        hmApi.executeCommand(updateCommands.toArray(new HomematicCommand[0]));
        checkStringsStatus(stringAmps);

        PvAdditionalDataModel pvAdditionalDataModel = processBatteryData(batteryKeysAndValues);
        if(pvAdditionalDataModel != null){
            ModelObjectDAO.getInstance().write(pvAdditionalDataModel);
            uploadService.uploadToClient(pvAdditionalDataModel);
        }
    }

    private PvAdditionalDataModel processBatteryData(Map<String, String> batteryKeysAndValues) {

        String actualStateOfCharge = batteryKeysAndValues.get("B_left_cap1");
        if(StringUtils.equals(lastStateOfCharge, actualStateOfCharge)){
            return null;
        }
        lastStateOfCharge = actualStateOfCharge;

        if(BATTERY_KEYS.size() != batteryKeysAndValues.size()){
            log.warn("Battery keys and values do not match");
            return null;
        }

        var pvAdditionalDataModel = new PvAdditionalDataModel();
        String batteryFileLogPath = DaoUtils.getConfigRoot() + "pvBattery.log";
        String logEntry = LocalDateTime.now() + " - " + batteryKeysAndValues + "\n";
        try {
            FileUtils.writeStringToFile(new File(batteryFileLogPath), logEntry, StandardCharsets.UTF_8, true);
        } catch (IOException e) {
            log.warn("Unable to write battery log to " + batteryFileLogPath, e);
        }

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
        pvAdditionalDataModel.setPvBatteryState(solarmanBatteryStateToInternalBatteryState(batteryKeysAndValues.get("B_ST1")));
        pvAdditionalDataModel.setBatteryWattage(Math.abs(new BigDecimal(batteryKeysAndValues.get("B_P1")).intValue()));
        return pvAdditionalDataModel;
    }

    private PvBatteryState solarmanBatteryStateToInternalBatteryState(String solarmanState){
        if(solarmanState == null){
            return PvBatteryState.STABLE;
        } else if(solarmanState.equalsIgnoreCase("Static")){
            return PvBatteryState.STABLE;
        }else if(solarmanState.equalsIgnoreCase("???_CHARGING_???")){ // FIXME
            return PvBatteryState.CHARGING;
        }else if(solarmanState.equalsIgnoreCase("???_DISCHARGING_???")){ // FIXME
            return PvBatteryState.DISCHARGING;
        }
        return PvBatteryState.STABLE;
    }

    private void checkStringsStatus(StringAmps stringAmps){

        if(stringAmps.string1 == null || stringAmps.string2 == null){
            stringsStatus = PhotovoltaicsStringsStatus.ERROR_DETECTING;
            return;
        }

        if(stringAmps.string1.compareTo(BigDecimal.ZERO) > 0 && stringAmps.string2.compareTo(BigDecimal.ZERO) > 0) {
            stringsStatus = PhotovoltaicsStringsStatus.OKAY;
            return;
        }

        if(stringAmps.string1.compareTo(BigDecimal.ZERO) == 0 && stringAmps.string2.compareTo(BigDecimal.ZERO) == 0) {
            // currently not detectable
            return;
        }

        var min = stringAmps.string1.min(stringAmps.string2);
        var max = stringAmps.string1.max(stringAmps.string2);

        if(min.compareTo(BigDecimal.ZERO) == 0 && max.compareTo(STRING_CHECK_LOWER_LIMIT_AMPS) > 0
                && LocalTime.now().getHour() > 11 /* && LocalTime.now().getHour() < 16 */) {
            stringsStatus = PhotovoltaicsStringsStatus.ONE_FAULTY;
        }
    }

    private static class StringAmps {
        BigDecimal string1;
        BigDecimal string2;
    }
}
