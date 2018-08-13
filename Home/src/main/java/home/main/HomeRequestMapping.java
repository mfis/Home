package home.main;

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

import domain.HouseModel;
import domain.HouseView;

@Controller
public class HomeRequestMapping {

	@Value("${spring.application.name}")
	String appName;

	@Autowired
	private Environment env;

	@RequestMapping("/toggle")
	public String toggle(@RequestParam("key") String key) throws Exception {
		callController(env.getProperty("controllerURL") + "toggle?key=" + key);
		return "redirect:/";
	}

	@RequestMapping("/")
	public String homePage(Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		HouseModel house = callController(env.getProperty("controllerURL"));
		new HouseView().fillViewModel(model, house);
		return "home";
	}

	private HouseModel callController(String url) {

		RestTemplate rest = new RestTemplate();
		HttpHeaders headers = createHeaders();

		HttpEntity<String> requestEntity = new HttpEntity<String>("", headers);
		ResponseEntity<String> responseEntity = rest.exchange(url, HttpMethod.GET, requestEntity, String.class);

		String response = responseEntity.getBody();

		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(response, HouseModel.class);
		} catch (Exception e) {
			throw new RuntimeException("Could not parse JSON file");
		}

	}

	HttpHeaders createHeaders() {

		return new HttpHeaders() {
			private static final long serialVersionUID = 1L;
			{
				String plainClientCredentials = ExternalPropertiesDAO.getInstance().read("xmlapi.auth.user") + ":" + ExternalPropertiesDAO.getInstance().read("xmlapi.auth.pass");
				String base64ClientCredentials = new String(Base64.encodeBase64(plainClientCredentials.getBytes()));
				set("Authorization", "Basic " + base64ClientCredentials);
				set("Accept", "*/*");
				set("Cache-Control", "no-cache");
			}
		};
	}

}