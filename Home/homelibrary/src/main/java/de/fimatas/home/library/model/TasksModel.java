package de.fimatas.home.library.model;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class TasksModel {

    private long dateTime;

    private List<Task> tasks = new LinkedList<>();
}
