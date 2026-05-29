package de.fimatas.home.controller.service;

import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.model.ControllerStateModel;
import de.fimatas.home.library.util.HomeUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import oshi.nativefree.SystemInfo;

import java.time.Instant;

@Component
@CommonsLog
public class ControllerStateService {

    @Autowired
    private UploadService uploadService;

    private Instant appUptime;

    private boolean uptimeExceptionLogged = false;

    @PostConstruct
    private void init() {
        appUptime = Instant.now();
    }

    @Scheduled(cron = "30 2/10 * * * *")
    public void scheduledRefresh() {
        refresh();
    }

    public void refresh() {

        var model = new ControllerStateModel();

        model.setSystemUptime("unbekannt");
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("linux")) {
                model.setSystemUptime(HomeUtils.durationSinceFormatted(Instant.ofEpochSecond(new SystemInfo().getOperatingSystem().getSystemBootTime()), true, false, false));
            }
        }catch(Exception e) {
            if(!uptimeExceptionLogged) {
                uptimeExceptionLogged = true;
                log.info("Uptime unknown: ", e);
            }
        }

        model.setAppUptime(HomeUtils.durationSinceFormatted(appUptime, true, false, false));

        ModelObjectDAO.getInstance().write(model);
        uploadService.uploadToClient(model);
    }

}
