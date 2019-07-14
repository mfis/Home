package homecontroller.request;

import java.util.concurrent.CompletableFuture;

import org.apache.commons.logging.LogFactory;
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
		LogFactory.getLog(CCURequestMapping.class).warn("REFRESH " + notifyString); // FIXME:
		CompletableFuture.runAsync(() -> houseService.refreshHouseModel(Boolean.parseBoolean(notifyString)));
		return new ActionModel("OK");
	}

}