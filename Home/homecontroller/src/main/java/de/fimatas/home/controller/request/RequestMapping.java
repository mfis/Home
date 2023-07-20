package de.fimatas.home.controller.request;

import de.fimatas.home.controller.service.PushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.domain.model.ActionModel;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@RestController
public class RequestMapping {

    @Autowired
    private HouseService houseService;

    @Autowired
    private PushService pushService;

    @GetMapping("/controller/refresh")
    public ActionModel refresh() {
        houseService.refreshHouseModel();
        return new ActionModel("OK");
    }

    @GetMapping("controller/memoryInfo")
    public ActionModel memoryInfo() {
        final Runtime runtime = Runtime.getRuntime();
        final long MEM_FACTOR_MB = 1024L * 1024L;
        final String MB = "MB";
        String info = "Memory Information:" +
                " free:" + (runtime.freeMemory() / MEM_FACTOR_MB) + MB +
                " allocated:" + (runtime.totalMemory() / MEM_FACTOR_MB) + MB +
                " max:" + (runtime.maxMemory() / MEM_FACTOR_MB) + MB +
                " totalFree:" + (runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory())) / MEM_FACTOR_MB + MB;
        return new ActionModel(info);
    }

}