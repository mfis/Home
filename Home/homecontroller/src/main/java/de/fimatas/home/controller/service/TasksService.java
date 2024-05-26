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
import java.time.format.DateTimeFormatter;
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
    private UniqueTimestampService uniqueTimestampService;

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
            task.setNextExecutionTime(task.getLastExecutionTime() != null ? task.getLastExecutionTime().plus(task.getDuration()) : null);
            task.setState(computeTaskState(task.getDuration(), task.getLastExecutionTime()));
            task.setDurationPercentage(computePercentage(task.getDuration(), task.getLastExecutionTime()));
            tasks.getTasks().add(task);
        });

        ModelObjectDAO.getInstance().write(tasks);
        uploadService.uploadToClient(tasks);
    }

    protected int computePercentage(Duration duration, LocalDateTime lastExecutionTime) {
        if(lastExecutionTime == null) {
            return 100;
        }
        var pastDuration = Duration.between(lastExecutionTime, uniqueTimestampService.getNonUnique());
        var computed = (int) ((100 * pastDuration.toMinutes()) / duration.toMinutes());
        return Math.min(computed, 100);
    }

    protected TaskState computeTaskState(Duration durationComplete, LocalDateTime nextExecutionTime){
        if(nextExecutionTime == null){
            return TaskState.UNKNOWN;
        }
        var durationNowToNextExecution = Duration.between(uniqueTimestampService.getNonUnique(), nextExecutionTime);
        var tenPercentOfDurationComplete = durationComplete.dividedBy(10);
        if(durationNowToNextExecution.isNegative()){
            // next execution in past arrived
            if(durationNowToNextExecution.abs().compareTo(tenPercentOfDurationComplete) < 0){
                return TaskState.LITTLE_OUT_OF_RANGE;
            }
            return TaskState.FAR_OUT_OF_RANGE;
        } else {
            // execution in the future
            if(durationNowToNextExecution.compareTo(tenPercentOfDurationComplete) < 0){
                return TaskState.NEAR_BEFORE_EXECUTION;
            }
            return TaskState.IN_RANGE;
        }
    }

    private LocalDateTime readLastExecutionTimestampFromDatabase(String id){
        var ts = stateHandlerDAO.readState(STATEHANDLER_GROUPNAME_TASKS, id);
        return ts != null ? LocalDateTime.parse(ts.getValue(), DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    private LocalDateTime readLastExecutionTimestampFromDevice(String id){
        var fieldname = Objects.requireNonNull(env.getProperty(String.format("tasks.%s.field", id)));
        final var abstractDeviceModel = ModelObjectDAO.getInstance().readHouseModel().lookupField(fieldname, AbstractDeviceModel.class);
        if (abstractDeviceModel instanceof WindowSensor){
            return Instant.ofEpochMilli(((WindowSensor)abstractDeviceModel).getStateTimestamp()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        throw new IllegalArgumentException("unsupported device model type: " + abstractDeviceModel.getClass().getName());
    }

    public void markAsExecuted(String id){
        stateHandlerDAO.writeState(STATEHANDLER_GROUPNAME_TASKS, id, uniqueTimestampService.getNonUnique().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        refresh();
    }

}
