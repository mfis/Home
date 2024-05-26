package de.fimatas.home.client.domain.model;

import lombok.Data;

@Data
public class TaskView extends View {

    private String id;

    private String name;

    private int progressPercent;

    private boolean manual;

    private String duration;

    private String lastExecutionTime;

    private String nextExecutionTime;

    private String state;

}
