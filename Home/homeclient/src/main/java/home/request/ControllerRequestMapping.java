package home.request;

import java.util.concurrent.CompletableFuture;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import home.model.Message;
import home.model.MessageQueue;
import homecontroller.domain.model.ActionModel;
import homecontroller.domain.model.CameraModel;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.SettingsModel;
import homelibrary.dao.ModelObjectDAO;

@RestController
public class ControllerRequestMapping {

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

	@RequestMapping(value = "/controllerLongPollingForAwaitRequest")
	public DeferredResult<Message> controllerLongPollingForAwaitRequest() {
		DeferredResult<Message> deferredResult = new DeferredResult<>(Long.MAX_VALUE, null);
		CompletableFuture.runAsync(() -> {
			deferredResult.setResult(MessageQueue.getInstance().pollMessage());
		});
		return deferredResult;
	}

	@RequestMapping(value = "/controllerLongPollingForAsyncResponse")
	public ActionModel controllerLongPollingForAsyncResponse(@RequestBody Message response) {
		MessageQueue.getInstance().addResponse(response);
		return new ActionModel("OK");
	}
}