package homecontroller.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import home.model.Message;
import homecontroller.domain.model.ActionModel;
import homecontroller.domain.model.AutomationState;
import homecontroller.domain.model.SettingsModel;
import homecontroller.domain.service.HistoryService;
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
	private HistoryService historyService;

	// @Autowired
	// private CameraService cameraService;

	@Autowired
	private SettingsService settingsService;

	@Autowired
	private UploadService uploadService;

	@Autowired
	@Qualifier("restTemplateLongPolling")
	private RestTemplate restTemplateLongPolling;

	private long resourceNotAvailableCounter;

	private static final Log LOG = LogFactory.getLog(ClientCommunicationService.class);

	@Scheduled(fixedDelay = 1)
	private void longPolling() {
		Message message = pollForMessage();
		if (message != null) {
			handle(message);
		}
	}

	private void handle(Message message) {

		try {
			switch (message.getMessageType()) {
			case REFRESH_ALL_MODELS:
				refreshAll();
				break;
			case TOGGLEAUTOMATION:
				houseService.toggleautomation(message.getDevice(),
						AutomationState.valueOf(message.getValue()));
				houseService.refreshHouseModel();
				break;
			case TOGGLESTATE:
				houseService.togglestate(message.getDevice(), Boolean.valueOf(message.getValue()));
				houseService.refreshHouseModel();
				break;
			case HEATINGBOOST:
				houseService.heatingBoost(message.getDevice());
				houseService.refreshHouseModel();
				break;
			case HEATINGMANUAL:
				houseService.heatingManual(message.getDevice(), new BigDecimal(message.getValue()));
				houseService.refreshHouseModel();
				break;
			case SHUTTERPOSITION:
				houseService.shutterPosition(message.getDevice(), Integer.parseInt(message.getValue()));
				houseService.refreshHouseModel();
				break;
			case SETTINGS_CLIENTNAME:
				SettingsModel settingsModelClientName = settingsService.read(message.getUser());
				settingsModelClientName.setClientName(message.getValue());
				settingsService.updateSettingsModel(settingsModelClientName);
				break;
			case SETTINGS_PUSH_HINTS:
				SettingsModel settingsModelHints = settingsService.read(message.getUser());
				settingsModelHints.setPushHints(Boolean.parseBoolean(message.getValue()));
				settingsService.updateSettingsModel(settingsModelHints);
				break;
			case SETTINGS_PUSH_HINTS_HYSTERESIS:
				SettingsModel settingsModelHintHysteresis = settingsService.read(message.getUser());
				settingsModelHintHysteresis.setHintsHysteresis(Boolean.parseBoolean(message.getValue()));
				settingsService.updateSettingsModel(settingsModelHintHysteresis);
				break;
			case SETTINGS_PUSH_DOORBELL:
				SettingsModel settingsModelDoorbell = settingsService.read(message.getUser());
				settingsModelDoorbell.setPushDoorbell(Boolean.parseBoolean(message.getValue()));
				settingsService.updateSettingsModel(settingsModelDoorbell);
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

	private void refreshAll() {

		if (ModelObjectDAO.getInstance().readHouseModel() == null) {
			houseService.refreshHouseModel();
		} else {
			uploadService.upload(ModelObjectDAO.getInstance().readHouseModel());
		}

		if (ModelObjectDAO.getInstance().readHistoryModel() == null) {
			historyService.refreshHistoryModelComplete();
		} else {
			uploadService.upload(ModelObjectDAO.getInstance().readHistoryModel());
		}

		uploadService.upload(ModelObjectDAO.getInstance().readCameraModel());

		settingsService.refreshSettingsModelsComplete();
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

		String plainClientCredentials = env.getProperty("client.auth.user") + ":"
				+ env.getProperty("client.auth.pass");
		String base64ClientCredentials = new String(Base64.encodeBase64(plainClientCredentials.getBytes()));
		headers.set("Authorization", "Basic " + base64ClientCredentials);

		try {
			HttpEntity<ActionModel> request = new HttpEntity<>(new ActionModel(""), headers);
			ResponseEntity<Message> response = restTemplateLongPolling.postForEntity(url, request,
					Message.class);
			HttpStatus statusCode = response.getStatusCode();

			connectionEstablishedLogging();
			if (!statusCode.is2xxSuccessful()) {
				LOG.error("Could not successful poll for message. RC=" + statusCode.value());
				return null;
			}
			return response.getBody();

		} catch (ResourceAccessException rae) {
			connectionNotEstablishedLogging();
			waitAMoment();
			return null;
		} catch (Exception e) {
			LOG.warn("Could not poll for message.", e);
			return null;
		}
	}

	private void waitAMoment() {
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) { // NOSONAR
		}
	}

	private void connectionNotEstablishedLogging() {
		resourceNotAvailableCounter++;
		String suppressLogEntries = resourceNotAvailableCounter == 5
				? " NO FURTHER LOG ENTRIES WILL BE WRITTEN."
				: "";
		if (resourceNotAvailableCounter < 6) {
			LOG.warn("Could not connect to client to poll for message (#" + resourceNotAvailableCounter + ")."
					+ suppressLogEntries);
		}
	}

	private void connectionEstablishedLogging() {
		if (resourceNotAvailableCounter > 0) {
			LOG.warn("Connecting to client successful after " + resourceNotAvailableCounter + " times.");
			resourceNotAvailableCounter = 0;
		}
	}

}
