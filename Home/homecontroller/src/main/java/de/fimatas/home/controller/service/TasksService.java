package de.fimatas.home.controller.service;

import de.fimatas.home.controller.dao.StateHandlerDAO;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.model.Task;
import de.fimatas.home.library.model.TasksModel;
import de.fimatas.home.library.util.HomeAppConstants;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

@Component
@CommonsLog
public class TasksService {

    @Autowired
    private UploadService uploadService;

    @Autowired
    private StateHandlerDAO stateHandlerDAO;

    @Autowired
    private Environment env;

    private final String STATEHANDLER_GROUPNAME_TASKS = "tasks";

    @Scheduled(initialDelay = 4 * 1000, fixedDelay = (1000 * HomeAppConstants.MODEL_TASKS_INTERVAL_SECONDS) + 400)
    private void scheduledRefresh() {
        refresh();
    }

    public void refresh() {

        var idList = Objects.requireNonNull(env.getProperty("tasks.id.list.active"));
        var ids = Arrays.stream(StringUtils.split(idList, ',')).map(String::trim).toList();

        var tasks = new TasksModel();
        ids.forEach(id -> {
            var task = new Task();
            task.setId(id);
            task.setName(Objects.requireNonNull(env.getProperty(String.format("tasks.%s.name", id))));
            task.setManual(Boolean.parseBoolean(Objects.requireNonNull(env.getProperty(String.format("tasks.%s.manual", id)))));
            task.setDuration(Duration.parse(Objects.requireNonNull(env.getProperty(String.format("tasks.%s.duration", id)))));
            //task.setLastExecutionTime();
            //task.setInRange();
            //task.setRangeDistanceTime();
            tasks.getTasks().add(task);
        });

        ModelObjectDAO.getInstance().write(tasks);
        uploadService.uploadToClient(tasks);
    }

    /*public void update(...){
        stateHandlerDAO.writeState(STATEHANDLER_GROUPNAME_TASKS, ...);
        refresh();
    }*/

}
