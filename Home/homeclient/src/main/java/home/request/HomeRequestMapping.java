package home.request;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.servlet.http.HttpServletResponse;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import home.domain.service.HouseViewService;
import home.model.Message;
import home.model.MessageQueue;
import home.model.MessageType;
import home.model.Pages;
import home.service.LoginCookieDAO;
import home.service.LoginInterceptor;
import home.service.SettingsViewService;
import home.service.TextQueryService;
import home.service.ViewAttributesDAO;
import homecontroller.domain.model.CameraMode;
import homecontroller.domain.model.CameraPicture;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.SettingsModel;
import homelibrary.dao.ModelObjectDAO;
import homelibrary.homematic.model.Device;

@Controller
public class HomeRequestMapping {

	private static final String REDIRECT = "redirect:";

	private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	private static final String DEVICE_NAME = "deviceName";

	private static final String CAMERA_MODE = "cameraMode";

	private static final Log log = LogFactory.getLog(HomeRequestMapping.class);

	@Autowired
	private HouseViewService houseView;

	@Autowired
	private SettingsViewService settingsView;

	@Autowired
	private TextQueryService textQueryService;

	@GetMapping("/message")
	public String message(Model model, @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie,
			@RequestParam(name = "type") String type,
			@RequestParam(name = DEVICE_NAME, required = false) String deviceName,
			@RequestParam("value") String value,
			@RequestParam(name = "securityPin", required = false) String securityPin) {

		Message response = request(userCookie, type, deviceName, value);

		if (!response.isSuccessfullExecuted()) {
			log.error("MESSAGE EXECUTION NOT SUCCESSFUL !!!");
		}
		return REDIRECT + response.getMessageType().getTargetSite();
	}

	@GetMapping("/history")
	public String history(Model model, @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie,
			@RequestParam(name = "key") String key) {
		fillUserAttributes(model, userCookie);
		houseView.fillHistoryViewModel(model, ModelObjectDAO.getInstance().readHistoryModel(),
				ModelObjectDAO.getInstance().readHouseModel(), key);
		return "history";
	}

	@PostMapping("/textquery")
	public String textquery(Model model, @RequestParam("text") String text, @RequestParam("user") String user,
			@RequestParam("pass") String pass) {
		model.addAttribute("responsetext",
				textQueryService.execute(ModelObjectDAO.getInstance().readHouseModel(), text));
		return "textquery";
	}

	@GetMapping(value = "/cameraPicture", produces = "image/jpeg")
	public ResponseEntity<byte[]> cameraPicture(@RequestParam(DEVICE_NAME) String deviceName,
			@RequestParam(CAMERA_MODE) String cameraMode, @RequestParam("ts") String timestamp,
			@RequestParam(name = "onlyheader", required = false) String onlyheader) {

		boolean onlyHeaderFlag = onlyheader != null && Boolean.parseBoolean(onlyheader);
		log.info("poll for camera image - " + timestamp + (onlyHeaderFlag ? " onlyHeader" : ""));
		byte[] bytes = cameraPicture(Device.valueOf(deviceName), CameraMode.valueOf(cameraMode),
				Long.parseLong(timestamp));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("image", "jpeg"));
		headers.setContentDispositionFormData("attachment", deviceName + "_" + cameraMode + ".jpg");
		headers.setContentLength(bytes.length);
		if (bytes.length == 0) {
			return new ResponseEntity<>(bytes, headers, HttpStatus.NO_CONTENT);
		} else {
			return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
		}
	}

	@GetMapping(value = "/cameraPictureRequest")
	public ResponseEntity<String> cameraPictureRequest(Model model,
			@CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie,
			@RequestParam(name = "type") String type,
			@RequestParam(name = DEVICE_NAME, required = false) String deviceName,
			@RequestParam("value") String value) {

		log.info("requesting new camera image " + deviceName);

		Message response = request(userCookie, type, deviceName, value);
		return new ResponseEntity<>(response.getResponse(), HttpStatus.OK);
	}

	@RequestMapping(Pages.PATH_HOME) // NOSONAR
	public String homePage(Model model, HttpServletResponse response,
			@CookieValue(name = LoginInterceptor.COOKIE_NAME, required = false) String userCookie,
			@RequestHeader(name = "ETag", required = false) String etag) {
		fillMenu(Pages.PATH_HOME, model);
		fillUserAttributes(model, userCookie);
		HouseModel houseModel = ModelObjectDAO.getInstance().readHouseModel();
		if (houseModel == null) {
			throw new IllegalStateException(
					"State error - " + ModelObjectDAO.getInstance().getLastHouseModelState());
		} else if (StringUtils.isNotBlank(etag)
				&& StringUtils.equals(etag, Long.toString(houseModel.getDateTime()))) {
			response.setStatus(HttpStatus.NOT_MODIFIED.value());
			return "empty";
		} else {
			houseView.fillViewModel(model, houseModel, ModelObjectDAO.getInstance().readHistoryModel());
			return Pages.getEntry(Pages.PATH_HOME).getTemplate();
		}
	}

	@GetMapping(Pages.PATH_SETTINGS)
	public String settings(Model model, @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie) {
		fillMenu(Pages.PATH_SETTINGS, model);
		fillUserAttributes(model, userCookie);
		String user = LoginCookieDAO.getInstance().read(userCookie);
		SettingsModel settings = ModelObjectDAO.getInstance().readSettingsModels(user);
		settingsView.fillSettings(model, settings);
		return Pages.getEntry(Pages.PATH_SETTINGS).getTemplate();
	}

	private Message request(String userCookie, String type, String deviceName, String value) {

		MessageType messageType = MessageType.valueOf(type);
		Device device = StringUtils.isBlank(deviceName) ? null : Device.valueOf(deviceName);

		Message message = new Message();
		message.setMessageType(messageType);
		message.setDevice(device);
		message.setValue(value);
		message.setUser(LoginCookieDAO.getInstance().read(userCookie));

		return MessageQueue.getInstance().request(message, true);
	}

	private byte[] cameraPicture(Device device, CameraMode mode, long timestamp) {
		CameraPicture cameraPicture = ModelObjectDAO.getInstance().readCameraPicture(device, mode, timestamp);
		if (cameraPicture != null) {
			return cameraPicture.getBytes();
		}
		return new byte[0];
	}

	private void fillUserAttributes(Model model, String userCookie) {
		String user = LoginCookieDAO.getInstance().read(userCookie);
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