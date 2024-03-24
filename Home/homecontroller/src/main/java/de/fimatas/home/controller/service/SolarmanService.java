package de.fimatas.home.controller.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.fimatas.home.controller.api.HomematicAPI;
import de.fimatas.home.controller.api.SolarmanAPI;
import de.fimatas.home.controller.command.HomematicCommand;
import de.fimatas.home.controller.command.HomematicCommandBuilder;
import de.fimatas.home.library.homematic.model.Datapoint;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.model.PhotovoltaicsStringsStatus;
import de.fimatas.home.library.util.HomeAppConstants;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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

    @Getter
    private PhotovoltaicsStringsStatus stringsStatus = PhotovoltaicsStringsStatus.UNKNOWN;

    @Getter
    private String alarm = null;

    private static final BigDecimal STRING_CHECK_LOWER_LIMIT_AMPS = new BigDecimal("0.5");

    private static final Map<String, Device> SOLARMAN_KEY_TO_HM_DEVICE  = new HashMap<>() {{
        put("Et_ge0", Device.ELECTRIC_POWER_PRODUCTION_COUNTER_HOUSE); // Summe PV
        put("Et_use1", Device.ELECTRIC_POWER_CONSUMPTION_COUNTER_HOUSE); // Summe Verbrauch
        put("T_AC_OP", Device.ELECTRIC_POWER_PRODUCTION_ACTUAL_HOUSE); // produktion
        put("E_Puse_t1", Device.ELECTRIC_POWER_CONSUMPTION_ACTUAL_HOUSE); // verbrauch
    }};

    @Scheduled(fixedDelay = (1000 * HomeAppConstants.SOLARMAN_INTERVAL_SECONDS) + 111, initialDelay = 12000)
    public void refresh() {

        final JsonNode currentData = solarmanAPI.callForCurrentData();
        if (currentData == null){
            return;
        }

        List<HomematicCommand> updateCommands = new ArrayList<>();
        long millis = Long.parseLong(currentData.get("collectionTime").asText()) * 1000L;
        LocalDateTime timestamp = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime();
        var stringAmps = new StringAmps();
        updateCommands.add(homematicCommandBuilder.write(Device.ELECTRIC_POWER_ACTUAL_TIMESTAMP_HOUSE, Datapoint.SYSVAR_DUMMY, Long.toString(millis / 1000)));
        log.debug("SOLARMAN CURRENT DATA");
        log.debug("   time = " + timestamp);

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
        }

        hmApi.executeCommand(updateCommands.toArray(new HomematicCommand[0]));
        checkStringsStatus(stringAmps);
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
