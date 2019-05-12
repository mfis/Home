package homecontroller.domain.service;

import java.util.Arrays;

import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class UploadService {

	@Autowired
	private RestTemplate restTemplate;

	public void upload(Object object) {
		uploadBinary("http://localhost:8099/upload" + object.getClass().getSimpleName(), object.getClass(),
				object);
	}

	private <T> T uploadBinary(String url, T t, Object instance) {

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.ALL));
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Cache-Control", "no-cache");

		@SuppressWarnings("unchecked")
		HttpEntity<T> request = new HttpEntity<>((T) instance, headers);
		ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
		int sc = response.getStatusCodeValue();

		try {
		} catch (RestClientException e) {
			LogFactory.getLog(UploadService.class).error("Could not upload data.", e);
		}
		LogFactory.getLog(UploadService.class).info("Uploaded data - " + sc);
		return t;
	}

}
