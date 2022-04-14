package de.fimatas.home.client.request;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.fimatas.home.library.domain.model.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

@Controller
public class HomeRequestMapping {

    private static final String CLIENT_NAME = "clientName";

    private static final String SITE_REQUEST_IS_APP = "isApp";

    private static final String SITE_REQUEST_TS = "SITE_REQUEST_TS";

    private static final String REDIRECT = "redirect:";

    public static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final String DEVICE_NAME = "deviceName";

    private static final String HUE_DEVICE_ID = "hueDeviceId";

    private static final String CAMERA_MODE = "cameraMode";

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
    public String message(Model model, //
            @CookieValue(name = LoginInterceptor.COOKIE_NAME, required = false) String userCookie, //
            @RequestParam(name = "type") String type, //
            @RequestParam(name = DEVICE_NAME, required = false) String deviceName, //
            @RequestParam(name = HUE_DEVICE_ID, required = false) String hueDeviceId, //
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
                + deviceName + ", hueDeviceId="
                + hueDeviceId + ", value="
                + value + ", isApp=" + isNativeApp + ", pinLength=" + StringUtils.trimToEmpty(securityPin).length());
        }

        String userName = isNativeApp ? appUserName : userService.userNameFromLoginCookie(userCookie);

        if (!isPinBlankOrSetAndCorrect(userName, securityPin)) {
            prepareErrorMessage(isNativeApp, "Die eingegebene PIN ist nicht korrekt.", userCookie, httpServletResponse);
            log.warn("message for previous error: userCookie length=" + userCookie.length() + ", appUserName=" + appUserName + ", type=" + type + ", deviceName="
                    + deviceName + ", hueDeviceId="
                    + hueDeviceId + ", value="
                    + value + ", isApp=" + isNativeApp + ", pin length=" + StringUtils.trimToEmpty(securityPin).length());
            return lookupMessageReturnValue(isNativeApp, MessageType.valueOf(type).getTargetSite());
        }

        Message responseMessage = request(userName, type, deviceName, hueDeviceId, value, securityPin);

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
    public String appInstallation(Model model, HttpServletRequest servletRequest,
                                  @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie, HttpServletResponse response) {

        fillMenu(Pages.PATH_APP, model, response, false);
        fillUserAttributes(model, userCookie);
        String itmsLink = "itms-services://?action=download-manifest&url=" + appdistributionWebUrl + "manifest.plist";
        model.addAttribute("itmsLink", itmsLink);
        return "appInstallation";
    }


    @GetMapping(value = "/cameraPicture", produces = "image/jpeg")
    public ResponseEntity<byte[]> cameraPicture(@RequestParam(DEVICE_NAME) String deviceName,
            @RequestParam(CAMERA_MODE) String cameraMode, @RequestParam("ts") String timestamp,
            @RequestParam(name = "onlyheader", required = false) String onlyheader) {

        boolean onlyHeaderFlag = Boolean.parseBoolean(onlyheader);
        log.info("poll for camera image - " + timestamp + (onlyHeaderFlag ? " onlyHeader" : ""));
        byte[] bytes = cameraPicture(Device.valueOf(deviceName), CameraMode.valueOf(cameraMode), Long.parseLong(timestamp));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("image", "jpeg"));
        headers.setContentDispositionFormData("attachment", deviceName + "_" + cameraMode + ".jpg");
        headers.setContentLength(bytes.length);
        if (bytes.length == 0) {
            return new ResponseEntity<>(bytes, headers, HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        }
    }

    @GetMapping(value = "/cameraPictureRequest")
    public ResponseEntity<String> cameraPictureRequest(Model model,
            @CookieValue(LoginInterceptor.COOKIE_NAME) String userCookie, @RequestParam(name = "type") String type,
            @RequestParam(name = DEVICE_NAME, required = false) String deviceName, @RequestParam("value") String value) {

        log.info("requesting new camera image " + deviceName);

        Message response = request(userService.userNameFromLoginCookie(userCookie), type, deviceName, null, value, null);
        return new ResponseEntity<>(response.getResponse(), HttpStatus.OK);
    }

    @RequestMapping(Pages.PATH_HOME) // NOSONAR: POST after login, all other GET
    public String homePage(Model model, HttpServletResponse response,
            @CookieValue(name = LoginInterceptor.COOKIE_NAME, required = false) String userCookie,
            @RequestHeader(name = "ETag", required = false) String etag,
            @RequestHeader(name = "User-Agent", required = false) String userAgent,
            @RequestHeader(name = CLIENT_NAME, required = false) String clientName,
            @RequestHeader(name = LoginInterceptor.APP_PUSH_TOKEN, required = false) String appPushToken) {

        boolean isWebViewApp = StringUtils.equals(userAgent, ControllerUtil.USER_AGENT_APP_WEB_VIEW);

        if (log.isDebugEnabled()) {
            log.debug(
                "home: isWebViewApp=" + isWebViewApp + ", clientName=" + clientName + ", appPushToken="
                    + appPushToken + ", etag=" + etag);
        }

        if (isWebViewApp) {
            handlePushToken(appPushToken, userService.userNameFromLoginCookie(userCookie), clientName);
        }

        boolean isNewMessage = ViewAttributesDAO.getInstance().isPresent(userCookie, ViewAttributesDAO.MESSAGE);
        fillMenu(Pages.PATH_HOME, model, response, isWebViewApp);
        fillUserAttributes(model, userCookie);
        HouseModel houseModel = ModelObjectDAO.getInstance().readHouseModel();

        try {
            if (houseModel == null) {
                mappingErrorAttributes(model, response, "Keine aktuellen Daten vorhanden - " + ModelObjectDAO.getInstance().getLastHouseModelState(), null);
                return "error";
            } else if (isModelUnchanged(etag, houseModel, ModelObjectDAO.getInstance().readLightsModel(), ModelObjectDAO.getInstance().readWeatherForecastModel()) && !isNewMessage) {
                response.setStatus(HttpStatus.NOT_MODIFIED.value());
                return "empty";
            } else {
                houseView.fillViewModel(model, houseModel, ModelObjectDAO.getInstance().readHistoryModel(),
                    ModelObjectDAO.getInstance().readLightsModel(), ModelObjectDAO.getInstance().readWeatherForecastModel());
                return Pages.getEntry(Pages.PATH_HOME).getTemplate();
            }
        } catch (Exception e) {
            String message = "sending error page to browser due to exception while mapping.";
            mappingErrorAttributes(model, response, message, e);
            log.error(message, e);
            return "error";
        }
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
            message.setValue(appPushToken);
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

    private boolean isModelUnchanged(String etag, HouseModel houseModel, LightsModel lightsModel, WeatherForecastModel weatherForecastModel) {
        long max = Stream.of(houseModel.getDateTime(), lightsModel.getTimestamp(), weatherForecastModel.getDateTime()).max(Long::compare).get();
        return StringUtils.isNotBlank(etag)
            && StringUtils.equals(etag, Long.toString(max));
    }

    private Message request(String userName, String type, String deviceName, String hueDeviceId, String value,
            String securityPin) {

        MessageType messageType = MessageType.valueOf(type);
        Device device = StringUtils.isBlank(deviceName) ? null : Device.valueOf(deviceName);

        Message message = new Message();
        message.setMessageType(messageType);
        message.setDevice(device);
        message.setHueDeviceId(hueDeviceId);
        message.setValue(value);
        message.setUser(userName);
        message.setSecurityPin(securityPin);

        return MessageQueue.getInstance().request(message, true);
    }

    private byte[] cameraPicture(Device device, CameraMode mode, long timestamp) {
        CameraPicture cameraPicture = ModelObjectDAO.getInstance().readCameraPicture(device, mode, timestamp);
        if (cameraPicture != null) {
            return cameraPicture.getBytes();
        }
        return new byte[0];
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