package de.fimatas.home.client.service;

import java.io.IOException;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import de.fimatas.home.client.domain.service.HistoryViewService;
import de.fimatas.home.client.domain.service.HouseViewService;
import de.fimatas.home.client.request.ControllerRequestMapping;
import de.fimatas.home.library.util.HomeAppConstants;

public class LoginInterceptor extends HandlerInterceptorAdapter {

	private static final String LOGIN_PASSWORD = "login_password"; // NOSONAR

	private static final String LOGIN_USERNAME = "login_username";

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

		if (isControllerRequest(request)) {
			String token = env.getProperty(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN);
			String tokenSent = request.getHeader(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN);
			return controllerSuccessResponse(
					StringUtils.isNotBlank(tokenSent) && StringUtils.equals(token, tokenSent));
		}

		if (isAssetRequest(request)) {
			return true;
		}

		if (isLoginRequest(request)) {
			return true;
		}

		if (isLogoffRequest(request)) {
			cookieDelete(request, response);
			response.sendRedirect(LoginController.LOGIN_URI);
			return false;
		}

		Map<String, String> params = mapRequestParameters(request);
		String cookie = cookieRead(request);
		String userName = null;
		
		if (params.containsKey(LOGIN_USERNAME)) {
			if (userHasNotAcceptedCookies(params)) {
				response.sendRedirect(LoginController.LOGIN_COOKIECHECK_URI);
				return false;
			} else {
				userName = login(params);
			}
		} else if (cookie != null) {
			userName = StringUtils.trimToNull(LoginCookieDAO.getInstance().read(cookie));
		}

		return handleLoginttempt(request, response, params, userName);
	}

	private boolean userHasNotAcceptedCookies(Map<String, String> params) {
		return !StringUtils.trimToEmpty(params.get("login_cookieok")).equals("true");
	}

	private boolean handleLoginttempt(HttpServletRequest request, HttpServletResponse response,
			Map<String, String> params, String userName) throws IOException {

		if (userName == null) {
			String loginUser = StringUtils.trimToNull(params.get(LOGIN_USERNAME));
			String cookie = StringUtils.trimToNull(cookieRead(request));
			if (cookie != null || loginUser != null) {
				response.sendRedirect(LoginController.LOGIN_FAILED_URI);
				handleClientFalseLoginCounter();
				logger.info("attempt login not successful - user=" + loginUser + ", cookie=" + cookie
						+ ", requested=" + request.getRequestURI());
			} else {
				response.sendRedirect(LoginController.LOGIN_URI);
			}
			return false;
		} else {
			controllerFalseLoginCounter = 0; // reset at manual login
			setNewCookie(request, response, userName);
			return true;
		}
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

	private void handleClientFalseLoginCounter() {

		clientFalseLoginCounter++;
		if (clientFalseLoginCounter > 10) { // cookie brute force attack?
			logger.info("handleClientFalseLogin - deleting all cookie information");
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

	private boolean isAssetRequest(HttpServletRequest request) {

		if (StringUtils.startsWith(request.getRequestURI(), "/webjars/")) {
			return true;
		}

		if ((StringUtils.endsWith(request.getRequestURI(), ".png")
				|| StringUtils.endsWith(request.getRequestURI(), ".ico")
				|| StringUtils.endsWith(request.getRequestURI(), ".css")
				|| StringUtils.endsWith(request.getRequestURI(), "robots.txt")
				|| StringUtils.endsWith(request.getRequestURI(), ".js"))
				&& StringUtils.countMatches(request.getRequestURI(), "/") == 1) {
			return true;
		}

		return StringUtils.equals(request.getRequestURI(), "/error");
	}

	private boolean isLoginRequest(HttpServletRequest request) {

		if (StringUtils.equals(request.getRequestURI(), LoginController.LOGIN_URI)) {
			return true;
		}

		if (StringUtils.equals(request.getRequestURI(), LoginController.LOGIN_COOKIECHECK_URI)) {
			return true;
		}

		if (StringUtils.equals(request.getRequestURI(), LoginController.LOGIN_FAILED_URI)) { // NOSONAR
			return true;
		}

		return false;
	}

	private String login(Map<String, String> params) {

		String loginUser = StringUtils.trimToEmpty(params.get(LOGIN_USERNAME));
		String loginPass = StringUtils.trimToEmpty(params.get(LOGIN_PASSWORD));
		if (checkAuthentication(loginUser, loginPass)) {
			return loginUser;
		} else {
			return null;
		}
	}

	public boolean checkAuthentication(String user, String pass) {

		HttpHeaders headers = new HttpHeaders();
		headers.add("Accept", "*/*");
		headers.add("Cache-Control", "no-cache");
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("user", user);
		map.add("pass", pass);
		map.add("application", "de.fimatas.home.client");

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
		try {
			ResponseEntity<String> responseEntity = restTemplate.postForEntity(authURL, request,
					String.class);
			return responseEntity.getStatusCode().is2xxSuccessful();
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
				return false;
			} else {
				logger.error("Checking authentication not successful.(#1)", e);
			}
		} catch (Exception e) {
			logger.error("Checking authentication not successful.(#2)", e);
		}
		return false;
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
