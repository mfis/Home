package de.fimatas.home.controller.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.fimatas.home.controller.api.HomematicAPI;
import de.fimatas.home.controller.api.SolarmanAPI;
import de.fimatas.home.controller.command.HomematicCommand;
import de.fimatas.home.controller.command.HomematicCommandBuilder;
import de.fimatas.home.library.homematic.model.Datapoint;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.util.HomeAppConstants;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
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
            }
        }

        hmApi.executeCommand(updateCommands.toArray(new HomematicCommand[0]));
    }
}
