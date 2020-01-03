package home.service;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import home.request.ControllerRequestMapping;
import homecontroller.util.HomeAppConstants;

public class LoginInterceptor extends HandlerInterceptorAdapter {

	private static final String LOGOFF = "/logoff";

	private static final String LOGIN_PASSWORD = "login_password"; // NOSONAR

	private static final String LOGIN_USERNAME = "login_username";

	private static final String LOGIN = "/login";

	@Value("${authenticationURL}")
	private String authURL;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private Environment env;

	private int controllerFalseLoginCounter = 0;

	private int clientFalseLoginCounter = 0;

	public static final String COOKIE_NAME = "HomeLoginCookie";

	private Log logger = LogFactory.getLog(LoginInterceptor.class);

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		if (isAssetRequest(request)) {
			return true;
		}

		if (isControllerRequest(request)) {
			String token = env.getProperty(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN);
			String tokenSent = request.getHeader(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN);
			return controllerSuccessResponse(
					StringUtils.isNotBlank(tokenSent) && StringUtils.equals(token, tokenSent));
		}

		if (StringUtils.equals(request.getRequestURI(), LOGOFF)) {
			cookieDelete(request, response);
			response.sendRedirect(LOGIN);
			return false;
		}

		Map<String, String> params = new HashMap<>();
		Enumeration<String> parameterNames = request.getParameterNames();
		while (parameterNames.hasMoreElements()) {
			String key = parameterNames.nextElement();
			params.put(key, request.getParameter(key));
		}

		boolean loggedIn = false;
		if (params.containsKey(LOGIN_USERNAME)) {
			if (!StringUtils.trimToEmpty(params.get("login_cookieok")).equals("true")) {
				response.sendRedirect("/loginCookieCheck");
				return false;
			} else {
				if (!login(params, request, response)) {
					logger.info("login failed");
					response.sendRedirect("/loginFailed");
					return false;
				} else {
					loggedIn = true;
					controllerFalseLoginCounter = 0; // reset
				}
			}
		}

		String user = StringUtils.trimToNull(LoginCookieDAO.getInstance().read(cookieRead(request)));
		if (clientFalseLogin(loggedIn, user)) {
			logger.info("no user - redirect to login page - " + request.getRequestURI());
			response.sendRedirect(LOGIN);
			handleClientFalseLogin();
			return false;
		}

		if (!loggedIn) {
			setNewCookie(request, response, user);
		}
		return true;
	}

	private void handleClientFalseLogin() {

		clientFalseLoginCounter++;
		if (clientFalseLoginCounter > 10) { // cookie brute force attack?
			LoginCookieDAO.getInstance().deleteAll();
			clientFalseLoginCounter = 0;
		}
	}

	private boolean controllerSuccessResponse(boolean success) {

		if (controllerFalseLoginCounter > 3) { // controller token brute force
												// attack?
			logger.error("controllerFalseLoginCounter=" + controllerFalseLoginCounter);
			return false;
		} else if (success) {
			return true;
		} else {
			controllerFalseLoginCounter++;
			return false;
		}
	}

	private boolean isControllerRequest(HttpServletRequest request) {
		return StringUtils.startsWith(request.getRequestURI(), ControllerRequestMapping.UPLOAD_METHOD_PREFIX)
				|| StringUtils.equals(request.getRequestURI(),
						ControllerRequestMapping.CONTROLLER_LONG_POLLING_FOR_AWAIT_MESSAGE_REQUEST);
	}

	private boolean clientFalseLogin(boolean loggedIn, String user) {
		return user == null && !loggedIn;
	}

	private boolean isAssetRequest(HttpServletRequest request) {

		if (StringUtils.startsWith(request.getRequestURI(), "/webjars/")) {
			return true;
		}

		if ((StringUtils.endsWith(request.getRequestURI(), ".png")
				|| StringUtils.endsWith(request.getRequestURI(), ".ico")
				|| StringUtils.endsWith(request.getRequestURI(), ".css")
				|| StringUtils.endsWith(request.getRequestURI(), ".js"))
				&& StringUtils.countMatches(request.getRequestURI(), "/") == 1) {
			return true;
		}

		if (StringUtils.equals(request.getRequestURI(), LOGIN)) {
			return true;
		}

		return StringUtils.equals(request.getRequestURI(), "/error");
	}

	private boolean login(Map<String, String> params, HttpServletRequest request,
			HttpServletResponse response) {

		String loginUser = StringUtils.trimToEmpty(params.get(LOGIN_USERNAME));
		String loginPass = StringUtils.trimToEmpty(params.get(LOGIN_PASSWORD));
		if (checkAuthentication(loginUser, loginPass)) {
			setNewCookie(request, response, loginUser);
			return true;
		} else {
			return false;
		}
	}

	public boolean checkAuthentication(String user, String pass) {

		try {

			HttpHeaders headers = new HttpHeaders();
			headers.add("Accept", "*/*");
			headers.add("Cache-Control", "no-cache");
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
			map.add("user", user);
			map.add("pass", pass);
			map.add("application", "home");

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
			ResponseEntity<String> responseEntity = restTemplate.postForEntity(authURL, request,
					String.class);
			return responseEntity.getStatusCode().is2xxSuccessful();

		} catch (Exception e) {
			logger.error("Checking authentication not successful.", e);
			return false;
		}
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

	public String setNewCookie(HttpServletRequest request, HttpServletResponse response, String loginUser) {

		String oldCookieID = cookieRead(request);

		String uuid = null;
		if (StringUtils.isBlank(oldCookieID)) {
			uuid = UUID.randomUUID().toString();
		} else {
			uuid = oldCookieID;
		}
		cookieWrite(response, uuid);
		LoginCookieDAO.getInstance().write(uuid, loginUser);

		if (!StringUtils.equals(oldCookieID, uuid) && oldCookieID != null) {
			LoginCookieDAO.getInstance().delete(oldCookieID);
		}

		return uuid;
	}

	public void cookieDelete(HttpServletRequest request, HttpServletResponse response) {

		String oldCookie = cookieRead(request);
		if (oldCookie != null) {
			LoginCookieDAO.getInstance().delete(oldCookie);
		}

		Cookie cookie = new Cookie(COOKIE_NAME, "");
		cookie.setHttpOnly(true);
		cookie.setMaxAge(0);
		response.addCookie(cookie);
	}

	private static void cookieWrite(HttpServletResponse response, String value) {

		Cookie cookie = new Cookie(COOKIE_NAME, value);
		cookie.setHttpOnly(true);
		cookie.setMaxAge(60 * 60 * 24 * 180);
		response.addCookie(cookie);
	}
}
