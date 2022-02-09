package de.fimatas.home.controller.service;

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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import de.fimatas.home.controller.domain.service.HistoryService;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.controller.domain.service.UploadService;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.ActionModel;
import de.fimatas.home.library.domain.model.AutomationState;
import de.fimatas.home.library.model.Message;
import de.fimatas.home.library.util.HomeAppConstants;

@Component
public class ClientCommunicationService {

    @Autowired
    private Environment env;

    @Autowired
    private HouseService houseService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private CameraService cameraService;

    @Autowired
    private LightService lightService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private PushService pushService;

    @Autowired
    @Qualifier("restTemplateLongPolling")
    private RestTemplate restTemplateLongPolling;

    private long resourceNotAvailableCounter;

    private static final Log LOG = LogFactory.getLog(ClientCommunicationService.class);

    @Scheduled(fixedDelay = 30)
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
                houseService.toggleautomation(message.getDevice(), AutomationState.valueOf(message.getValue()));
                houseService.refreshHouseModel();
                break;
            case TOGGLESTATE:
                houseService.togglestate(message.getDevice(), Boolean.parseBoolean(message.getValue()));
                houseService.refreshHouseModel();
                break;
            case TOGGLELIGHT:
                lightService.toggleLight(message.getHueDeviceId(), Boolean.valueOf(message.getValue()));
                lightService.refreshLightsModel();
                break;
            case HEATINGBOOST:
                houseService.heatingBoost(message.getDevice());
                houseService.refreshHouseModel();
                break;
            case HEATINGMANUAL:
                houseService.heatingManual(message.getDevice(), new BigDecimal(message.getValue()));
                houseService.refreshHouseModel();
                break;
            case HEATINGAUTO:
                houseService.heatingAuto(message.getDevice());
                houseService.refreshHouseModel();
                break;
            case OPEN:
                houseService.doorState(message);
                houseService.refreshHouseModel();
                break;
            case CAMERAPICTUREREQUEST:
                message.setResponse(cameraService.takeLivePicture(message.getDevice()));
                break;
            case SHUTTERPOSITION:
                houseService.shutterPosition(message.getDevice(), Integer.parseInt(message.getValue()));
                houseService.refreshHouseModel();
                break;
            case SETTINGS_NEW:
                if(settingsService.createNewSettingsForToken(message.getValue(), message.getUser(), message.getClient())){
                    pushService.sendRegistrationConfirmation(message.getValue(), message.getClient());
                }
                break;
            default:
                throw new IllegalStateException("Unknown MessageType:" + message.getMessageType().name());
            }
            message.setSuccessfullExecuted(true);

        } catch (Exception e) {
            LOG.error("Error executing message:", e);
            message.setSuccessfullExecuted(false);
        }
        uploadService.uploadToClient(message);
    }

    private void refreshAll() {

        if (ModelObjectDAO.getInstance().readHouseModel() == null) {
            houseService.refreshHouseModel();
        } else {
            uploadService.uploadToClient(ModelObjectDAO.getInstance().readHouseModel());
        }

        if (ModelObjectDAO.getInstance().readHistoryModel() == null) {
            historyService.refreshHistoryModelComplete();
        } else {
            uploadService.uploadToClient(ModelObjectDAO.getInstance().readHistoryModel());
        }

        if (ModelObjectDAO.getInstance().readLightsModel() == null) {
            lightService.refreshLightsModel();
        } else {
            uploadService.uploadToClient(ModelObjectDAO.getInstance().readLightsModel());
        }

        uploadService.uploadToClient(ModelObjectDAO.getInstance().readCameraModel());

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

        String plainClientCredentials = env.getProperty("client.auth.user") + ":" + env.getProperty("client.auth.pass");
        String base64ClientCredentials = new String(Base64.encodeBase64(plainClientCredentials.getBytes()));
        headers.set("Authorization", "Basic " + base64ClientCredentials);

        try {
            HttpEntity<ActionModel> request = new HttpEntity<>(new ActionModel(""), headers);
            ResponseEntity<Message> response = restTemplateLongPolling.postForEntity(url, request, Message.class);
            HttpStatus statusCode = response.getStatusCode();

            connectionEstablishedLogging();
            if (!statusCode.is2xxSuccessful()) {
                LOG.error("Could not successful poll for message. RC=" + statusCode.value());
                return null;
            }
            return response.getBody();

        } catch (ResourceAccessException | HttpServerErrorException e) {
            connectionNotEstablishedLogging(e);
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

    private void connectionNotEstablishedLogging(RestClientException e) {
        resourceNotAvailableCounter++;
        String suppressLogEntries = resourceNotAvailableCounter == 5 ? " NO FURTHER LOG ENTRIES WILL BE WRITTEN." : "";
        if (resourceNotAvailableCounter < 4) {
            LOG.warn("Could not connect to client to poll for message (#" + resourceNotAvailableCounter + "). "
                + (e.getMessage() != null ? e.getMessage().replace('\r', ' ').replace('\n', ' ') : "") + suppressLogEntries); // NOSONAR
        }
    }

    private void connectionEstablishedLogging() {
        if (resourceNotAvailableCounter > 0) {
            LOG.warn("Connecting to client successful after " + resourceNotAvailableCounter + " times.");
            resourceNotAvailableCounter = 0;
        }
    }

}
