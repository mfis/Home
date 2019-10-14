package home.request;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

import home.domain.service.HouseViewService;
import home.model.Message;
import home.model.MessageQueue;
import home.model.MessageType;
import home.model.Pages;
import home.service.ControllerAPI;
import home.service.ExternalPropertiesDAO;
import home.service.LoginInterceptor;
import home.service.SettingsViewService;
import home.service.TextQueryService;
import home.service.ViewAttributesDAO;
import homecontroller.domain.model.CameraMode;
import homecontroller.domain.model.Device;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.SettingsModel;
import homelibrary.dao.ModelObjectDAO;

@Controller
public class HomeRequestMapping {

	private static final String REDIRECT = "redirect:";

	private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	private static final Log log = LogFactory.getLog(HomeRequestMapping.class);

	@Autowired
	private HouseViewService houseView;

	@Autowired
	private SettingsViewService settingsView;

	@Autowired
	private TextQueryService textQueryService;

	@Autowired
	private ControllerAPI controllerAPI;

	@RequestMapping("/message")
	public String message(Model model, @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie,
			@RequestParam(name = "type") String type,
			@RequestParam(name = "deviceName", required = false) String deviceName,
			@RequestParam("value") String value,
			@RequestParam(name = "securityPin", required = false) String securityPin) {

		MessageType messageType = MessageType.valueOf(type);
		Device device = StringUtils.isBlank(deviceName) ? null : Device.valueOf(deviceName);

		Message message = new Message();
		message.setMessageType(messageType);
		message.setDevice(device);
		message.setValue(value);
		message.setUser(ExternalPropertiesDAO.getInstance().read(userCookie));

		boolean success = MessageQueue.getInstance().request(message, true);
		if (!success) {
			System.out.println("MESSAGE EXECUTION NOT SUCCESSFUL !!!");
			// TODO: Error Popup
		}

		return REDIRECT + messageType.getTargetSite();
	}

	@RequestMapping("/history")
	public String history(Model model, @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie,
			@RequestParam(name = "key") String key) {
		LogFactory.getLog(this.getClass()).info("/history - " + key);
		fillUserAttributes(model, userCookie);
		houseView.fillHistoryViewModel(model, ModelObjectDAO.getInstance().readHistoryModel(),
				ModelObjectDAO.getInstance().readHouseModel(), key);
		LogFactory.getLog(this.getClass()).info("model:" + model.asMap().toString());
		return "history";
	}

	@RequestMapping("/textquery")
	public String textquery(Model model, @RequestParam("text") String text, @RequestParam("user") String user,
			@RequestParam("pass") String pass) {
		model.addAttribute("responsetext",
				textQueryService.execute(ModelObjectDAO.getInstance().readHouseModel(), text));
		return "textquery";
	}

	@RequestMapping(value = "/cameraPicture", produces = "image/jpeg")
	public ResponseEntity<byte[]> cameraPicture(@RequestParam(ControllerAPI.DEVICE_NAME) String deviceName,
			@RequestParam(ControllerAPI.CAMERA_MODE) String cameraMode,
			@RequestParam("ts") String timestamp) {
		log.info("poll for camera image - " + timestamp);
		byte[] bytes = controllerAPI.cameraPicture(Device.valueOf(deviceName), CameraMode.valueOf(cameraMode),
				Long.parseLong(timestamp));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("image", "jpeg"));
		headers.setContentDispositionFormData("attachment", deviceName + "_" + cameraMode + ".jpg");
		headers.setContentLength(bytes.length);
		if (bytes.length == 0) {
			return new ResponseEntity<byte[]>(bytes, headers, HttpStatus.NO_CONTENT);
		} else {
			return new ResponseEntity<byte[]>(bytes, headers, HttpStatus.OK);
		}
	}

	@RequestMapping(value = "/cameraPictureRequest")
	public ResponseEntity<String> cameraPictureRequest(
			@RequestParam(ControllerAPI.DEVICE_NAME) String deviceName) {
		log.info("requesting new camera image " + deviceName);
		String requestTimestamp = controllerAPI.cameraPictureRequest(Device.valueOf(deviceName));
		return new ResponseEntity<>(requestTimestamp, HttpStatus.OK);
	}

	@RequestMapping(Pages.PATH_HOME)
	public String homePage(Model model,
			@CookieValue(name = LoginInterceptor.COOKIE_NAME, required = false) String userCookie) {
		fillMenu(Pages.PATH_HOME, model);
		fillUserAttributes(model, userCookie);
		HouseModel houseModel = ModelObjectDAO.getInstance().readHouseModel();
		if (houseModel == null) {
			return "error";
		} else {
			houseView.fillViewModel(model, ModelObjectDAO.getInstance().readHouseModel());
			return Pages.getEntry(Pages.PATH_HOME).getTemplate();
		}
	}

	@RequestMapping(Pages.PATH_LINKS)
	public String links(Model model, @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie) {
		fillMenu(Pages.PATH_LINKS, model);
		fillUserAttributes(model, userCookie);
		houseView.fillLinks(model);
		return Pages.getEntry(Pages.PATH_LINKS).getTemplate();
	}

	@RequestMapping(Pages.PATH_SETTINGS)
	public String settings(Model model, @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie) {
		fillMenu(Pages.PATH_SETTINGS, model);
		fillUserAttributes(model, userCookie);
		String user = ExternalPropertiesDAO.getInstance().read(userCookie);
		SettingsModel settings = ModelObjectDAO.getInstance().readSettingsModels(user);
		settingsView.fillSettings(model, settings);
		return Pages.getEntry(Pages.PATH_SETTINGS).getTemplate();
	}

	private void fillUserAttributes(Model model, String userCookie) {
		String user = ExternalPropertiesDAO.getInstance().read(userCookie);
		if (user != null) {
			model.addAttribute(ViewAttributesDAO.USER_NAME, user);
		}
	}

	private void fillMenu(String pathHome, Model model) {
		model.addAttribute("MENU_SELECTED", Pages.getEntry(pathHome));
		model.addAttribute("MENU_SELECTABLE", Pages.getOtherEntries(pathHome));
		model.addAttribute("SITE_REQUEST_TS", TS_FORMATTER.format(LocalDateTime.now()));
	}

}