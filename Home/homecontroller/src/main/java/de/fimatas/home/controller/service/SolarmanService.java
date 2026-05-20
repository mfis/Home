package de.fimatas.home.controller.service;

import de.fimatas.home.controller.api.SolarmanAPI;
import de.fimatas.home.controller.command.HomematicCommandBuilder;
import de.fimatas.home.controller.command.PersistentCacheCommand;
import de.fimatas.home.controller.dao.DaoUtils;
import de.fimatas.home.controller.dao.PersistentCacheDAO;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.PvBatteryMinCharge;
import de.fimatas.home.library.domain.model.ValueWithTendency;
import de.fimatas.home.library.model.PersistentCacheKey;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

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

import static de.fimatas.home.library.util.HomeUtils.calculateTendency;

@Component
@CommonsLog
public class SolarmanService {

    @Autowired
    private SolarmanAPI solarmanAPI;

    @Autowired
    private PersistentCacheDAO persistentCacheDAO;

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
    private static final BigDecimal POWER_TENDENCY_DIFF = new BigDecimal("99.99");

    protected static final String FIELD_PRODUCTION_COUNTER = "Et_ge0";
    protected static final String FIELD_CONSUMPTION_COUNTER = "Et_use1";
    protected static final String FIELD_PRODUCTION_ACTUAL = "PVTP";
    protected static final String FIELD_CONSUMPTION_ACTUAL = "E_Puse_t1";

    private static final Map<String, PersistentCacheKey> SOLARMAN_KEY_TO_PERSISTENT_CACHE  = new HashMap<>() {{
        put(FIELD_PRODUCTION_COUNTER, PersistentCacheKey.ELECTRIC_POWER_PRODUCTION_COUNTER_HOUSE); // Summe PV
        put(FIELD_CONSUMPTION_COUNTER, PersistentCacheKey.ELECTRIC_POWER_CONSUMPTION_COUNTER_HOUSE); // Summe Verbrauch
        put(FIELD_PRODUCTION_ACTUAL, PersistentCacheKey.ELECTRIC_POWER_PRODUCTION_ACTUAL_HOUSE); // produktion
        put(FIELD_CONSUMPTION_ACTUAL, PersistentCacheKey.ELECTRIC_POWER_CONSUMPTION_ACTUAL_HOUSE); // verbrauch
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

        var valuesToWriteToPersistentCache = new HashMap<PersistentCacheKey, Object>();
        lastCollectionTimeRead = Long.parseLong(currentData.get("collectionTime").asString()) * 1000L;
        var stringAmps = new StringAmps();
        String alarm = null;
        valuesToWriteToPersistentCache.put(PersistentCacheKey.ELECTRIC_POWER_ACTUAL_TIMESTAMP_HOUSE, (lastCollectionTimeRead / 1000));
        Map<String, String> inverterKeysAndValues = new HashMap<>();

        final ArrayNode dataList = (ArrayNode) currentData.get("dataList");
        Collection<JsonNode> dataListElements = dataList.elements();

        for (JsonNode element : dataListElements) {
            String key = element.get("key").asString();
            String value = element.get("value").asString();
            if(SOLARMAN_KEY_TO_PERSISTENT_CACHE.containsKey(key)){
                PersistentCacheKey persistentCacheKey = SOLARMAN_KEY_TO_PERSISTENT_CACHE.get(key);
                log.debug("   " + persistentCacheKey + " = " + value);
                valuesToWriteToPersistentCache.put(persistentCacheKey, value);
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

        valuesToWriteToPersistentCache.forEach((key, val) -> persistentCacheDAO.write(new PersistentCacheCommand(key, val)));

        PvAdditionalDataModel pvAdditionalDataModel = processPvAdditionalDataModel(inverterKeysAndValues, stringAmps, alarm);
        if(pvAdditionalDataModel != null){
            pvAdditionalDataModel.setLastCollectionTimeReadMillis(lastCollectionTimeRead);
            calculateTendencies(pvAdditionalDataModel);
            ModelObjectDAO.getInstance().write(pvAdditionalDataModel);
            uploadService.uploadToClient(pvAdditionalDataModel);
            liveActivityService.newModel(PvAdditionalDataModel.class);
        }
    }

    private void calculateTendencies(PvAdditionalDataModel pvAdditionalDataModel) {

        var oldModel = ModelObjectDAO.getInstance().readPvAdditionalDataModel();
        ValueWithTendency<BigDecimal> oldProduction;
        ValueWithTendency<BigDecimal> oldConsumption;

        ValueWithTendency<BigDecimal> referencePower;
        if (oldModel == null) {
            oldProduction = pvAdditionalDataModel.getProductionWattage();
            oldProduction.setReferenceValue(oldProduction.getValue());
            oldConsumption = pvAdditionalDataModel.getConsumptionWattage();
            oldConsumption.setReferenceValue(oldConsumption.getValue());
        } else {
            oldProduction = oldModel.getProductionWattage();
            oldConsumption = oldModel.getConsumptionWattage();
        }
        calculateTendency(lastCollectionTimeRead, oldProduction, pvAdditionalDataModel.getProductionWattage(), POWER_TENDENCY_DIFF);
        calculateTendency(lastCollectionTimeRead, oldConsumption, pvAdditionalDataModel.getConsumptionWattage(), POWER_TENDENCY_DIFF);
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

        pvAdditionalDataModel.setProductionWattage(new ValueWithTendency<>(new BigDecimal(inverterKeysAndValues.get(FIELD_PRODUCTION_ACTUAL))));
        pvAdditionalDataModel.setConsumptionWattage(new ValueWithTendency<>(new BigDecimal(inverterKeysAndValues.get(FIELD_CONSUMPTION_ACTUAL))));

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
