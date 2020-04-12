package de.fimatas.home.client.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class UserService {

	@Autowired
	private RestTemplate restTemplate;

	@Value("${authenticationURL}")
	private String authURL;

	private Log logger = LogFactory.getLog(UserService.class);

	public boolean checkAuthentication(String user, String pass) {
		return call(user, pass, SecretType.PASSWORD);
	}

	public boolean checkPin(String user, String pin) {
		return call(user, pin, SecretType.PIN);
	}

	private boolean call(String user, String secret, SecretType type) {

		HttpHeaders headers = new HttpHeaders();
		headers.add("Accept", "*/*");
		headers.add("Cache-Control", "no-cache");
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("user", user);
		map.add(type.parameterName, secret);
		map.add("application", "de.fimatas.home.client");

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
		try {
			ResponseEntity<String> responseEntity = restTemplate.postForEntity(authURL, request, String.class);
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

	private enum SecretType {
		PASSWORD("pass"), PIN("pin");

		private SecretType(String n) {
			parameterName = n;
		}

		private String parameterName;
	}
}
