package de.fimatas.home.client.request;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import de.fimatas.home.client.domain.model.ValueWithCaption;
import de.fimatas.home.library.model.MaintenanceOptions;
import jakarta.servlet.http.HttpServletResponse;

import de.fimatas.home.library.domain.model.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import de.fimatas.home.client.domain.service.HistoryViewService;
import de.fimatas.home.client.domain.service.HouseViewService;
import de.fimatas.home.client.model.MessageQueue;
import de.fimatas.home.client.service.LoginInterceptor;
import de.fimatas.home.client.service.SettingsViewService;
import de.fimatas.home.client.service.ViewAttributesDAO;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.model.Message;
import de.fimatas.home.library.model.MessageType;
import de.fimatas.home.library.model.Pages;
import mfi.files.api.UserService;

import static de.fimatas.home.client.domain.service.HouseViewService.*;

@Controller
public class HomeRequestMapping {

    private static final String CLIENT_NAME = "clientName";

    private static final String SITE_REQUEST_IS_APP = "isApp";

    private static final String SITE_REQUEST_TS = "SITE_REQUEST_TS";

    private static final String REDIRECT = "redirect:";

    public static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final String DEVICE_NAME = "deviceName";

    private static final String DEVICE_ID = "deviceId";

    private static final Log log = LogFactory.getLog(HomeRequestMapping.class);

    @Autowired
    private HouseViewService houseView;

    @Autowired
    private HistoryViewService historyViewService;

    @Autowired
    private SettingsViewService settingsViewService;

    @Autowired
    private UserService userService;

    @Value("${appdistribution.web.url}")
    private String appdistributionWebUrl;


    @GetMapping("/message")
    public String message(
            @CookieValue(name = LoginInterceptor.COOKIE_NAME, required = false) String userCookie, //
            @RequestParam(name = "type") String type, //
            @RequestParam(name = DEVICE_NAME, required = false) String deviceName, //
            @RequestParam(name = DEVICE_ID, required = false) String deviceId, //
            @RequestParam(name = "placeName", required = false) String placeName, //
            @RequestParam(name = "additionalData", required = false) String additionalData, //
            @RequestParam(name = "value") String value, //
            @RequestHeader(name = LoginInterceptor.APP_USER_NAME, required = false) String appUserName, //
            @RequestHeader(name = "CSRF") String csrf, //
            @RequestHeader(name = "pin", required = false) String securityPin, //
            HttpServletResponse httpServletResponse) {

        if (!Boolean.parseBoolean(csrf)) {
            throw new IllegalStateException("CSRF Header not set properly");
        }

        boolean isNativeApp = StringUtils.isNotBlank(appUserName);

        if (log.isDebugEnabled()) {
            log.debug("message: userCookie=" + userCookie + ", appUserName=" + appUserName + ", type=" + type + ", deviceName="
                + deviceName + ", placeName=" + placeName + ", additionalData=" + additionalData + ", deviceId="
                + deviceId + ", value="
                + value + ", isApp=" + isNativeApp + ", pinLength=" + StringUtils.trimToEmpty(securityPin).length());
        }

        String userName = isNativeApp ? appUserName : userService.userNameFromLoginCookie(userCookie);

        if (!isPinBlankOrSetAndCorrect(userName, securityPin)) {
            prepareErrorMessage(isNativeApp, "Die eingegebene PIN ist nicht korrekt.", userCookie, httpServletResponse);
            log.warn("message for previous error: userCookie length=" + userCookie.length() + ", appUserName=" + appUserName + ", type=" + type + ", deviceName="
                    + deviceName + ", deviceId="
                    + deviceId + ", value="
                    + value + ", isApp=" + isNativeApp + ", pin length=" + StringUtils.trimToEmpty(securityPin).length());
            return lookupMessageReturnValue(isNativeApp, MessageType.valueOf(type).getTargetSite());
        }

        Message responseMessage = request(userName, type, deviceName, placeName, additionalData, deviceId, value, securityPin);

        if (!responseMessage.isSuccessfullExecuted()) {
            prepareErrorMessage(isNativeApp, "Die Anfrage konnte nicht erfolgreich verarbeitet werden.", userCookie,
                httpServletResponse);
            log.error("MESSAGE EXECUTION NOT SUCCESSFUL !!! - " + type);
        }
        return lookupMessageReturnValue(isNativeApp, responseMessage.getMessageType().getTargetSite());
    }

    private void prepareErrorMessage(boolean isApp, String message, String userCookie,
            HttpServletResponse httpServletResponse) {
        if (isApp) {
            httpServletResponse.setStatus(HttpStatus.CONFLICT.value());
        } else {
            ViewAttributesDAO.getInstance().push(userCookie, ViewAttributesDAO.MESSAGE, message);
        }
        if (log.isInfoEnabled()) {
            log.info("message - error=" + message);
        }
    }

    private String lookupMessageReturnValue(boolean isApp, String template) {
        if (isApp) {
            return "empty";
        } else {
            return REDIRECT + template;
        }
    }

    private boolean isPinBlankOrSetAndCorrect(String userName, String securityPin) {
        return StringUtils.isBlank(securityPin) || userService.checkPin(userName, securityPin);
    }

    @GetMapping("/history")
    public String history(Model model, @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie,
            @RequestParam(name = "key") String key) {
        fillUserAttributes(model, userCookie);
        historyViewService.fillHistoryViewModel(model, ModelObjectDAO.getInstance().readHistoryModel(),
            ModelObjectDAO.getInstance().readHouseModel(), key);
        return "history";
    }

    @GetMapping("/settings")
    public String settings(Model model, @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie) {
        fillUserAttributes(model, userCookie);
        model.addAttribute("pushsettings", settingsViewService.allSettingsAsString());
        return "settings";
    }

    @GetMapping("/appInstallation")
    public String appInstallation(Model model,
                                  @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie, HttpServletResponse response) {
        fillMenu(Pages.PATH_APP, model, response, false);
        fillUserAttributes(model, userCookie);
        String itmsLink = "itms-services://?action=download-manifest&url=" + appdistributionWebUrl + "manifest.plist";
        model.addAttribute("itmsLink", itmsLink);
        return "appInstallation";
    }

    @GetMapping("/maintenance")
    public String repair(Model model, @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie, HttpServletResponse response) {
        fillMenu(Pages.PATH_MAINTENANCE, model, response, false);
        fillUserAttributes(model, userCookie);
        List<ValueWithCaption> list = new LinkedList<>();
        Arrays.stream(MaintenanceOptions.values()).forEach(mo -> {
            var vwc = new ValueWithCaption();
            vwc.setCaption(mo.name());
            vwc.setValue(MESSAGEPATH + TYPE_IS + MessageType.MAINTENANCE + AND_VALUE_IS + mo.name());
            vwc.setCssClass(mo.getConditionColor().getUiClass());
            list.add(vwc);
        });
        model.addAttribute("maintenanceLinks", list);
        model.addAttribute("modelState", ModelObjectDAO.getInstance().printModelState());
        return "maintenance";
    }

    @RequestMapping(Pages.PATH_HOME) // NOSONAR: POST after login, all other GET
    public String homePage(Model model, HttpServletResponse response,
            @CookieValue(name = LoginInterceptor.COOKIE_NAME, required = false) String userCookie,
            @RequestHeader(name = "ETag", required = false) String etag,
            @RequestHeader(name = "User-Agent", required = false) String userAgent,
            @RequestHeader(name = CLIENT_NAME, required = false) String clientName,
            @RequestHeader(name = LoginInterceptor.APP_PUSH_TOKEN, required = false) String appPushToken) {

        long l1 = System.nanoTime();
        boolean isWebViewApp = StringUtils.equals(userAgent, ControllerUtil.USER_AGENT_APP_WEB_VIEW);

        if (isWebViewApp) {
            handlePushToken(appPushToken, userService.userNameFromLoginCookie(userCookie), clientName);
        }

        boolean isNewMessage = ViewAttributesDAO.getInstance().isPresent(userCookie, ViewAttributesDAO.MESSAGE);
        fillMenu(Pages.PATH_HOME, model, response, isWebViewApp);
        fillUserAttributes(model, userCookie);
        HouseModel houseModel = ModelObjectDAO.getInstance().readHouseModel();

        String returnTemplate;
        try {
            if (isModelUnchanged(etag) && !isNewMessage) {
                response.setStatus(HttpStatus.NOT_MODIFIED.value());
                returnTemplate =  "empty";
            } else {
                String user = userService.userNameFromLoginCookie(userCookie);
                houseView.fillViewModel(model, user, houseModel, ModelObjectDAO.getInstance().readHistoryModel(),
                    ModelObjectDAO.getInstance().readLightsModel(), ModelObjectDAO.getInstance().readWeatherForecastModel(), ModelObjectDAO.getInstance().readPresenceModel(), ModelObjectDAO.getInstance().readHeatpumpModel(), ModelObjectDAO.getInstance().readElectricVehicleModel(), ModelObjectDAO.getInstance().readPushMessageModel(), ModelObjectDAO.getInstance().readTasksModel(), ModelObjectDAO.getInstance().readPvAdditionalDataModel());
                returnTemplate =  Objects.requireNonNull(Pages.getEntry(Pages.PATH_HOME)).getTemplate();
            }
        } catch (Exception e) {
            String message = "sending error page to browser due to exception while mapping.";
            mappingErrorAttributes(model, response, message, e);
            log.error(message, e);
            returnTemplate =  "error";
        }

        if (log.isDebugEnabled()) {
            log.debug(
                    "home=" + response.getStatus() + ": isWebViewApp=" + isWebViewApp + ", clientName=" + clientName + ", etag=" + etag);
        }

        long l2 = System.nanoTime();
        long ldiff = (l2 - l1) / 1000000; // ms
        if(ldiff > 1500){
            log.warn("HomeRequestMapping#homePage slow response: " + ldiff + " ms!");
        }
        return returnTemplate;
    }

    private void handlePushToken(String appPushToken, String userName, String client) {
        
        if(!settingsViewService.isValidPushToken(appPushToken)) {
            return;
        }
        
        if (StringUtils.isBlank(client)) {
            return;
        }

        if (!ModelObjectDAO.getInstance().isKnownPushToken(appPushToken)) {
            Message message = new Message();
            message.setMessageType(MessageType.SETTINGS_NEW);
            message.setToken(appPushToken);
            message.setUser(userName);
            message.setClient(client);
            MessageQueue.getInstance().request(message, false);
        }
    }

    public void mappingErrorAttributes(Model model, HttpServletResponse response, String message, Exception exception) {
        model.addAttribute("timestamp", LocalDateTime.now().toString());
        model.addAttribute("status", response.getStatus());
        model.addAttribute("error", "n/a");
        model.addAttribute("path", Pages.PATH_HOME);
        model.addAttribute("message", message);
        model.addAttribute("exception", exception!=null ? exception.getMessage(): Strings.EMPTY);
    }

    private boolean isModelUnchanged(String etag) {
        return StringUtils.isNotBlank(etag)
            && StringUtils.equals(etag, Long.toString(ModelObjectDAO.getInstance().calculateModelTimestamp()));
    }

    private Message request(String userName, String type, String deviceName, String placeName, String additionalData, String deviceId, String value,
            String securityPin) {

        MessageType messageType = MessageType.valueOf(type);
        Device device = StringUtils.isBlank(deviceName) ? null : Device.valueOf(deviceName);
        Place place = StringUtils.isBlank(placeName) ? null : Place.valueOf(placeName);

        Message message = new Message();
        message.setMessageType(messageType);
        message.setDevice(device);
        message.setPlace(place);
        message.setAdditionalData(additionalData);
        message.setDeviceId(deviceId);
        message.setValue(value);
        message.setUser(userName);
        message.setSecurityPin(securityPin);

        return MessageQueue.getInstance().request(message, true);
    }

    private void fillUserAttributes(Model model, String userCookie) {
        String user = userService.userNameFromLoginCookie(userCookie);
        if (user != null) {
            model.addAttribute(ViewAttributesDAO.USER_NAME, user);
        }
        model.addAttribute("userErrorMessageText",
            StringUtils.trimToEmpty(ViewAttributesDAO.getInstance().pull(userCookie, ViewAttributesDAO.MESSAGE)));
    }

    private void fillMenu(String pathHome, Model model, HttpServletResponse response, boolean isWebViewApp) {

        if (isWebViewApp) {
            model.addAttribute("MENU_SELECTED", Pages.getAppHomeEntry());
        } else {
            model.addAttribute("MENU_SELECTED", Pages.getEntry(pathHome));
        }

        model.addAttribute("MENU_SELECTABLE", Pages.getOtherEntries(pathHome));

        model.addAttribute(SITE_REQUEST_IS_APP, Boolean.toString(isWebViewApp));

        model.addAttribute(SITE_REQUEST_TS, TS_FORMATTER.format(LocalDateTime.now()));
        response.setHeader(SITE_REQUEST_TS, TS_FORMATTER.format(LocalDateTime.now()));
        ControllerUtil.setEssentialHeader(response);
    }

}