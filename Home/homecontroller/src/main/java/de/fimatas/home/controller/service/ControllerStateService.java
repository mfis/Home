package de.fimatas.home.controller.service;

import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.model.ControllerStateModel;
import de.fimatas.home.library.util.HomeAppConstants;
import de.fimatas.home.library.util.HomeUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import java.time.Instant;

@Component
@CommonsLog
public class ControllerStateService {

    @Autowired
    private UploadService uploadService;

    private Instant appUptime;

    @PostConstruct
    private void init() {
        appUptime = Instant.now();
    }

    @Scheduled(initialDelay = 9000, fixedDelay = ((1000 * HomeAppConstants.MODEL_CONTROLLERSTATE_INTERVAL_SECONDS)) + 831)
    private void scheduledRefresh() {
        refresh();
    }

    public void refresh() {

        var model = new ControllerStateModel();
        model.setSystemUptime(HomeUtils.durationSinceFormatted(Instant.ofEpochSecond(new SystemInfo().getOperatingSystem().getSystemBootTime()), true, false, false));
        model.setAppUptime(HomeUtils.durationSinceFormatted(appUptime, true, false, false));

        ModelObjectDAO.getInstance().write(model);
        uploadService.uploadToClient(model);
    }

}
