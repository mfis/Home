package de.fimatas.home.library.model;

import de.fimatas.home.library.domain.model.AbstractSystemModel;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@Setter
@Getter
public class SettingsContainer extends AbstractSystemModel {

    private List<SettingsModel> settings = new LinkedList<>();
}
