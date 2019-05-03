package homecontroller.request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;

import javax.servlet.http.HttpServletResponse;

import org.h2.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import homecontroller.dao.ModelObjectDAO;
import homecontroller.domain.model.ActionModel;
import homecontroller.domain.model.AutomationState;
import homecontroller.domain.model.CameraMode;
import homecontroller.domain.model.Device;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.SettingsModel;
import homecontroller.domain.service.HouseService;
import homecontroller.service.CameraService;
import homecontroller.service.SettingsService;

@RestController
public class HomeClientRequestMapping {

	@Autowired
	private HouseService houseService;

	@Autowired
	private CameraService cameraService;

	@Autowired
	private SettingsService settingsService;

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

	@PostMapping("/controller/actualstate")
	public HouseModel actualstate() {
		return ModelObjectDAO.getInstance().readHouseModel();
	}

	@PostMapping("/controller/cameraPicture")
	public void cameraPicture(HttpServletResponse response, @RequestParam("deviceName") String deviceName,
			@RequestParam("cameraMode") String cameraMode) throws IOException {
		byte[] bytes = cameraService.readCameraPicture(Device.valueOf(deviceName),
				CameraMode.valueOf(cameraMode));
		response.setContentType(MediaType.IMAGE_JPEG_VALUE);
		IOUtils.copy(new ByteArrayInputStream(bytes), response.getOutputStream());
	}

	@PostMapping("/controller/history")
	public HistoryModel history() {
		return ModelObjectDAO.getInstance().readHistoryModel();
	}

	@PostMapping("/controller/settings")
	public SettingsModel settings(@RequestParam("user") String user) {
		return settingsService.read(user);
	}

	@PostMapping("/controller/settingspushtoggle")
	public ActionModel settingspushtoggle(@RequestParam("user") String user) {
		settingsService.togglePush(user);
		return new ActionModel("OK");
	}

	@PostMapping("/controller/settingpushoverdevice")
	public ActionModel settingspushover(@RequestParam("user") String user,
			@RequestParam("device") String device) {
		settingsService.setupPushDevice(user, device);
		return new ActionModel("OK");
	}
}