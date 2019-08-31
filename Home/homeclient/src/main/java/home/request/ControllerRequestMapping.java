package home.request;

import java.util.concurrent.CompletableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

	private static final Log log = LogFactory.getLog(ControllerRequestMapping.class);

	@RequestMapping(value = "/uploadCameraModel")
	public ActionModel uploadCameraModel(@RequestBody CameraModel cameraModel) {
		log.info("recieved new camera image upload");
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
}