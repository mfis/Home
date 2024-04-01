package de.fimatas.home.controller.service;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.model.PresenceState;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import de.fimatas.home.controller.domain.service.HistoryService;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.dao.ModelObjectDAO;
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
    private LightService lightService;

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private PresenceService presenceService;

    @Autowired
    private HeatpumpService heatpumpService;
    @Autowired
    private SettingsService settingsService;

    @Autowired
    private ElectricVehicleService electricVehicleService;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private PushService pushService;

    @Autowired
    private LiveActivityService liveActivityService;

    @Autowired
    private PhotovoltaicsOverflowService photovoltaicsOverflowService;

    @Autowired
    @Qualifier("restTemplateLongPolling")
    private RestTemplate restTemplateLongPolling;

    private long resourceNotAvailableCounter;

    private static final Log LOG = LogFactory.getLog(ClientCommunicationService.class);

    @Scheduled(fixedDelay = 40)
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
                houseService.refreshHouseModel(false);
                break;
            case TOGGLESTATE:
                if(message.getDevice()== Device.SCHALTER_WALLBOX && Boolean.parseBoolean(message.getValue())){
                    electricVehicleService.saveChargingUser(message.getUser());
                }
                houseService.togglestate(message.getDevice(), Boolean.parseBoolean(message.getValue()));
                houseService.refreshHouseModel(false);
                break;
            case TOGGLELIGHT:
                lightService.toggleLight(message.getDeviceId(), Boolean.valueOf(message.getValue()));
                lightService.refreshLightsModel();
                break;
            case HEATINGBOOST:
                houseService.heatingBoost(message.getDevice());
                houseService.refreshHouseModel(false);
                break;
            case HEATINGMANUAL:
                houseService.heatingManual(message.getDevice(), new BigDecimal(message.getValue()));
                houseService.refreshHouseModel(false);
                break;
            case HEATINGAUTO:
                houseService.heatingAuto(message.getDevice());
                houseService.refreshHouseModel(false);
                break;
            case OPEN:
                houseService.doorState(message);
                houseService.refreshHouseModel(false);
                break;
            case SHUTTERPOSITION:
                houseService.shutterPosition(message.getDevice(), Integer.parseInt(message.getValue()));
                houseService.refreshHouseModel(false);
                break;
            case SETTINGS_NEW:
                if(settingsService.createNewSettingsForToken(message.getToken(), message.getUser(), message.getClient())){
                    pushService.sendRegistrationConfirmation(message.getUser(), message.getToken(), message.getClient());
                }
                break;
            case SETTINGS_EDIT:
                settingsService.editSetting(message.getToken(), message.getKey(), Boolean.parseBoolean(message.getValue()));
                break;
            case PRESENCE_EDIT:
                presenceService.update(message.getKey(), PresenceState.valueOf(message.getValue()));
                break;
            case CONTROL_HEATPUMP:
                List<Place> places = new LinkedList<>();
                places.add(message.getPlace());
                List.of(StringUtils.split(message.getAdditionalData(), ',')).forEach(ap ->places.add(Place.valueOf(ap)));
                heatpumpService.preset(places, HeatpumpPreset.valueOf(message.getValue()));
                break;
            case SLIDERVALUE:
                electricVehicleService.saveChargingUser(message.getUser());
                electricVehicleService.updateBatteryPercentage(ElectricVehicle.valueOf(message.getDeviceId()), message.getValue());
                break;
            case WALLBOX_SELECTED_EV:
                electricVehicleService.saveChargingUser(message.getUser());
                electricVehicleService.updateSelectedEvForWallbox(ElectricVehicle.valueOf(message.getDeviceId()));
                break;
            case CHARGELIMIT:
                electricVehicleService.saveChargingUser(message.getUser());
                electricVehicleService.updateChargeLimit(ElectricVehicle.valueOf(message.getDeviceId()), message.getValue());
                break;
            case LIVEACTIVITY_START:
                liveActivityService.start(message.getToken(), message.getUser(), message.getDeviceId());
                break;
            case LIVEACTIVITY_END:
                liveActivityService.end(message.getToken());
                break;
            case PV_OVERFLOW_MAX_WATTS_GRID:
                photovoltaicsOverflowService.writeOverflowGridWattage(message.getDevice(), Integer.parseInt(message.getValue()));
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
            houseService.refreshHouseModel(false);
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

        if (ModelObjectDAO.getInstance().readWeatherForecastModel() == null) {
            weatherService.refreshWeatherForecastModel();
        } else {
            uploadService.uploadToClient(ModelObjectDAO.getInstance().readWeatherForecastModel());
        }

        if (ModelObjectDAO.getInstance().readPresenceModel() == null) {
            presenceService.refresh();
        } else {
            uploadService.uploadToClient(ModelObjectDAO.getInstance().readPresenceModel());
        }

        if (ModelObjectDAO.getInstance().readHeatpumpModel() == null) {
            heatpumpService.scheduledRefreshFromDriverCache();
        } else {
            uploadService.uploadToClient(ModelObjectDAO.getInstance().readHeatpumpModel());
        }

        if (ModelObjectDAO.getInstance().readElectricVehicleModel() == null) {
            electricVehicleService.refreshModel();
        } else {
            uploadService.uploadToClient(ModelObjectDAO.getInstance().readElectricVehicleModel());
        }

        if (ModelObjectDAO.getInstance().readPushMessageModel() == null) {
            pushService.refreshModel();
        } else {
            uploadService.uploadToClient(ModelObjectDAO.getInstance().readPushMessageModel());
        }

        settingsService.refreshSettingsModelsComplete();
    }

    private Message pollForMessage() {

        String host = env.getProperty("client.hostName");
        String url = host + "/controllerLongPollingForAwaitMessageRequest";

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.ALL));
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
            HttpStatusCode statusCode = response.getStatusCode();

            connectionEstablishedLogging();
            if (!statusCode.is2xxSuccessful()) {
                LOG.error("Could not successful poll for message. RC=" + statusCode.value());
                return null;
            }
            return response.getBody();

        } catch (ResourceAccessException | HttpServerErrorException | HttpClientErrorException e) {
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
            TimeUnit.SECONDS.sleep(3);
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
