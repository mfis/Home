package home.service;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import home.domain.model.Pages;
import home.domain.service.HouseView;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.HouseModel;

@Controller
public class HomeRequestMapping {

	@Autowired
	private Environment env;

	@Autowired
	private HouseView houseView;

	@RequestMapping("/toggle")
	public String toggle(@CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie, @RequestParam("devIdVar") String devIdVar,
			@RequestParam(name = "y", required = false) String y) throws Exception {
		saveYPos(userCookie, y);
		call(env.getProperty("controller.url") + "toggle?devIdVar=" + devIdVar);
		return "redirect:" + Pages.PATH_HOME;
	}

	@RequestMapping("/heatingboost")
	public String heatingBoost(@CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie, @RequestParam("prefix") String prefix,
			@RequestParam(name = "y", required = false) String y) throws Exception {
		saveYPos(userCookie, y);
		call(env.getProperty("controller.url") + "heatingboost?prefix=" + prefix);
		return "redirect:" + Pages.PATH_HOME;
	}

	@RequestMapping("/heatingmanual")
	public String heatingManual(@CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie, @RequestParam("prefix") String prefix,
			@RequestParam("temperature") String temperature, @RequestParam(name = "y", required = false) String y) throws Exception {
		saveYPos(userCookie, y);
		call(env.getProperty("controller.url") + "heatingmanual?prefix=" + prefix + "&temperature=" + temperature);
		return "redirect:" + Pages.PATH_HOME;
	}

	@RequestMapping(Pages.PATH_HOME)
	public String homePage(Model model, @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie) throws Exception {
		fillMenu(Pages.PATH_HOME, model);
		fillUserAttributes(model, userCookie, ViewAttributesDAO.Y_POS_HOME);
		HouseModel house = callForObject(env.getProperty("controller.url") + "actualstate", HouseModel.class);
		houseView.fillViewModel(model, house);
		return Pages.getEntry(Pages.PATH_HOME).getTemplate();
	}

	@RequestMapping(Pages.PATH_HISTORY)
	public String history(Model model, @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie) throws Exception {
		fillMenu(Pages.PATH_HISTORY, model);
		fillUserAttributes(model, userCookie, null);
		HistoryModel history = callForObject(env.getProperty("controller.url") + "history", HistoryModel.class);
		houseView.fillHistoryViewModel(model, history);
		return Pages.getEntry(Pages.PATH_HISTORY).getTemplate();
	}

	@RequestMapping(Pages.PATH_LINKS)
	public String links(Model model, @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie) throws Exception {
		fillMenu(Pages.PATH_LINKS, model);
		fillUserAttributes(model, userCookie, null);
		return Pages.getEntry(Pages.PATH_LINKS).getTemplate();
	}

	private void saveYPos(String userCookie, String y) {
		if (y != null) {
			String user = ExternalPropertiesDAO.getInstance().read(userCookie);
			ViewAttributesDAO.getInstance().push(user, ViewAttributesDAO.Y_POS_HOME, y);
		}
	}

	private void fillUserAttributes(Model model, String userCookie, String yPosAttribute) {
		String user = ExternalPropertiesDAO.getInstance().read(userCookie);
		model.addAttribute(ViewAttributesDAO.USER_NAME, user);
		if (yPosAttribute != null) {
			String y = ViewAttributesDAO.getInstance().pull(user, yPosAttribute);
			model.addAttribute("yPos", StringUtils.trimToEmpty(y));
		}
	}

	private void fillMenu(String pathHome, Model model) {
		model.addAttribute("MENU_SELECTED", Pages.getEntry(pathHome));
		model.addAttribute("MENU_SELECTABLE", Pages.getOtherEntries(pathHome));
	}

	private <T> T callForObject(String url, Class<T> clazz) {

		try {
			ResponseEntity<String> responseEntity = call(url);
			return new ObjectMapper().readValue(responseEntity.getBody(), clazz);
		} catch (Exception e) {
			LogFactory.getLog(HomeRequestMapping.class).error("Could not call controller!", e);
			return null;
		}
	}

	private ResponseEntity<String> call(String url) {

		RestTemplate rest = new RestTemplate();
		HttpHeaders headers = createHeaders();

		HttpEntity<String> requestEntity = new HttpEntity<String>("", headers);
		ResponseEntity<String> responseEntity = rest.exchange(url, HttpMethod.GET, requestEntity, String.class);
		return responseEntity;
	}

	HttpHeaders createHeaders() {

		return new HttpHeaders() {
			private static final long serialVersionUID = 1L;
			{
				String plainClientCredentials = env.getProperty("controller.user") + ":" + env.getProperty("controller.pass");
				String base64ClientCredentials = new String(Base64.encodeBase64(plainClientCredentials.getBytes()));
				set("Authorization", "Basic " + base64ClientCredentials);
				set("Accept", "*/*");
				set("Cache-Control", "no-cache");
			}
		};
	}

}