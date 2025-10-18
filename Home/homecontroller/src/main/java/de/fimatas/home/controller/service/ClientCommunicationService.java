package de.fimatas.home.controller.service;

import de.fimatas.home.controller.domain.service.HistoryService;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.model.Message;
import de.fimatas.home.library.model.PresenceState;
import de.fimatas.home.library.util.HomeAppConstants;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class ClientCommunicationService {

    @Autowired
    private Environment env;

    @Autowired
    private HouseService houseService;

    @Autowired
    private FrontDoorService frontDoorService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private LightService lightService;

    @Autowired
    private WeatherServiceScheduler weatherServiceScheduler;

    @Autowired
    private PresenceService presenceService;

    @Autowired
    private HeatpumpRoofService heatpumpRoofService;

    @Autowired
    private HeatpumpBasementService heatpumpBasementService;

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
    private TasksService tasksService;

    @Autowired
    private MaintenanceService maintenanceService;

    @Autowired
    private ControllerStateService controllerStateService;

    @Autowired
    @Qualifier("restTemplateLongPolling")
    private RestTemplate restTemplateLongPolling;

    private long resourceNotAvailableCounter;

    private static final Log LOG = LogFactory.getLog(ClientCommunicationService.class);

    @CircuitBreaker(name = "reverseconnection", fallbackMethod = "fallbackResponse")
    public void longPolling() {
        Message message = pollForMessage();
        if (message != null) {
            handle(message);
        }
    }

    @SuppressWarnings("unused") // used by resilience4j
    public void fallbackResponse(Throwable t) {
        // noop
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
                frontDoorService.changeDoorLockState(message, true);
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
                frontDoorService.handlePresenceChange(message.getKey(), PresenceState.valueOf(message.getValue()));
                break;
            case CONTROL_HEATPUMP_ROOF:
                List<Place> places = new LinkedList<>();
                places.add(message.getPlace());
                List.of(StringUtils.split(message.getAdditionalData(), ',')).forEach(ap ->places.add(Place.valueOf(ap)));
                heatpumpRoofService.preset(places, HeatpumpRoofPreset.valueOf(message.getValue()));
                break;
            case CONTROL_HEATPUMP_BASEMENT:
                heatpumpBasementService.readFromClientRequest();
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
                liveActivityService.endActivitiyFromClient(message.getToken());
                break;
            case PV_OVERFLOW_MAX_WATTS_GRID:
                photovoltaicsOverflowService.writeOverflowGridWattage(message.getDevice(), Integer.parseInt(message.getValue()));
                break;
                case PV_OVERFLOW_MIN_PATTERY_PERCENTAGE:
                photovoltaicsOverflowService.writeOverflowMinBatteryPercentage(message.getDevice(), PvBatteryMinCharge.valueOf(message.getValue()));
                break;
            case TASKS_EXECUTION:
                tasksService.markAsExecuted(message.getDeviceId());
                break;
            case MAINTENANCE:
                maintenanceService.doMaintenance(message);
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

    public void refreshAll() {

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
            // no active refresh from client
        } else {
            uploadService.uploadToClient(ModelObjectDAO.getInstance().readWeatherForecastModel());
        }

        if (ModelObjectDAO.getInstance().readPresenceModel() == null) {
            presenceService.refresh();
        } else {
            uploadService.uploadToClient(ModelObjectDAO.getInstance().readPresenceModel());
        }

        if (ModelObjectDAO.getInstance().readHeatpumpRoofModel() == null) {
            heatpumpRoofService.scheduledRefreshFromDriverCache();
        } else {
            uploadService.uploadToClient(ModelObjectDAO.getInstance().readHeatpumpRoofModel());
        }

        if (ModelObjectDAO.getInstance().readHeatpumpBasementModel() == null) {
            heatpumpBasementService.scheduledRefreshFromDriverCache();
        } else {
            uploadService.uploadToClient(ModelObjectDAO.getInstance().readHeatpumpBasementModel());
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

        if (ModelObjectDAO.getInstance().readTasksModel() == null) {
            tasksService.refresh();
        } else {
            uploadService.uploadToClient(ModelObjectDAO.getInstance().readTasksModel());
        }

        if (ModelObjectDAO.getInstance().readPvAdditionalDataModel() != null) {
            // no refresh here because of scheduled 1-minute interval
            uploadService.uploadToClient(ModelObjectDAO.getInstance().readPvAdditionalDataModel());
        }

        if (ModelObjectDAO.getInstance().readControllerStateModel() == null) {
            controllerStateService.refresh();
        } else {
            uploadService.uploadToClient(ModelObjectDAO.getInstance().readControllerStateModel());
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
