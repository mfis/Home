package de.fimatas.home.client.request;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import de.fimatas.home.client.model.MessageQueue;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.ActionModel;
import de.fimatas.home.library.domain.model.BackupFile;
import de.fimatas.home.library.domain.model.CameraModel;
import de.fimatas.home.library.domain.model.HistoryModel;
import de.fimatas.home.library.domain.model.HouseModel;
import de.fimatas.home.library.domain.model.SettingsModel;
import de.fimatas.home.library.model.Message;

@RestController
public class ControllerRequestMapping {

    public static final String CONTROLLER_LONG_POLLING_FOR_AWAIT_MESSAGE_REQUEST =
        "/controllerLongPollingForAwaitMessageRequest";

    public static final String UPLOAD_METHOD_PREFIX = "/upload";

    @Autowired
    private Environment env;

    @PostMapping(value = UPLOAD_METHOD_PREFIX + "CameraModel")
    public ActionModel uploadCameraModel(@RequestBody CameraModel cameraModel) {
        ModelObjectDAO.getInstance().write(cameraModel);
        return new ActionModel("OK");
    }

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

    @PostMapping(value = UPLOAD_METHOD_PREFIX + "SettingsModel")
    public ActionModel uploadSettingsModel(@RequestBody SettingsModel settingsModel) {
        ModelObjectDAO.getInstance().write(settingsModel);
        return new ActionModel("OK");
    }

    @PostMapping(value = UPLOAD_METHOD_PREFIX + "Message")
    public ActionModel controllerLongPollingForAsyncResponse(@RequestBody Message response) {
        MessageQueue.getInstance().addResponse(response);
        return new ActionModel("OK");
    }

    @PostMapping(value = UPLOAD_METHOD_PREFIX + "BackupFile")
    public ActionModel uploadBackupFile(@RequestBody BackupFile backupFile) throws IOException {
        String path = env.getProperty("backup.location");
        if (!path.endsWith("/")) {
            path = path + "/"; // NOSONAR
        }
        String absFilePath = path + backupFile.getFilename();
        FileUtils.writeByteArrayToFile(new File(absFilePath), backupFile.getBytes());
        return new ActionModel("OK");
    }

    @PostMapping(value = CONTROLLER_LONG_POLLING_FOR_AWAIT_MESSAGE_REQUEST)
    public DeferredResult<Message> controllerLongPollingForAwaitMessageRequest() {
        ModelObjectDAO.getInstance().setLastLongPollingTimestamp(new Date().getTime());
        DeferredResult<Message> deferredResult = new DeferredResult<>(Long.MAX_VALUE, null);
        CompletableFuture.runAsync(() -> deferredResult.setResult(MessageQueue.getInstance().pollMessage()));
        return deferredResult;
    }
}