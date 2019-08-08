package homecontroller.request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import homecontroller.domain.model.ActionModel;
import homecontroller.domain.service.HouseService;

@RestController
public class CCURequestMapping {

	@Autowired
	private HouseService houseService;

	@GetMapping("/controller/refresh")
	public ActionModel refresh(@RequestParam("notify") String notifyString) {
		if (Boolean.parseBoolean(notifyString)) {
			houseService.notifyAboutCcuProgramCompletion();
		} else {
			houseService.refreshHouseModel();
		}
		return new ActionModel("OK");
	}

}