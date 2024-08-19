package de.fimatas.home.library.model;

import de.fimatas.home.library.domain.model.AbstractSystemModel;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class TasksModel extends AbstractSystemModel {

    private List<Task> tasks = new LinkedList<>();
}
