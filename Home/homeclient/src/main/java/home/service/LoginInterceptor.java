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

import homecontroller.util.HomeAppConstants;

public class LoginInterceptor extends HandlerInterceptorAdapter {

	private static final String LOGIN = "/login";

	@Value("${authenticationURL}")
	private String authURL;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private Environment env;

	public static final String COOKIE_NAME = "HomeLoginCookie";

	private Log logger = LogFactory.getLog(LoginInterceptor.class);

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		if (exlusion(request)) {
			return true;
		}

		if (StringUtils.containsIgnoreCase(request.getRequestURL(), "upload")
				|| StringUtils.containsIgnoreCase(request.getRequestURL(), "controllerLongPolling")) {
			String token = env.getProperty(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN);
			String tokenSent = request.getHeader(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN);
			return StringUtils.isNotBlank(tokenSent) && StringUtils.equals(token, tokenSent);
		}

		if (StringUtils.containsIgnoreCase(request.getRequestURL(), "/logoff")) {
			logger.info("manual logout");
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
		if (params.containsKey("login_username")) {
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
				}
			}
		}

		String user = StringUtils.trimToNull(ExternalPropertiesDAO.getInstance().read(cookieRead(request)));
		if (user == null && !loggedIn) {
			logger.info("no user - redirect to login page - " + request.getRequestURL());
			response.sendRedirect(LOGIN);
			return false;
		}

		if (!loggedIn) {
			setNewCookie(request, response, user);
		}
		return true;
	}

	private boolean exlusion(HttpServletRequest request) {

		if (StringUtils.containsIgnoreCase(request.getRequestURL(), "/webjars/")) {
			return true;
		}

		if (StringUtils.endsWithIgnoreCase(request.getRequestURL(), ".png")
				|| StringUtils.endsWithIgnoreCase(request.getRequestURL(), ".ico")) {
			return true;
		}

		if (StringUtils.containsIgnoreCase(request.getRequestURL(), LOGIN)) {
			return true;
		}

		return StringUtils.containsIgnoreCase(request.getRequestURL(), "/error");
	}

	private boolean login(Map<String, String> params, HttpServletRequest request,
			HttpServletResponse response) {

		String loginUser = StringUtils.trimToEmpty(params.get("login_username"));
		String loginPass = StringUtils.trimToEmpty(params.get("login_password"));
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
		ExternalPropertiesDAO.getInstance().write(uuid, loginUser);

		if (!StringUtils.equals(oldCookieID, uuid) && oldCookieID != null) {
			ExternalPropertiesDAO.getInstance().delete(oldCookieID);
		}

		return uuid;
	}

	public void cookieDelete(HttpServletRequest request, HttpServletResponse response) {

		String oldCookie = cookieRead(request);
		if (oldCookie != null) {
			ExternalPropertiesDAO.getInstance().delete(oldCookie);
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
