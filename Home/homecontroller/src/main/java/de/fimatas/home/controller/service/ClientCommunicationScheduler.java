package de.fimatas.home.controller.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ClientCommunicationScheduler {

    @Autowired
    private ClientCommunicationService clientCommunicationService;

    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Scheduled(fixedDelay = 40)
    public void reverseConnection() {
        clientCommunicationService.longPolling();
    }

    @PostConstruct
    public void init() {
        threadPoolTaskScheduler
                .schedule(() -> clientCommunicationService.refreshAll(), Instant.now().plusSeconds(20));
    }
}
