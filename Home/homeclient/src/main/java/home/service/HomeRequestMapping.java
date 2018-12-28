package home.service;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import home.domain.model.Pages;
import home.domain.service.HouseViewService;
import homecontroller.domain.model.ActionModel;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.SettingsModel;
import homecontroller.util.URIParameter;

@Controller
public class HomeRequestMapping {

	private static final String PREFIX = "prefix";

	private static final String CONTROLLER_URL = "controller.url";

	private static final String REDIRECT = "redirect:";

	@Autowired
	private Environment env;

	@Autowired
	private HouseViewService houseView;

	@Autowired
	private SettingsViewService settingsView;

	@Autowired
	private RestTemplate restTemplate;

	@RequestMapping("/toggle")
	public String toggle(@CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie,
			@RequestParam("devIdVar") String devIdVar, @RequestParam(name = "y", required = false) String y) {
		saveYPos(userCookie, y);
		call(env.getProperty(CONTROLLER_URL) + "toggle", ActionModel.class,
				new URIParameter().add("devIdVar", devIdVar).build());
		return REDIRECT + Pages.PATH_HOME;
	}

	@RequestMapping("/heatingboost")
	public String heatingBoost(@CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie,
			@RequestParam(PREFIX) String prefix, @RequestParam(name = "y", required = false) String y) {
		saveYPos(userCookie, y);
		call(env.getProperty(CONTROLLER_URL) + "heatingboost", ActionModel.class,
				new URIParameter().add(PREFIX, prefix).build());
		return REDIRECT + Pages.PATH_HOME;
	}

	@RequestMapping("/heatingmanual")
	public String heatingManual(@CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie,
			@RequestParam(PREFIX) String prefix, @RequestParam("temperature") String temperature,
			@RequestParam(name = "y", required = false) String y) {
		saveYPos(userCookie, y);
		call(env.getProperty(CONTROLLER_URL) + "heatingmanual", ActionModel.class,
				new URIParameter().add(PREFIX, prefix).add("temperature", temperature).build());
		return REDIRECT + Pages.PATH_HOME;
	}

	@RequestMapping("/shutterSetPosition")
	public String shutterSetPosition(@CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie,
			@RequestParam("devIdVar") String devIdVar,
			@RequestParam("positionPercentage") String positionPercentage,
			@RequestParam(name = "y", required = false) String y) {
		saveYPos(userCookie, y);
		call(env.getProperty(CONTROLLER_URL) + "shutterSetPosition", ActionModel.class, new URIParameter()
				.add("devIdVar", devIdVar).add("positionPercentage", positionPercentage).build());
		return REDIRECT + Pages.PATH_HOME;
	}

	@RequestMapping("/settingspushtoggle")
	public String settingspushtoggle(@CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie) {
		call(env.getProperty(CONTROLLER_URL) + "settingspushtoggle", ActionModel.class,
				new URIParameter().add("user", ExternalPropertiesDAO.getInstance().read(userCookie)).build());
		return REDIRECT + Pages.PATH_SETTINGS;
	}

	@RequestMapping("/settingpushoverdevice")
	public String settingspushover(@CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie,
			@RequestParam("pushoverDevice") String pushoverDevice) {
		call(env.getProperty(CONTROLLER_URL) + "settingpushoverdevice", ActionModel.class,
				new URIParameter().add("user", ExternalPropertiesDAO.getInstance().read(userCookie))
						.add("device", pushoverDevice).build());
		return REDIRECT + Pages.PATH_SETTINGS;
	}

	@RequestMapping(Pages.PATH_HOME)
	public String homePage(Model model,
			@CookieValue(name = LoginInterceptor.COOKIE_NAME, required = false) String userCookie) {
		fillMenu(Pages.PATH_HOME, model);
		fillUserAttributes(model, userCookie, ViewAttributesDAO.Y_POS_HOME);
		HouseModel house = call(env.getProperty(CONTROLLER_URL) + "actualstate", HouseModel.class,
				new URIParameter().build());
		houseView.fillViewModel(model, house);
		return Pages.getEntry(Pages.PATH_HOME).getTemplate();
	}

	@RequestMapping(Pages.PATH_HISTORY)
	public String history(Model model, @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie) {
		fillMenu(Pages.PATH_HISTORY, model);
		fillUserAttributes(model, userCookie, null);
		HistoryModel history = call(env.getProperty(CONTROLLER_URL) + "history", HistoryModel.class,
				new URIParameter().build());
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
		SettingsModel settings = call(env.getProperty(CONTROLLER_URL) + "settings", SettingsModel.class,
				new URIParameter().add("user", ExternalPropertiesDAO.getInstance().read(userCookie)).build());
		settingsView.fillSettings(model, settings);
		return Pages.getEntry(Pages.PATH_SETTINGS).getTemplate();
	}

	private void saveYPos(String userCookie, String y) {
		if (y != null) {
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
	}

	private <T> T call(String url, Class<T> clazz, MultiValueMap<String, String> parameters) {

		try {
			HttpHeaders headers = createHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);
			ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
			return new ObjectMapper().readValue(response.getBody(), clazz);
		} catch (Exception e) {
			LogFactory.getLog(HomeRequestMapping.class).error("Could not call controller!", e);
			return null;
		}
	}

	HttpHeaders createHeaders() {

		String plainClientCredentials = env.getProperty("controller.user") + ":"
				+ env.getProperty("controller.pass");
		String base64ClientCredentials = new String(Base64.encodeBase64(plainClientCredentials.getBytes()));

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Basic " + base64ClientCredentials);
		headers.set("Accept", "*/*");
		headers.set("Cache-Control", "no-cache");
		return headers;
	}

}