package de.fimatas.home.client.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class AppPushSettingsModels implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<AppPushSettingsModel> settings;

    private List<AppAttributeModel> attributes;
}
