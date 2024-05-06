package de.fimatas.home.library.model;

import lombok.Data;

import java.time.Duration;

@Data
public class Task {

    private String id;

    private String name;

    private boolean manual;

    private Duration duration;

    private Long lastExecutionTime;

    private TaskState state;

    private Duration rangeDistanceTime;
}
