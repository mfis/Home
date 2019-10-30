package homecontroller.request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import homecontroller.domain.model.ActionModel;
import homecontroller.domain.service.HouseService;

@RestController
public class CCURequestMapping {

	@Autowired
	private HouseService houseService;

	private static final Log LOG = LogFactory.getLog(CCURequestMapping.class);

	@GetMapping("/controller/refresh")
	public ActionModel refresh() {
		LOG.info("REFRESH request");
		houseService.refreshHouseModel();
		return new ActionModel("OK");
	}

}