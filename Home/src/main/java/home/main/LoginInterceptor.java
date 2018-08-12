package home.main;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

public class LoginInterceptor extends HandlerInterceptorAdapter {

	@Value("${authenticationURL}")
	private String authURL;

	private static final String COOKIE_NAME = "HomeLoginCookie";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

		// System.out.println("Request URL: " + request.getRequestURL());

		if (StringUtils.containsIgnoreCase(request.getRequestURL(), "/webjars/")) {
			return true;
		}

		if (StringUtils.endsWithIgnoreCase(request.getRequestURL(), ".png") || StringUtils.endsWithIgnoreCase(request.getRequestURL(), ".ico")) {
			return true;
		}

		if (StringUtils.containsIgnoreCase(request.getRequestURL(), "/login")) {
			return true;
		}

		if (StringUtils.containsIgnoreCase(request.getRequestURL(), "/logoff")) {
			cookieDelete(request, response);
			response.sendRedirect("/login");
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
					response.sendRedirect("/loginFailed");
					return false;
				} else {
					loggedIn = true;
				}
			}
		}

		String user = StringUtils.trimToNull(ExternalPropertiesDAO.getInstance().read(cookieRead(request)));
		if (user == null && !loggedIn) {
			response.sendRedirect("/login");
			return false;
		}

		if (!loggedIn) {
			setNewCookie(request, response, user);
		}
		return true;
	}

	private boolean login(Map<String, String> params, HttpServletRequest request, HttpServletResponse response) throws IOException {

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

			RestTemplate rest = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.add("Accept", "*/*");
			headers.add("Cache-Control", "no-cache");
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
			map.add("user", user);
			map.add("pass", pass);
			map.add("application", "home");

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
			ResponseEntity<String> responseEntity = rest.postForEntity(authURL, request, String.class);
			return responseEntity.getStatusCode().is2xxSuccessful();

		} catch (Exception e) {
			System.out.println(e);
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

		String uuid = UUID.randomUUID().toString();
		cookieWrite(response, uuid);
		ExternalPropertiesDAO.getInstance().write(uuid, loginUser);
		if (StringUtils.isNotBlank(oldCookieID)) {
			if (oldCookieID != null) {
				ExternalPropertiesDAO.getInstance().delete(oldCookieID);
			}
		}

		return uuid;
	}

	public void cookieDelete(HttpServletRequest request, HttpServletResponse response) {

		String oldCookie = cookieRead(request);
		if (oldCookie != null) {
			ExternalPropertiesDAO.getInstance().delete(oldCookie);
		}

		Cookie cookie = new Cookie(COOKIE_NAME, "");
		cookie.setMaxAge(0);
		response.addCookie(cookie);
	}

	private static void cookieWrite(HttpServletResponse response, String value) {

		Cookie cookie = new Cookie(COOKIE_NAME, value);
		cookie.setMaxAge(60 * 60 * 24 * 180);
		response.addCookie(cookie);
	}
}
