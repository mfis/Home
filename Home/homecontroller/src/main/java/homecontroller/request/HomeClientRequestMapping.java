package homecontroller.request;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import homecontroller.domain.model.ActionModel;
import homecontroller.domain.model.AutomationState;
import homecontroller.domain.model.Device;
import homecontroller.domain.service.HouseService;
import homecontroller.service.CameraService;

@RestController
public class HomeClientRequestMapping {

	@Autowired
	private HouseService houseService;

	@Autowired
	private CameraService cameraService;

	@PostMapping("/controller/togglestate")
	public ActionModel togglestate(@RequestParam("deviceName") String deviceName,
			@RequestParam("booleanValue") String booleanValue) {
		houseService.togglestate(Device.valueOf(deviceName), Boolean.valueOf(booleanValue));
		return new ActionModel("OK");
	}

	@PostMapping("/controller/toggleautomation")
	public ActionModel toggleautomation(@RequestParam("deviceName") String deviceName,
			@RequestParam("automationStateValue") String automationStateValue) {
		houseService.toggleautomation(Device.valueOf(deviceName),
				AutomationState.valueOf(automationStateValue));
		return new ActionModel("OK");
	}

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