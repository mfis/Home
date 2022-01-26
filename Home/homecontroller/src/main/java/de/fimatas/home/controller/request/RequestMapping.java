package de.fimatas.home.controller.request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.domain.model.ActionModel;

import java.text.NumberFormat;

@RestController
public class RequestMapping {

    @Autowired
    private HouseService houseService;

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
        final StringBuilder sb = new StringBuilder();
        sb.append("Memory Information:");
        sb.append(" free:" + (runtime.freeMemory() / MEM_FACTOR_MB) + MB);
        sb.append(" allocated:" + (runtime.totalMemory() / MEM_FACTOR_MB) + MB);
        sb.append(" max:" + (runtime.maxMemory() / MEM_FACTOR_MB) + MB);
        sb.append(" totalFree:" + ((runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory())) / MEM_FACTOR_MB) + MB);
        return new ActionModel(sb.toString());
    }

}