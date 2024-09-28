package de.fimatas.home.controller.request;

import de.fimatas.home.controller.configuration.ScheduledTaskInspector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.domain.model.ActionModel;

import java.util.List;

@RestController
public class RequestMapping {

    @Autowired
    private HouseService houseService;

    @Autowired
    private ScheduledTaskInspector scheduledTaskInspector;

    @GetMapping("/controller/refresh")
    public ActionModel refresh() {
        houseService.refreshHouseModel(false);
        return new ActionModel("OK");
    }

    @GetMapping("/controller/scheduledTasks")
    public List<ScheduledTaskInspector.ScheduledTaskInfo> getScheduledTasks() {
        return scheduledTaskInspector.getScheduledTasks();
    }

}