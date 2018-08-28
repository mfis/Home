package home.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import home.domain.HistoryModel;
import home.domain.HouseModel;
import home.domain.HouseView;

@Controller
public class HomeRequestMapping {

	@Value("${spring.application.name}")
	String appName;

	@Autowired
	private Environment env;

	@Autowired
	private HouseView houseView;

	@RequestMapping("/toggle")
	public String toggle(@RequestParam("key") String key) throws Exception {
		call(env.getProperty("controller.url") + "toggle?key=" + key);
		return "redirect:/";
	}

	@RequestMapping("/")
	public String homePage(Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		HouseModel house = callForObject(env.getProperty("controller.url") + "actualstate", HouseModel.class);
		houseView.fillViewModel(model, house);
		return "home";
	}

	@RequestMapping("/history")
	public String history(Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		HistoryModel history = callForObject(env.getProperty("controller.url") + "history", HistoryModel.class);
		houseView.fillHistoryViewModel(model, history);
		return "history";
	}

	private <T> T callForObject(String url, Class<T> clazz) {

		ResponseEntity<String> responseEntity = call(url);
		try {
			return new ObjectMapper().readValue(responseEntity.getBody(), clazz);
		} catch (Exception e) {
			throw new RuntimeException("Could not parse JSON file", e);
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