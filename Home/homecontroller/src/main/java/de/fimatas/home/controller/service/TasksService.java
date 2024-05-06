package de.fimatas.home.controller.service;

import de.fimatas.home.controller.dao.StateHandlerDAO;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.AbstractDeviceModel;
import de.fimatas.home.library.domain.model.WindowSensor;
import de.fimatas.home.library.model.Task;
import de.fimatas.home.library.model.TaskState;
import de.fimatas.home.library.model.TasksModel;
import de.fimatas.home.library.util.HomeAppConstants;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    @Scheduled(initialDelay = 8 * 1000, fixedDelay = (1000 * HomeAppConstants.MODEL_TASKS_INTERVAL_SECONDS) + 400)
    private void scheduledRefresh() {
        refresh();
    }

    public void refresh() {

        if(ModelObjectDAO.getInstance().readHouseModel() == null) {
            return;
        }

        var idList = Objects.requireNonNull(env.getProperty("tasks.id.list.active"));
        var ids = Arrays.stream(StringUtils.split(idList, ',')).map(String::trim).toList();

        var tasks = new TasksModel();
        ids.forEach(id -> {
            var task = new Task();
            task.setId(id);
            task.setName(Objects.requireNonNull(env.getProperty(String.format("tasks.%s.name", id))));
            task.setManual(Boolean.parseBoolean(Objects.requireNonNull(env.getProperty(String.format("tasks.%s.manual", id)))));
            task.setDuration(Duration.parse(Objects.requireNonNull(env.getProperty(String.format("tasks.%s.duration", id)))));
            task.setLastExecutionTime(task.isManual() ? readLastExecutionTimestampFromDatabase(id) : readLastExecutionTimestampFromDevice(id));
            task.setRangeDistanceTime(computeDurationFrom(task.getLastExecutionTime()));
            task.setState(computeTaskState(task.getRangeDistanceTime()));
            tasks.getTasks().add(task);
        });

        ModelObjectDAO.getInstance().write(tasks);
        uploadService.uploadToClient(tasks);
    }

    private TaskState computeTaskState(Duration distance){
        if(distance == null){
            return TaskState.UNKNOWN;
        }
        var now = LocalDateTime.now();
        var next = now.plus(distance);
        var nearby = distance.dividedBy(10);
        if(distance.isNegative()){
            // execution in the future
            if(now.plus(nearby).isAfter(next)){
                return TaskState.NEAR_BEFORE_EXECUTION;
            }
            return TaskState.IN_RANGE;
        } else {
            // next execution in past arrived
          if(now.plus(nearby).isBefore(next)){
              return TaskState.LITTLE_OUT_OF_RANGE;
          }
          return TaskState.FAR_OUT_OF_RANGE;
        }
    }

    private Duration computeDurationFrom(Long millisLastExecution){
        if(millisLastExecution == null) {
            return null;
        }
        var from = Instant.ofEpochMilli(millisLastExecution).atZone(ZoneId.systemDefault()).toLocalDateTime();
        return Duration.between(from, LocalDateTime.now());
    }

    private Long readLastExecutionTimestampFromDatabase(String id){
        var ts = stateHandlerDAO.readState(STATEHANDLER_GROUPNAME_TASKS, id);
        return ts != null ? Long.parseLong(ts.getValue()) : null;
    }

    private Long readLastExecutionTimestampFromDevice(String id){
        var fieldname = Objects.requireNonNull(env.getProperty(String.format("tasks.%s.field", id)));
        final var abstractDeviceModel = ModelObjectDAO.getInstance().readHouseModel().lookupField(fieldname, AbstractDeviceModel.class);
        if (abstractDeviceModel instanceof WindowSensor){
            return ((WindowSensor)abstractDeviceModel).getStateTimestamp();
        }
        throw new IllegalArgumentException("unsupported device model type: " + abstractDeviceModel.getClass().getName());
    }

    public void markAsExecuted(String id){
        stateHandlerDAO.writeState(STATEHANDLER_GROUPNAME_TASKS, id, Long.toString(System.currentTimeMillis()));
        refresh();
    }

}
