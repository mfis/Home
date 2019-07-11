package home.request;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import homecontroller.domain.model.ActionModel;
import homecontroller.domain.model.CameraModel;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.HouseModel;
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
}