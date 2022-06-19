package de.fimatas.home.controller.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class State {

    private String groupname;

    private String statename;

    private LocalDateTime timestamp;

    private String value;
}
