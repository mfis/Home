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
import de.fimatas.users.api.TokenResult;
import de.fimatas.users.api.UserAPI;
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
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.fimatas.home.library.util.HomeAppConstants.PUSH_TOKEN_NOT_AVAILABLE_INDICATOR;

@RestController
public class AppRequestMapping {

    public static final String URI_CREATE_AUTH_TOKEN = "/createAuthToken";

    public static final String URI_WHOAMI = "/whoami";

    public static final String URI_GET_APP_MODEL = "/getAppModel";

    public static final String URI_GET_PUSH_MESSAGE_MODEL = "/getPushMessageModel";

    public static final String URI_GET_PUSH_SETTINGS = "/getPushSettings";

    public static final String URI_SET_PUSH_SETTING = "/setPushSetting";

    public static final String URI_SET_PRESENCE = "/setPresence";

    public static final String URI_LIVE_START = "/liveActivityStart";

    public static final String URI_LIVE_END = "/liveActivityEnd";

    public static Set<String> ALL_NON_LOGIN_APP_URIS = Set.of(
            URI_GET_APP_MODEL, URI_GET_PUSH_MESSAGE_MODEL, URI_GET_PUSH_SETTINGS, URI_SET_PUSH_SETTING,  URI_SET_PRESENCE, URI_LIVE_START, URI_LIVE_END
    );

    @Autowired
    private UserAPI userAPI;

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

        TokenResult result = userAPI.createToken(user, pass, applicationIdentifier, device);
        AppTokenCreationModel model = new AppTokenCreationModel();
        model.setSuccess(result.isCheckOk());
        model.setToken(StringUtils.trimToEmpty(result.getNewToken()));
        log.debug("NEW TOKEN: " + StringUtils.substring(result.getNewToken(), 0, 50));
        return model;
    }

    @GetMapping(value = URI_GET_APP_MODEL)
    public HomeViewModel getModel(@RequestParam("viewTarget") String viewTarget) {

        HouseModel houseModel = ModelObjectDAO.getInstance().readHouseModel();
        try {
            if (houseModel == null) {
                throw new IllegalStateException("State error - " + ModelObjectDAO.getInstance().getLastHouseModelState());
            } else {
                Model model = new ExtendedModelMap();
                houseView.fillViewModel(model, null, houseModel, ModelObjectDAO.getInstance().readHistoryModel(),
                    ModelObjectDAO.getInstance().readLightsModel(), ModelObjectDAO.getInstance().readWeatherForecastModel(), ModelObjectDAO.getInstance().readPresenceModel(), ModelObjectDAO.getInstance().readHeatpumpRoofModel(), ModelObjectDAO.getInstance().readHeatpumpBasementModel(), ModelObjectDAO.getInstance().readElectricVehicleModel(), null, null, null, ModelObjectDAO.getInstance().readPvAdditionalDataModel());
                return appViewService.mapAppModel(model, AppViewService.AppViewTarget.valueOf(viewTarget.toUpperCase()));
            }
        } catch (Exception e) {
            log.error("sending empty app model due to exception while mapping.", e);
            return appViewService.newEmptyModel();
        }
    }

    @GetMapping(value = URI_GET_PUSH_MESSAGE_MODEL)
    public PushMessagesView getPushMessageModel(@RequestHeader("appUserName") String appUserName) {

        final PushMessageModel pushMessageModel = ModelObjectDAO.getInstance().readPushMessageModel();
        if(pushMessageModel == null){
            return null;
        }

        var list = pushMessageModel.getList().stream()
                .filter(msg -> msg.getUsername().equalsIgnoreCase(appUserName))
                .map(msg -> {
                    var ts = StringUtils.capitalize(viewFormatter.formatTimestamp(msg.getTimestamp(), ViewFormatter.TimestampFormat.DATE_TIME));
                    return new PushMessageView("id_pm_" + msg.getTimestamp(), ts, msg.getTitle(), msg.getTextMessage());
                }).collect(Collectors.toList());

        return new PushMessagesView(list);
    }

    @GetMapping(value = URI_GET_PUSH_SETTINGS)
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

    @PostMapping(value = URI_SET_PUSH_SETTING)
    public AppPushSettingsModels setPushSetting(@RequestParam("token") String token, @RequestParam("key")String key, @RequestParam("value") String value) {

        Message message = new Message();
        message.setMessageType(MessageType.SETTINGS_EDIT);
        message.setToken(lookupToken(token));
        message.setKey(key);
        message.setValue(value);

        MessageQueue.getInstance().request(message, true);

        return getPushSettings(token);
    }

    @PostMapping(value = URI_SET_PRESENCE)
    public void setPushSetting(@RequestHeader(name = LoginInterceptor.APP_USER_NAME) String appUserName, @RequestParam("value") String value) {

        Message message = new Message();
        message.setMessageType(MessageType.PRESENCE_EDIT);
        message.setKey(appUserName);
        message.setValue(value);

        MessageQueue.getInstance().request(message, true);
    }

    @PostMapping(value = URI_LIVE_START)
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

    @PostMapping(value = URI_LIVE_END)
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