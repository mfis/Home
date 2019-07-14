package homecontroller.request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import homecontroller.domain.model.ActionModel;
import homecontroller.domain.model.Device;
import homecontroller.service.CameraService;

@RestController
public class HomeClientRequestMapping { // FIXME: DELETE

	@Autowired
	private CameraService cameraService;

	@PostMapping("/controller/cameraLivePicture")
	public ActionModel cameraPicture(@RequestParam("deviceName") String deviceName) {
		String requestTimestamp = cameraService.takeLivePicture(Device.valueOf(deviceName));
		return new ActionModel(requestTimestamp);
	}

}