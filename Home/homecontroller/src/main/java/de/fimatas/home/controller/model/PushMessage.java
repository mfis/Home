package de.fimatas.home.controller.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PushMessage {

    private LocalDateTime timestamp;

    private String username;

    private String title;

    private String textMessage;
}
