package de.fimatas.home.client.domain.model;

import lombok.Data;

@Data
public class TaskView extends View {

    private int progressPercent;

    private boolean manual;

    private String durationInfoText;
}
