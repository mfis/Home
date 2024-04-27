package de.fimatas.home.client.request;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.model.PresenceModel;
import de.fimatas.home.library.model.TasksModel;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import de.fimatas.home.client.model.MessageQueue;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.model.Message;
import de.fimatas.home.library.model.SettingsContainer;

@RestController
@CommonsLog
public class ControllerRequestMapping {

    public static final String CONTROLLER_LONG_POLLING_FOR_AWAIT_MESSAGE_REQUEST =
        "/controllerLongPollingForAwaitMessageRequest";

    public static final String UPLOAD_METHOD_PREFIX = "/upload";

    @Autowired
    private Environment env;

    @PostMapping(value = UPLOAD_METHOD_PREFIX + "HouseModel")
    public ActionModel uploadHouseModel(@RequestBody HouseModel houseModel) {
        ModelObjectDAO.getInstance().write(houseModel);
        return new ActionModel("OK");
    }

    @PostMapping(value = UPLOAD_METHOD_PREFIX + "HistoryModel")
    public ActionModel uploadHistoryModel(@RequestBody HistoryModel historyModel) {
        ModelObjectDAO.getInstance().write(historyModel);
        return new ActionModel("OK");
    }

    @PostMapping(value = UPLOAD_METHOD_PREFIX + "LightsModel")
    public ActionModel uploadLightsModel(@RequestBody LightsModel lightsModel) {
        ModelObjectDAO.getInstance().write(lightsModel);
        return new ActionModel("OK");
    }

    @PostMapping(value = UPLOAD_METHOD_PREFIX + "WeatherForecastModel")
    public ActionModel uploadWeatherForecastModel(@RequestBody WeatherForecastModel weatherForecastModel) {
        ModelObjectDAO.getInstance().write(weatherForecastModel);
        return new ActionModel("OK");
    }

    @PostMapping(value = UPLOAD_METHOD_PREFIX + "PresenceModel")
    public ActionModel uploadPresenceModel(@RequestBody PresenceModel presenceModel) {
        ModelObjectDAO.getInstance().write(presenceModel);
        return new ActionModel("OK");
    }

    @PostMapping(value = UPLOAD_METHOD_PREFIX + "HeatpumpModel")
    public ActionModel uploadHeatpumpModel(@RequestBody HeatpumpModel heatpumpModel) {
        //log.info("NEW HEATPUMP MODEL. BUSY=" + heatpumpModel.isBusy() + ", PRESET=" + heatpumpModel.getHeatpumpMap().get(Place.BEDROOM));
        ModelObjectDAO.getInstance().write(heatpumpModel);
        return new ActionModel("OK");
    }

    @PostMapping(value = UPLOAD_METHOD_PREFIX + "ElectricVehicleModel")
    public ActionModel uploadElectricVehicleModel(@RequestBody ElectricVehicleModel electricVehicleModel) {
        ModelObjectDAO.getInstance().write(electricVehicleModel);
        return new ActionModel("OK");
    }

    @PostMapping(value = UPLOAD_METHOD_PREFIX + "PushMessageModel")
    public ActionModel uploadPushMessageModel(@RequestBody PushMessageModel pushMessageModel) {
        ModelObjectDAO.getInstance().write(pushMessageModel);
        return new ActionModel("OK");
    }

    @PostMapping(value = UPLOAD_METHOD_PREFIX + "SettingsContainer")
    public ActionModel uploadSettingsContainer(@RequestBody SettingsContainer settingsContainer) {
        ModelObjectDAO.getInstance().write(settingsContainer);
        return new ActionModel("OK");
    }

    @PostMapping(value = UPLOAD_METHOD_PREFIX + "Message")
    public ActionModel controllerLongPollingForAsyncResponse(@RequestBody Message response) {
        MessageQueue.getInstance().addResponse(response);
        return new ActionModel("OK");
    }

    @PostMapping(value = UPLOAD_METHOD_PREFIX + "TasksModel")
    public ActionModel uploadTasksModel(@RequestBody TasksModel tasksModel) {
        ModelObjectDAO.getInstance().write(tasksModel);
        return new ActionModel("OK");
    }

    @PostMapping(value = UPLOAD_METHOD_PREFIX + "BackupFile")
    public ActionModel uploadBackupFile(@RequestBody BackupFile backupFile) throws IOException {
        String path = env.getProperty("backup.location");
        if (!Objects.requireNonNull(path).endsWith("/")) {
            path = path + "/"; // NOSONAR
        }
        String absFilePath = path + backupFile.getFilename();
        FileUtils.writeByteArrayToFile(new File(absFilePath), backupFile.getBytes());
        return new ActionModel("OK");
    }

    @PostMapping(value = CONTROLLER_LONG_POLLING_FOR_AWAIT_MESSAGE_REQUEST)
    public DeferredResult<Message> controllerLongPollingForAwaitMessageRequest() {
        DeferredResult<Message> deferredResult = new DeferredResult<>(Long.MAX_VALUE, null);
        CompletableFuture.runAsync(() -> deferredResult.setResult(MessageQueue.getInstance().pollMessage()));
        return deferredResult;
    }
}