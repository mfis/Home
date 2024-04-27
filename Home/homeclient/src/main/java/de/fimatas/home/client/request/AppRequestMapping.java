package de.fimatas.home.client.request;

import de.fimatas.home.client.domain.model.PushMessageView;
import de.fimatas.home.client.domain.model.PushMessagesView;
import de.fimatas.home.client.domain.service.AppViewService;
import de.fimatas.home.client.domain.service.HouseViewService;
import de.fimatas.home.client.domain.service.ViewFormatter;
import de.fimatas.home.client.model.*;
import de.fimatas.home.client.service.LoginInterceptor;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.HouseModel;
import de.fimatas.home.library.domain.model.PushMessageModel;
import de.fimatas.home.library.model.Message;
import de.fimatas.home.library.model.MessageType;
import de.fimatas.home.library.model.SettingsModel;
import mfi.files.api.DeviceType;
import mfi.files.api.TokenResult;
import mfi.files.api.UserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.fimatas.home.library.util.HomeAppConstants.PUSH_TOKEN_NOT_AVAILABLE_INDICATOR;

@RestController
public class AppRequestMapping {

    public static final String URI_CREATE_AUTH_TOKEN = "/createAuthToken";

    public static final String URI_WHOAMI = "/whoami";

    @Autowired
    private UserService userService;

    @Autowired
    private HouseViewService houseView;

    @Autowired
    private AppViewService appViewService;

    @Autowired
    private ViewFormatter viewFormatter;

    @Value("${application.identifier}")
    private String applicationIdentifier;

    @Value("${pushtoken.acceptNotAvailableToken}")
    private boolean acceptNotAvailableToken;

    private static final Log log = LogFactory.getLog(AppRequestMapping.class);

    @GetMapping(value = URI_WHOAMI)
    public ResponseEntity<String> whoami() {
        return new ResponseEntity<>(applicationIdentifier, HttpStatus.OK);
    }

    @PostMapping(value = URI_CREATE_AUTH_TOKEN)
    public AppTokenCreationModel createAuthToken(@RequestParam("user") String user, @RequestParam("pass") String pass,
            @RequestParam("device") String device) {

        TokenResult result = userService.createToken(user, pass, device, DeviceType.APP);
        AppTokenCreationModel model = new AppTokenCreationModel();
        model.setSuccess(result.isCheckOk());
        model.setToken(StringUtils.trimToEmpty(result.getNewToken()));
        log.debug("NEW TOKEN: " + StringUtils.substring(result.getNewToken(), 0, 50));
        return model;
    }

    @GetMapping(value = "/getAppModel")
    public HomeViewModel getModel(@RequestParam("viewTarget") String viewTarget) {

        log.debug("getModel()");
        HouseModel houseModel = ModelObjectDAO.getInstance().readHouseModel();
        try {
            if (houseModel == null) {
                throw new IllegalStateException("State error - " + ModelObjectDAO.getInstance().getLastHouseModelState());
            } else {
                Model model = new ExtendedModelMap();
                houseView.fillViewModel(model, null, houseModel, ModelObjectDAO.getInstance().readHistoryModel(),
                    ModelObjectDAO.getInstance().readLightsModel(), ModelObjectDAO.getInstance().readWeatherForecastModel(), ModelObjectDAO.getInstance().readPresenceModel(), ModelObjectDAO.getInstance().readHeatpumpModel(), ModelObjectDAO.getInstance().readElectricVehicleModel(), ModelObjectDAO.getInstance().readPushMessageModel(), ModelObjectDAO.getInstance().readTasksModel());
                return appViewService.mapAppModel(model, AppViewService.AppViewTarget.valueOf(viewTarget.toUpperCase()));
            }
        } catch (Exception e) {
            log.error("sending empty app model due to exception while mapping.", e);
            return appViewService.newEmptyModel();
        }
    }

    @GetMapping(value = "/getPushMessageModel")
    public PushMessagesView getPushMessageModel(@RequestHeader("appUserName") String appUserName) {

        final PushMessageModel pushMessageModel = ModelObjectDAO.getInstance().readPushMessageModel();
        if(pushMessageModel == null){
            return null;
        }

        var list = pushMessageModel == null ? Collections.EMPTY_LIST :
                pushMessageModel.getList().stream()
                        .filter(msg -> msg.getUsername().equalsIgnoreCase(appUserName))
                        .map(msg -> {
                            var ts = StringUtils.capitalize(viewFormatter.formatTimestamp(msg.getTimestamp(), ViewFormatter.TimestampFormat.DATE_TIME));
                            return new PushMessageView("id_pm_" + msg.getTimestamp(), ts, msg.getTitle(), msg.getTextMessage());
                        }).collect(Collectors.toList());

        return new PushMessagesView(list);
    }

    @GetMapping(value = "/getPushSettings")
    public AppPushSettingsModels getPushSettings(@RequestParam("token") String token) {

        final Collection<SettingsModel> settingsModels = ModelObjectDAO.getInstance().readAllSettings();
        if(settingsModels == null){
            return null;
        }

        final Optional<SettingsModel> settingsModel = settingsModels.stream()
                .filter(settings -> settings.getToken().equals(lookupToken(token))).findFirst();
        var listPushSettings = new LinkedList<AppPushSettingsModel>();
        var listAttributes = new LinkedList<AppAttributeModel>();
        if(settingsModel.isPresent()){
            settingsModel.get().getPushNotifications().forEach((k, v) -> listPushSettings.add(new AppPushSettingsModel(k.name(), k.getSettingsText(), v)));
            settingsModel.get().getAttributes().forEach((k, v) -> listAttributes.add(new AppAttributeModel(k, v)));
        }
        return new AppPushSettingsModels(listPushSettings, listAttributes);
    }

    @PostMapping(value = "/setPushSetting")
    public AppPushSettingsModels setPushSetting(@RequestParam("token") String token, @RequestParam("key")String key, @RequestParam("value") String value) {

        Message message = new Message();
        message.setMessageType(MessageType.SETTINGS_EDIT);
        message.setToken(lookupToken(token));
        message.setKey(key);
        message.setValue(value);

        MessageQueue.getInstance().request(message, true);

        return getPushSettings(token);
    }

    @PostMapping(value = "/setPresence")
    public void setPushSetting(@RequestHeader(name = LoginInterceptor.APP_USER_NAME) String appUserName, @RequestParam("value") String value) {

        Message message = new Message();
        message.setMessageType(MessageType.PRESENCE_EDIT);
        message.setKey(appUserName);
        message.setValue(value);

        MessageQueue.getInstance().request(message, true);
    }

    @PostMapping(value = "/liveActivityStart")
    public void liveActivityStart(@RequestParam("token") String token,
                                  @RequestHeader(name = LoginInterceptor.APP_USER_NAME) String appUserName,
                                  @RequestHeader(name = LoginInterceptor.APP_DEVICE) String appDevice) {

        Message message = new Message();
        message.setMessageType(MessageType.LIVEACTIVITY_START);
        message.setToken(token);
        message.setUser(appUserName);
        message.setDeviceId(appDevice);

        MessageQueue.getInstance().request(message, true);
    }

    @PostMapping(value = "/liveActivityEnd")
    public void liveActivityEnd(@RequestParam("token") String token) {

        Message message = new Message();
        message.setMessageType(MessageType.LIVEACTIVITY_END);
        message.setToken(token);

        MessageQueue.getInstance().request(message, true);
    }

    private String lookupToken(String tokenRequestParameter){
        if(StringUtils.isBlank(tokenRequestParameter) && acceptNotAvailableToken){
            return PUSH_TOKEN_NOT_AVAILABLE_INDICATOR;
        }
        return tokenRequestParameter;
    }

}