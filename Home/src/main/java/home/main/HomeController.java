package home.main;

import java.util.ArrayList;
import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import domain.HouseService;

@Controller
public class HomeController {

	@Value("${spring.application.name}")
	String appName;

	@Autowired
	private Environment env;

	private static HomematicAPI api;

	private static HouseService houseService;

	@PostConstruct
	public void init() {
		String hmHost = env.getProperty("homematic.hostName");
		String hmDevPrefixes = env.getProperty("homematic.devicePrefixes");
		api = new HomematicAPI(hmHost, new ArrayList<String>(Arrays.asList(hmDevPrefixes.split(","))));
		houseService = new HouseService(api);
	}

	@RequestMapping("/toggle")
	public String toggle(@RequestParam("key") String key) throws Exception {
		houseService.toggle(key);
		return "redirect:/";
	}

	@RequestMapping("/")
	public String homePage(Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		houseService.refreshModel();
		houseService.calculateConclusion();
		houseService.fillViewModel(model);

		return "home";
	}

}