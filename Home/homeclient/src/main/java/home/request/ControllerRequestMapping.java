package home.request;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

	@Autowired
	private Environment env;

	@RequestMapping(value = "/uploadCameraModel")
	public ActionModel uploadCameraModel(@RequestBody CameraModel cameraModel) {
		ModelObjectDAO.getInstance().write(cameraModel);
		return new ActionModel("OK");
	}

	@RequestMapping(value = "/uploadHouseModel")
	public ActionModel uploadHouseModel(@RequestBody HouseModel houseModel) {
		ModelObjectDAO.getInstance().write(houseModel);
		return new ActionModel("OK");
	}

	@RequestMapping(value = "/uploadHistoryModel")
	public ActionModel uploadHistoryModel(@RequestBody HistoryModel historyModel) {
		ModelObjectDAO.getInstance().write(historyModel);
		return new ActionModel("OK");
	}

	@RequestMapping(value = "/uploadSettingsModel")
	public ActionModel uploadSettingsModel(@RequestBody SettingsModel settingsModel) {
		ModelObjectDAO.getInstance().write(settingsModel);
		return new ActionModel("OK");
	}

	@RequestMapping(value = "/controllerLongPollingForAwaitMessageRequest")
	public DeferredResult<Message> controllerLongPollingForAwaitMessageRequest() {
		DeferredResult<Message> deferredResult = new DeferredResult<>(Long.MAX_VALUE, null);
		CompletableFuture.runAsync(() -> {
			deferredResult.setResult(MessageQueue.getInstance().pollMessage());
		});
		return deferredResult;
	}

	@RequestMapping(value = "/uploadMessage")
	public ActionModel controllerLongPollingForAsyncResponse(@RequestBody Message response) {
		MessageQueue.getInstance().addResponse(response);
		return new ActionModel("OK");
	}

	@RequestMapping(value = "/uploadBackupFile")
	public ActionModel uploadBackupFile(@RequestBody BackupFile backupFile) throws IOException {
		String path = env.getProperty("backup.location");
		if (!path.endsWith("/")) {
			path = path + "/";
		}
		String absFilePath = path + backupFile.getFilename();
		FileUtils.writeByteArrayToFile(new File(absFilePath), backupFile.getBytes());
		return new ActionModel("OK");
	}
}