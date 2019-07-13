package homecontroller.request;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import homecontroller.domain.model.ActionModel;
import homecontroller.domain.model.Device;
import homecontroller.domain.service.HouseService;
import homecontroller.service.CameraService;

@RestController
public class HomeClientRequestMapping {

	@Autowired
	private HouseService houseService;

	@Autowired
	private CameraService cameraService;

	@PostMapping("/controller/heatingboost")
	public ActionModel heatingBoost(@RequestParam("deviceName") String deviceName)
			throws InterruptedException {
		houseService.heatingBoost(Device.valueOf(deviceName));
		return new ActionModel("OK");
	}

	@PostMapping("/controller/heatingmanual")
	public ActionModel heatingManual(@RequestParam("deviceName") String deviceName,
			@RequestParam("temperature") String temperature) throws InterruptedException {
		houseService.heatingManual(Device.valueOf(deviceName), new BigDecimal(temperature));
		return new ActionModel("OK");
	}

	@PostMapping("/controller/cameraLivePicture")
	public ActionModel cameraPicture(@RequestParam("deviceName") String deviceName) {
		String requestTimestamp = cameraService.takeLivePicture(Device.valueOf(deviceName));
		return new ActionModel(requestTimestamp);
	}

}