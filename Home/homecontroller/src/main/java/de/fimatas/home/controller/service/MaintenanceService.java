package de.fimatas.home.controller.service;

import de.fimatas.home.library.model.MaintenanceOptions;
import de.fimatas.home.library.model.Message;
import lombok.extern.apachecommons.CommonsLog;
import mfi.files.api.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.UnknownHostException;

@Component
@CommonsLog
public class MaintenanceService {

    @Autowired
    private UserService userService;

    @Autowired
    private ClientCommunicationService clientCommunicationService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Environment env;

    public void doMaintenance(Message message){
        if (StringUtils.isNotBlank(message.getSecurityPin())
                && userService.checkPin(message.getUser(), message.getSecurityPin())) {

            var maintenanceOption = MaintenanceOptions.valueOf(message.getValue());
            log.warn("DO_MAINTENANCE: " + maintenanceOption);
            switch (maintenanceOption){
                case REFRESH_MODELS -> clientCommunicationService.refreshAll();
                case REBOOT_CONTROLLER -> controllerReboot();
            }
        }
    }

    private void controllerReboot() {
        log.warn("MAINTENANCE: Rebooting controller");
        try {
            String command = "sudo /sbin/reboot";
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
            processBuilder.start();
        } catch (IOException e) {
            log.error("Reboot konnte nicht ausgeloest werden.", e);
        }
    }

    @Scheduled(cron = "45 0/6 * * * *")
    public void checkDnsResolver() {
        try {
            String url = env.getProperty("dnscheck.url");
            assert url != null;
            restTemplate.getForEntity(url, String.class);
        } catch (ResourceAccessException rae) {
            log.warn("DNS check FAILED: " + rae.getClass() + ": " + rae.getMessage() + " Cause: " + rae.getCause());
            if(rae.getCause() instanceof UnknownHostException) {
                log.warn("Caught UnknownHostException!");
                restartDnsResolver();
            }
        }
    }

    private void restartDnsResolver() {
        try {
            String command = "sudo systemctl restart systemd-resolved";
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
            processBuilder.start();
        } catch (IOException e) {
            log.error("restart systemd-resolved konnte nicht ausgeloest werden.", e);
        }
    }
}
