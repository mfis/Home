package de.fimatas.home.client.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TaskView extends View {

    private int progressPercent;

    private boolean manual;

    private String durationInfoText;

    private String resetLink;

    private String colorClassProgressBar;
}
