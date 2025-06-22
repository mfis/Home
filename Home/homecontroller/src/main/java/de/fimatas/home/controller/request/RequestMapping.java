package de.fimatas.home.controller.request;

import de.fimatas.home.controller.configuration.ScheduledTaskInspector;
import de.fimatas.home.controller.service.LiveActivityService;
import de.fimatas.home.controller.service.PushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Autowired
    private PushService pushService;

    @Autowired
    private LiveActivityService liveActivityService;

    @Value("${test.push.enabled:false}")
    private boolean testPushEnabled;

    @GetMapping("/controller/refresh")
    public ActionModel refresh() {
        houseService.refreshHouseModel(false);
        return new ActionModel("OK");
    }

    @GetMapping("/controller/scheduledTasks")
    public List<ScheduledTaskInspector.ScheduledTaskInfo> getScheduledTasks() {
        return scheduledTaskInspector.getScheduledTasks();
    }

    @GetMapping(value = "/controller/testPush")
    public ActionModel testPush(@RequestParam("user") String user) {
        if(!testPushEnabled){
            return new ActionModel("PustPush is not enabled");
        }
        pushService.testMessage(user);
        return new ActionModel("OK");
    }

    @GetMapping(value = "/controller/testStartLiveActivity")
    public ActionModel testStartLiveActivity() {
        liveActivityService.start(LiveActivityService.TEST_LOKEN_ONLY_LOGGING, "USER", "DEVICE");
        return new ActionModel("OK");
    }
}