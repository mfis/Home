package de.fimatas.home.controller.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class LiveActivityModel {

    private String username;

    private String token;

    private String device;

    private int highPriorityCount = 0;
}
