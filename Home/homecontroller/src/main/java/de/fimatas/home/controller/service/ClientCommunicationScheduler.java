package de.fimatas.home.controller.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ClientCommunicationScheduler {

    @Autowired
    private ClientCommunicationService clientCommunicationService;

    @Scheduled(fixedDelay = 40)
    public void reverseConnection() {
        clientCommunicationService.longPolling();
    }
}
