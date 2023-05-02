package de.fimatas.home.controller.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.fimatas.home.controller.api.SolarmanAPI;
import de.fimatas.home.library.util.HomeAppConstants;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
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
    private UploadService uploadService;

    private static final String KEY_CUMULATIVE_PRODUCTION = "Et_ge0";

    private static final String KEY_CUMULATIVE_CONSUMPTION = "Et_use1";

    private static final String KEY_CURRENT_PRODUCTION = "T_AC_OP";

    private static final String KEY_CURRENT_CONSUMPTION = "E_Puse_t1";

    @Scheduled(fixedDelay = (1000 * HomeAppConstants.SOLARMAN_INTERVAL_SECONDS) + 111, initialDelay = 12000)
    public void refresh() {

        final JsonNode currentData = solarmanAPI.callForCurrentData();
        if (currentData == null){
            return;
        }

        log.info("SOLARMAN CURRENT DATA");

        LocalDateTime timestamp =
                Instant.ofEpochMilli(Long.parseLong(currentData.get("collectionTime").asText()) * 1000L).atZone(ZoneId.systemDefault()).toLocalDateTime();
        log.info("  time     = " + timestamp);

        final ArrayNode dataList = (ArrayNode) currentData.get("dataList");
        Iterator<JsonNode> dataListElements = dataList.elements();
        while (dataListElements.hasNext()) {
            JsonNode element = dataListElements.next();
            switch (element.get("key").asText()){
                case KEY_CUMULATIVE_PRODUCTION:
                    log.info("  sum pv   = " + element.get("value").asText());
                    break;
                case KEY_CUMULATIVE_CONSUMPTION:
                    log.info("  sum cons = " + element.get("value").asText());
                    break;

                case KEY_CURRENT_PRODUCTION:
                    log.info("  act pv   = " + element.get("value").asText());
                    break;

                case KEY_CURRENT_CONSUMPTION:
                    log.info("  act cons = " + element.get("value").asText());
                    break;
            }

        }
    }
}
