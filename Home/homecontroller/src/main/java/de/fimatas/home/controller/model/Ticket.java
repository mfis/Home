package de.fimatas.home.controller.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Ticket {

    private String ticket;

    private String event;

    private LocalDateTime timestamp;

    private String value;
}
