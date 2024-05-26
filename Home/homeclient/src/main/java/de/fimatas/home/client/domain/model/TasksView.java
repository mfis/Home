package de.fimatas.home.client.domain.model;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class TasksView extends View {

    private List<TaskView> list = new LinkedList<>();
}
