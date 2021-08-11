package de.fimatas.home.client.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import de.fimatas.home.client.request.AppRequestMapping;
import de.fimatas.home.client.request.ControllerRequestMapping;
import de.fimatas.home.library.model.Pages;
import de.fimatas.home.library.util.HomeAppConstants;
import mfi.files.api.DeviceType;
import mfi.files.api.TokenResult;
import mfi.files.api.UserService;

public class LoginInterceptor extends HandlerInterceptorAdapter {

    public static final String COOKIE_NAME = "HomeLoginCookie";

    public static final String APP_DEVICE = "appDevice";

    public static final String APP_USER_NAME = "appUserName";

    private static final String APP_USER_TOKEN = "appUserToken";

    private static final String APP_ADDITIONAL_COOKIE_HEADER = "appAdditionalCookieHeader";

    public static final String APP_PUSH_TOKEN = "appPushToken";

    private static final String LOGIN_PASSWORD = "login_password"; // NOSONAR

    private static final String LOGIN_USERNAME = "login_username";

    private static final String USER_AGENT = "user-agent";

    private static final Set<String> LOGIN_URIS =
        Set.of(LoginController.LOGIN_URI, LoginController.LOGIN_COOKIECHECK_URI, LoginController.LOGIN_FAILED_URI,
            LoginController.LOGIN_VIA_APP_FAILED_URI, AppRequestMapping.URI_WHOAMI, AppRequestMapping.URI_CREATE_AUTH_TOKEN);

    private Set<String> WHITELIST_URIS = Set.of("/error", "/robots.txt");
    private Set<String> WHITELIST_URIS_DYNAMIC;

    private static final Set<String> WHITELIST_EXTENSIONS =
        Set.of("png", "css", "js", "ico", "svg", "eot", "ttf", "woff", "woff2", "map");

    @Autowired
    private UserService userService;

    @Autowired
    private Environment env;

    @Value("${server.servlet.session.cookie.secure}")
    private String cookieSecure;

    @Value("${appdistribution.web.url}")
    private String appdistributionWebUrl;

    private int controllerFalseLoginCounter = 0;

    private final Log log = LogFactory.getLog(LoginInterceptor.class);

    @PostConstruct
    public void postConstruct() throws MalformedURLException {
        URL appdistributionUrl = new URL(appdistributionWebUrl);
        WHITELIST_URIS_DYNAMIC = Set.of(appdistributionUrl.getPath() + "homeClient.ipa", appdistributionUrl.getPath() + "manifest.plist");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        boolean loginOK = checkLogin(request, response);

        if (!loginOK && !noLoginDataProvided(request)) {
            log.warn("Request: " + request.getRequestURI() + " NOT ok");
        }

        return loginOK;
    }

    boolean isAssetRequest(HttpServletRequest request) {

        if (Pages.getEntry(request.getRequestURI()) != null) {
            return false;
        }

        if (WHITELIST_URIS.contains(request.getRequestURI())) {
            return true;
        }

        if (WHITELIST_URIS_DYNAMIC!=null && WHITELIST_URIS_DYNAMIC.contains(request.getRequestURI())) {
            return true;
        }

        return WHITELIST_EXTENSIONS.contains(FilenameUtils.getExtension(request.getRequestURI()));
    }

    private boolean checkLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {

        if (isControllerRequest(request)) {
            return controllerLogin(request);
        }

        if (isAssetRequest(request)) {
            return true;
        }

        if (isLoginRequest(request)) {
            return true;
        }

        if (isLogoffRequest(request)) {
            logoff(request, response);
            response.sendRedirect(LoginController.LOGIN_URI);
            return true;
        }

        Map<String, String> params = mapRequestParameters(request);

        if (noLoginDataProvided(request)) {
            response.sendRedirect(LoginController.LOGIN_URI);
            return false;
        }

        // Token hat hoehere Prio als Cookie
        if (StringUtils.isNotBlank(request.getHeader(APP_USER_TOKEN))) {
            return checkUser(tokenLogin(request, response));
        }

        if (params.containsKey(LOGIN_USERNAME)) {
            if (userHasNotAcceptedCookies(params)) {
                response.sendRedirect(LoginController.LOGIN_COOKIECHECK_URI);
                return false;
            } else {
                return checkUser(credentialsBrowserLogin(params, request, response));
            }
        } else {
            return checkUser(cookieBrowserLogin(request, response));
        }
    }

    private boolean checkUser(String userName) {
        return StringUtils.isNotBlank(userName);
    }

    private void logoff(HttpServletRequest request, HttpServletResponse response) {

        String oldCookie = cookieRead(request);
        if (checkUser(oldCookie)) {
            userService.deleteToken(userService.userNameFromLoginCookie(oldCookie), request.getHeader(USER_AGENT),
                DeviceType.BROWSER);
            cookieDelete(response);
        }

        if (StringUtils.isNoneBlank(request.getHeader(APP_DEVICE), request.getHeader(APP_USER_TOKEN))) {
            userService.deleteToken(userService.userNameFromLoginCookie(request.getHeader(APP_USER_TOKEN)),
                request.getHeader(APP_DEVICE), DeviceType.APP);
        }
    }

    private boolean noLoginDataProvided(HttpServletRequest request) {
        return StringUtils.isAllBlank(request.getHeader(APP_USER_TOKEN), request.getParameter(LOGIN_USERNAME),
            cookieRead(request));
    }

    private boolean controllerLogin(HttpServletRequest request) {

        String controllerToken = env.getProperty(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN);
        String controllerTokenSent = request.getHeader(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN);

        return controllerSuccessResponse(
            checkUser(controllerTokenSent) && StringUtils.equals(controllerToken, controllerTokenSent));
    }

    private String tokenLogin(HttpServletRequest request, HttpServletResponse response) {

        boolean refreshToken = BooleanUtils.toBoolean(request.getHeader("refreshToken"));
        TokenResult tokenResult = userService.checkToken(request.getHeader(APP_USER_NAME), request.getHeader(APP_USER_TOKEN),
            request.getHeader(APP_DEVICE), DeviceType.APP, refreshToken);

        if (tokenResult.isCheckOk()) {
            if (refreshToken) {
                response.addHeader(APP_USER_TOKEN, tokenResult.getNewToken());
                if(log.isDebugEnabled()){
                    log.debug("REFRESHED TOKEN: " + StringUtils.substring(tokenResult.getNewToken(), 0, 50));
                }
                return userService.userNameFromLoginCookie(tokenResult.getNewToken());
            } else {
                return userService.userNameFromLoginCookie(request.getHeader(APP_USER_TOKEN));
            }
        } else {
            response.setStatus(HttpStatus.UNAUTHORIZED.value()); // 401
            return null;
        }
    }

    private String credentialsBrowserLogin(Map<String, String> params, HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String loginUser = StringUtils.trimToEmpty(params.get(LOGIN_USERNAME));
        String loginPass = StringUtils.trimToEmpty(params.get(LOGIN_PASSWORD));
        TokenResult tokenResult =
            userService.createToken(loginUser, loginPass, request.getHeader(USER_AGENT), DeviceType.BROWSER);
        if (tokenResult.isCheckOk()) {
            cookieWrite(response, tokenResult.getNewToken());
            return loginUser;
        } else {
            response.sendRedirect(LoginController.LOGIN_FAILED_URI);
            return null;
        }
    }

    private String cookieBrowserLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String token = cookieRead(request);
        if (StringUtils.isBlank(token)) {
            return null;
        }

        // Start Workaround
        String workaroundHeaderForWebViewCookieRefreshProblem = request.getHeader(APP_ADDITIONAL_COOKIE_HEADER);
        if (StringUtils.isNotBlank(workaroundHeaderForWebViewCookieRefreshProblem)) {
            if(getTokenCreationDate(workaroundHeaderForWebViewCookieRefreshProblem).isAfter(getTokenCreationDate(token))){
                log.warn("USING workaroundHeaderForWebViewCookieRefreshProblem");
                token = workaroundHeaderForWebViewCookieRefreshProblem;
            }
        }
        // End Workaround

        boolean isAjaxRequest = BooleanUtils.toBoolean(request.getHeader("isAjaxRequest"));
        TokenResult tokenResult = userService.checkToken(userService.userNameFromLoginCookie(token), token,
            request.getHeader(USER_AGENT), DeviceType.BROWSER, !isAjaxRequest);

        if (tokenResult.isCheckOk()) {
            if (isAjaxRequest) {
                return userService.userNameFromLoginCookie(token);
            } else {
                cookieWrite(response, tokenResult.getNewToken());
                return userService.userNameFromLoginCookie(tokenResult.getNewToken());
            }
        } else {
            response.sendRedirect(LoginController.LOGIN_FAILED_URI);
            logoff(request, response);
            return null;
        }
    }

    private LocalDateTime getTokenCreationDate(String token){
        return LocalDateTime.parse(StringUtils.split(token, '*')[1], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private boolean userHasNotAcceptedCookies(Map<String, String> params) {
        return !StringUtils.trimToEmpty(params.get("login_cookieok")).equals("true");
    }

    private Map<String, String> mapRequestParameters(HttpServletRequest request) {

        Map<String, String> params = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String key = parameterNames.nextElement();
            params.put(key, request.getParameter(key));
        }
        return params;
    }

    private boolean isLogoffRequest(HttpServletRequest request) {
        return StringUtils.equals(request.getRequestURI(), LoginController.LOGOFF_URI);
    }

    private boolean controllerSuccessResponse(boolean success) {

        if (controllerFalseLoginCounter > 2) { // controller token brute force
                                               // attack?
            log.error("controllerFalseLoginCounter=" + controllerFalseLoginCounter);
            return false;
        } else if (success) {
            return true;
        } else {
            controllerFalseLoginCounter++;
            return false;
        }
    }

    private boolean isControllerRequest(HttpServletRequest request) {
        return StringUtils.startsWith(request.getRequestURI(), ControllerRequestMapping.UPLOAD_METHOD_PREFIX) || StringUtils
            .equals(request.getRequestURI(), ControllerRequestMapping.CONTROLLER_LONG_POLLING_FOR_AWAIT_MESSAGE_REQUEST);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return LOGIN_URIS.contains(request.getRequestURI());
    }

    private String cookieRead(HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(COOKIE_NAME)) {
                return StringUtils.trimToNull(cookie.getValue());
            }
        }
        return null;
    }

    private void cookieDelete(HttpServletResponse response) {

        Cookie cookie = new Cookie(COOKIE_NAME, StringUtils.EMPTY);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private void cookieWrite(HttpServletResponse response, String value) {

        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(60 * 60 * 24 * 92);
        cookie.setSecure(Boolean.parseBoolean(cookieSecure));
        response.addCookie(cookie);
    }

}
