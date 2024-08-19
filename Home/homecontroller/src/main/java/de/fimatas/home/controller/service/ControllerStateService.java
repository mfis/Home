package de.fimatas.home.controller.service;

import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.model.ControllerStateModel;
import de.fimatas.home.library.util.HomeAppConstants;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
@CommonsLog
public class ControllerStateService {

    @Autowired
    private UploadService uploadService;

    @Scheduled(initialDelay = 1000, fixedDelay = (1000 * HomeAppConstants.MODEL_CONTROLLERSTATE_INTERVAL_SECONDS) + 831)
    private void scheduledRefresh() {
        refresh();
    }

    public void refresh() {

        var model = new ControllerStateModel();
        model.setUptime(getSystemUptime());

        ModelObjectDAO.getInstance().write(model);
        uploadService.uploadToClient(model);
    }

    public static String getSystemUptime() {
        try {
            var uptimeOption = System.getProperty("os.name").toLowerCase().contains("mac") ? "" : " -p";
            Process uptimeProc = Runtime.getRuntime().exec("uptime" + uptimeOption);
            BufferedReader in = new BufferedReader(new InputStreamReader(uptimeProc.getInputStream()));
            return in.readLine();
        } catch (Exception e) {
            return "unknown...";
        }
    }
}
