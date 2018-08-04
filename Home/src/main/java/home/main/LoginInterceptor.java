package home.main;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

public class LoginInterceptor extends HandlerInterceptorAdapter {

	@Value("${authenticationURL}")
	private String authURL;

	private static final String COOKIE_NAME = "HomeLoginCookie";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

		System.out.println("Request URL: " + request.getRequestURL());

		if (StringUtils.containsIgnoreCase(request.getRequestURL(), "/webjars/")) {
			return true;
		}

		Map<String, String> params = new HashMap<>();
		Enumeration<String> parameterNames = request.getParameterNames();
		while (parameterNames.hasMoreElements()) {
			String key = parameterNames.nextElement();
			params.put(key, request.getParameter(key));
		}

		if (params.containsKey("login_username")) {
			if (!StringUtils.trimToEmpty(params.get("login_cookieok")).equals("true")) {
				response.sendRedirect("/loginCookieCheck");
				return false;
			} else {
				if (!login(params, request, response)) {
					response.sendRedirect("/loginFailed");
					return false;
				}
			}
		}

		String user = StringUtils.trimToNull(CookieMap.getInstance().read(cookieRead(request)));
		if (user == null) {
			response.sendRedirect("/login");
			return false;
		}

		return true;
	}

	private boolean login(Map<String, String> params, HttpServletRequest request, HttpServletResponse response) throws IOException {

		String loginUser = StringUtils.trimToEmpty(params.get("login_username"));
		String loginPass = StringUtils.trimToEmpty(params.get("login_password"));
		return checkAuthentication(loginUser, loginPass);
	}

	public boolean checkAuthentication(String user, String pass) {

		try {

			RestTemplate rest = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.add("Accept", "*/*");

			HttpEntity<String> requestEntity = new HttpEntity<String>("", headers);
			ResponseEntity<String> responseEntity = rest.exchange(url, HttpMethod.GET, requestEntity, String.class);

			String response = responseEntity.getBody();

			HttpClient client = new HttpClient();
			PostMethod method = new PostMethod(url);

			method.addParameter("user", user);
			method.addParameter("pass", pass);
			return client.executeMethod(method) == 200;
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
		CookieMap.getInstance().write(uuid, loginUser);
		if (StringUtils.isNotBlank(oldCookieID)) {
			if (oldCookieID != null) {
				CookieMap.getInstance().delete(oldCookieID);
			}
		}

		return uuid;
	}

	public void cookieDelete(HttpServletRequest request, HttpServletResponse response) {

		String oldCookie = cookieRead(request);
		if (oldCookie != null) {
			CookieMap.getInstance().delete(oldCookie);
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
