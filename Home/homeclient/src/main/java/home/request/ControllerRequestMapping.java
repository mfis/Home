package home.request;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import home.model.Message;
import home.model.MessageQueue;
import homecontroller.domain.model.ActionModel;
import homecontroller.domain.model.BackupFile;
import homecontroller.domain.model.CameraModel;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.SettingsModel;
import homelibrary.dao.ModelObjectDAO;

@RestController
public class ControllerRequestMapping {

	public static final String CONTROLLER_LONG_POLLING_FOR_AWAIT_MESSAGE_REQUEST = "/controllerLongPollingForAwaitMessageRequest";

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
		DeferredResult<Message> deferredResult = new DeferredResult<>(Long.MAX_VALUE, null);
		CompletableFuture.runAsync(() -> {
			deferredResult.setResult(MessageQueue.getInstance().pollMessage());
		});
		return deferredResult;
	}
}