package de.fimatas.home.client.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class AppPushSettingsModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private String key;

    private String text;

    private boolean value;
}
