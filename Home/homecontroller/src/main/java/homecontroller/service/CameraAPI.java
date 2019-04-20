package homecontroller.service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CameraAPI {

	@Autowired
	private RestTemplate restTemplate;

	@Scheduled(fixedDelay = (1000 * 60))
	public void foo() throws Exception {
		documentFromUrl();
	}

	@PostConstruct
	public void a() {
		restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());
	}

	private void documentFromUrl() throws Exception { // TODO

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));

		HttpEntity<String> entity = new HttpEntity<String>(headers);

		ResponseEntity<byte[]> response = restTemplate.exchange(
				"http://homematic-ccu2/ise/img/homematic_logo_small.png", HttpMethod.GET, entity,
				byte[].class, "1");

		if (response.getStatusCode() == HttpStatus.OK) {
			Files.write(Paths.get("/Users/mfi/Downloads/hmlogo.png"), response.getBody());
		}
	}

}
