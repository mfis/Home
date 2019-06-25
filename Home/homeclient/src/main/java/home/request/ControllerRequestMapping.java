package home.request;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import homecontroller.domain.model.ActionModel;
import homecontroller.domain.model.CameraModel;
import homelibrary.dao.ModelObjectDAO;

@RestController
public class ControllerRequestMapping {

	@RequestMapping(value = "/uploadCameraModel")
	public ActionModel uploadCameraModel(@RequestBody CameraModel cameraModel) {
		ModelObjectDAO.getInstance().write(cameraModel);
		return new ActionModel("OK");
	}

}