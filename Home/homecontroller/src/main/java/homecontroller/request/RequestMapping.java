package homecontroller.request;

import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import homecontroller.domain.model.ActionModel;
import homecontroller.domain.service.DatabaseMigrationService;
import homecontroller.domain.service.HouseService;

@RestController
public class RequestMapping {

	@Autowired
	private HouseService houseService;

	@Autowired
	private DatabaseMigrationService databaseMigrationService;

	private static final Log LOG = LogFactory.getLog(RequestMapping.class);

	private String uuid = null;

	@PostConstruct
	private void postConstruct() {
		uuid = UUID.randomUUID().toString();
		LOG.info("UUID=" + uuid);
	}

	@GetMapping("/controller/refresh")
	public ActionModel refresh() {
		houseService.refreshHouseModel();
		return new ActionModel("OK");
	}

	@GetMapping("/controller/startDataMigration")
	public void startDataMigration(@RequestParam(name = "uuid", required = false) String uuid) {
		if (!this.uuid.equals(uuid)) {
			LOG.error("UUID not set correctly");
			return;
		}
		LOG.info("startDataMigration");
		databaseMigrationService.startMigration();
		postConstruct();
	}

}