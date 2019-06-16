package home.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import home.domain.model.Pages;
import home.domain.service.HouseViewService;
import home.service.ControllerAPI;
import home.service.ExternalPropertiesDAO;
import home.service.LoginInterceptor;
import home.service.SettingsViewService;
import home.service.TextQueryService;
import home.service.ViewAttributesDAO;
import homecontroller.domain.model.AutomationState;
import homecontroller.domain.model.CameraMode;
import homecontroller.domain.model.Device;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.SettingsModel;

@Controller
public class HomeRequestMapping {

	private static final String Y_POS = "y";

	private static final String REDIRECT = "redirect:";

	private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter
			.ofPattern("E, dd. MMM yyyy, HH:mm");

	@Autowired
	private HouseViewService houseView;

	@Autowired
	private SettingsViewService settingsView;

	@Autowired
	private TextQueryService textQueryService;

	@Autowired
	private ControllerAPI controllerAPI;

	@RequestMapping("/togglestate")
	public String togglestate(@CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie,
			@RequestParam(ControllerAPI.DEVICE_NAME) String deviceName,
			@RequestParam(ControllerAPI.BOOLEAN_VALUE) String booleanValue,
			@RequestParam(name = Y_POS, required = false) String y) {
		saveYPos(userCookie, y);
		controllerAPI.togglestate(Device.valueOf(deviceName), Boolean.valueOf(booleanValue));
		return REDIRECT + Pages.PATH_HOME;
	}

	@RequestMapping("/toggleautomation")
	public String toggleautomation(@CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie,
			@RequestParam(ControllerAPI.DEVICE_NAME) String deviceName,
			@RequestParam("automationStateValue") String automationStateValue,
			@RequestParam(name = Y_POS, required = false) String y) {
		saveYPos(userCookie, y);
		controllerAPI.toggleautomation(Device.valueOf(deviceName),
				AutomationState.valueOf(automationStateValue));
		return REDIRECT + Pages.PATH_HOME;
	}

	@RequestMapping("/heatingboost")
	public String heatingBoost(@CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie,
			@RequestParam(ControllerAPI.DEVICE_NAME) String deviceName,
			@RequestParam(name = Y_POS, required = false) String y) {
		saveYPos(userCookie, y);
		controllerAPI.heatingboost(Device.valueOf(deviceName));
		return REDIRECT + Pages.PATH_HOME;
	}

	@RequestMapping("/heatingmanual")
	public String heatingManual(@CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie,
			@RequestParam(ControllerAPI.DEVICE_NAME) String deviceName,
			@RequestParam("temperature") String temperature,
			@RequestParam(name = Y_POS, required = false) String y) {
		saveYPos(userCookie, y);
		controllerAPI.heatingmanual(Device.valueOf(deviceName), new BigDecimal(temperature));
		return REDIRECT + Pages.PATH_HOME;
	}

	@RequestMapping("/shutterSetPosition")
	public String shutterSetPosition(@CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie,
			@RequestParam(ControllerAPI.DEVICE_NAME) String deviceName,
			@RequestParam("positionPercentage") String positionPercentage,
			@RequestParam(name = Y_POS, required = false) String y) {
		saveYPos(userCookie, y);
		controllerAPI.shuttersetposition(Device.valueOf(deviceName), Integer.parseInt(positionPercentage));
		return REDIRECT + Pages.PATH_HOME;
	}

	@RequestMapping("/settingspushtoggle")
	public String settingspushtoggle(@CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie) {
		controllerAPI.settingspushtoggle(userCookie);
		return REDIRECT + Pages.PATH_SETTINGS;
	}

	@RequestMapping("/settingpushoverdevice")
	public String settingspushover(@CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie,
			@RequestParam("pushoverDevice") String pushoverDevice) {
		controllerAPI.settingspushover(userCookie, pushoverDevice);
		return REDIRECT + Pages.PATH_SETTINGS;
	}

	@RequestMapping("/textquery")
	public String textquery(Model model, @RequestParam("text") String text, @RequestParam("user") String user,
			@RequestParam("pass") String pass) {
		HouseModel house = controllerAPI.actualstate();
		model.addAttribute("responsetext", textQueryService.execute(house, text));
		return "textquery";
	}

	@RequestMapping(value = "/cameraPicture", produces = "image/jpeg")
	public ResponseEntity<byte[]> cameraPicture(@RequestParam(ControllerAPI.DEVICE_NAME) String deviceName,
			@RequestParam(ControllerAPI.CAMERA_MODE) String cameraMode,
			@RequestParam("ts") String timestamp) {

		byte[] bytes = controllerAPI.cameraPicture(Device.valueOf(deviceName), CameraMode.valueOf(cameraMode),
				Long.parseLong(timestamp));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("image", "jpeg"));
		headers.setContentDispositionFormData("attachment", deviceName + "_" + cameraMode + ".jpg");
		headers.setContentLength(bytes.length);
		if (bytes.length == 0) {
			return new ResponseEntity<byte[]>(bytes, headers, HttpStatus.NO_CONTENT);
		} else {
			return new ResponseEntity<byte[]>(bytes, headers, HttpStatus.NOT_FOUND);
		}
	}

	@RequestMapping(value = "/cameraPictureRequest")
	public ResponseEntity<String> cameraPictureRequest(
			@RequestParam(ControllerAPI.DEVICE_NAME) String deviceName) {

		String requestTimestamp = controllerAPI.cameraPictureRequest(Device.valueOf(deviceName));
		return new ResponseEntity<>(requestTimestamp, HttpStatus.OK);
	}

	@RequestMapping(Pages.PATH_HOME)
	public String homePage(Model model,
			@CookieValue(name = LoginInterceptor.COOKIE_NAME, required = false) String userCookie) {
		fillMenu(Pages.PATH_HOME, model);
		fillUserAttributes(model, userCookie, ViewAttributesDAO.Y_POS_HOME);
		HouseModel house = controllerAPI.actualstate();
		houseView.fillViewModel(model, house);
		return Pages.getEntry(Pages.PATH_HOME).getTemplate();
	}

	@RequestMapping(Pages.PATH_HISTORY)
	public String history(Model model, @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie) {
		fillMenu(Pages.PATH_HISTORY, model);
		fillUserAttributes(model, userCookie, null);
		HistoryModel history = controllerAPI.history();
		houseView.fillHistoryViewModel(model, history);
		return Pages.getEntry(Pages.PATH_HISTORY).getTemplate();
	}

	@RequestMapping(Pages.PATH_LINKS)
	public String links(Model model, @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie) {
		fillMenu(Pages.PATH_LINKS, model);
		fillUserAttributes(model, userCookie, null);
		houseView.fillLinks(model);
		return Pages.getEntry(Pages.PATH_LINKS).getTemplate();
	}

	@RequestMapping(Pages.PATH_SETTINGS)
	public String settings(Model model, @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie) {
		fillMenu(Pages.PATH_SETTINGS, model);
		fillUserAttributes(model, userCookie, null);
		SettingsModel settings = controllerAPI.settings(userCookie);
		settingsView.fillSettings(model, settings);
		return Pages.getEntry(Pages.PATH_SETTINGS).getTemplate();
	}

	private void saveYPos(String userCookie, String y) {
		if (y != null && userCookie != null) {
			String user = ExternalPropertiesDAO.getInstance().read(userCookie);
			ViewAttributesDAO.getInstance().push(user, ViewAttributesDAO.Y_POS_HOME, y);
		}
	}

	private void fillUserAttributes(Model model, String userCookie, String yPosAttribute) {
		String user = ExternalPropertiesDAO.getInstance().read(userCookie);
		if (user != null) {
			model.addAttribute(ViewAttributesDAO.USER_NAME, user);
			if (yPosAttribute != null) {
				String y = ViewAttributesDAO.getInstance().pull(user, yPosAttribute);
				model.addAttribute("yPos", StringUtils.trimToEmpty(y));
			}
		}
	}

	private void fillMenu(String pathHome, Model model) {
		model.addAttribute("MENU_SELECTED", Pages.getEntry(pathHome));
		model.addAttribute("MENU_SELECTABLE", Pages.getOtherEntries(pathHome));
		model.addAttribute("SITE_REQUEST_TS", "Stand: " + TS_FORMATTER.format(LocalDateTime.now()));
	}

}