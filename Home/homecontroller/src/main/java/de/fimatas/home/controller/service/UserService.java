package de.fimatas.home.controller.service;

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

	public boolean checkPin(String user, String pin) {
		return call(user, pin);
	}

	private boolean call(String user, String pin) {

		HttpHeaders headers = new HttpHeaders();
		headers.add("Accept", "*/*");
		headers.add("Cache-Control", "no-cache");
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("user", user);
		map.add("pin", pin);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
		try {
			ResponseEntity<String> responseEntity = restTemplate.postForEntity(authURL, request, String.class);
			return responseEntity.getStatusCode().is2xxSuccessful();
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
				return false;
			} else {
				throw e;
			}
		} catch (Exception e) {
			logger.error("Checking authentication not successful.", e);
		}
		return false;
	}

}
