package de.fimatas.home.controller.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PushToken {

    private String username;

    private String token;
}
