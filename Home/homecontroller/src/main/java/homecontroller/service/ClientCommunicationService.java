package homecontroller.service;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import home.model.Message;
import homecontroller.domain.model.ActionModel;
import homecontroller.domain.model.AutomationState;
import homecontroller.domain.service.HouseService;
import homecontroller.domain.service.UploadService;
import homecontroller.util.HomeAppConstants;
import homelibrary.dao.ModelObjectDAO;

@Component
public class ClientCommunicationService {

	@Autowired
	private Environment env;

	@Autowired
	private HouseService houseService;

	@Autowired
	private CameraService cameraService;

	@Autowired
	private SettingsService settingsService;

	@Autowired
	private UploadService uploadService;

	@Autowired
	@Qualifier("restTemplateLongPolling")
	private RestTemplate restTemplateLongPolling;

	private static final Log LOG = LogFactory.getLog(ClientCommunicationService.class);

	@Scheduled(fixedDelay = 0)
	private void longPolling() {
		Message message = pollForMessage();
		if (message != null) {
			CompletableFuture.runAsync(() -> {
				handle(message);
			});
		}
	}

	private void handle(Message message) {

		try {
			switch (message.getMessageType()) {
			case REFRESH_ALL_MODELS:
				uploadService.upload(ModelObjectDAO.getInstance().readHouseModel());
				uploadService.upload(ModelObjectDAO.getInstance().readHistoryModel());
				uploadService.upload(ModelObjectDAO.getInstance().readCameraModel());
				settingsService.refreshSettingsModelsComplete();
				break;
			case TOGGLEAUTOMATION:
				houseService.toggleautomation(message.getDevice(),
						AutomationState.valueOf(message.getValue()));
				uploadService.upload(ModelObjectDAO.getInstance().readHouseModel());
				break;
			case TOGGLESTATE:
				houseService.togglestate(message.getDevice(), Boolean.valueOf(message.getValue()));
				uploadService.upload(ModelObjectDAO.getInstance().readHouseModel());
				break;
			default:
				throw new IllegalStateException("Unknown MessageType:" + message.getMessageType().name());
			}
			message.setSuccessfullExecuted(true);

		} catch (Exception e) {
			LOG.error("Error executing message:", e);
			message.setSuccessfullExecuted(false);
		}
		uploadService.upload(message);
	}

	private Message pollForMessage() {

		String host = env.getProperty("client.hostName");
		String url = host + "/controllerLongPollingForAwaitMessageRequest";

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.ALL));
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Cache-Control", "no-cache");
		headers.set(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN,
				env.getProperty(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN));

		try {
			HttpEntity<ActionModel> request = new HttpEntity<>(new ActionModel(""), headers);
			ResponseEntity<Message> response = restTemplateLongPolling.postForEntity(url, request,
					Message.class);
			HttpStatus statusCode = response.getStatusCode();
			if (!statusCode.is2xxSuccessful()) {
				LOG.error("Could not successful poll for message. RC=" + statusCode.value());
				return null;
			}
			return response.getBody();

		} catch (Exception e) {
			LOG.warn("Could not poll for message.", e);
			return null;
		}
	}

}
