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
import de.fimatas.home.client.Application;

@Component
public class UserService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${authenticationURL}")
    private String authURL;

    @Value("${tokenCreationURL}")
    private String tokenCreationURL;

    private Log logger = LogFactory.getLog(UserService.class);

    public boolean checkAuthentication(String user, String pass) {
        return checkCall(user, pass, null, SecretType.PASSWORD);
    }

    public boolean checkPin(String user, String pin) {
        return checkCall(user, pin, null, SecretType.PIN);
    }

    public boolean checkToken(String user, String token, String device) {
        return checkCall(user, token, device, SecretType.TOKEN);
    }

    public String createAppToken(String user, String pass, String device) {

        HttpHeaders headers = createHeaders();
        MultiValueMap<String, String> map = createParameters(user, pass, device, SecretType.PASSWORD);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(tokenCreationURL, request, String.class);
            if(responseEntity.getStatusCode().is2xxSuccessful()) {
                return responseEntity.getBody();
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return null;
            } else {
                logger.error("Checking authentication not successful.(#1)", e);
            }
        } catch (Exception e) {
            logger.error("Checking authentication not successful.(#2)", e);
        }
        return null;
    }

    private boolean checkCall(String user, String secret, String device, SecretType type) {

        HttpHeaders headers = createHeaders();
        MultiValueMap<String, String> map = createParameters(user, secret, device, type);
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

    private MultiValueMap<String, String> createParameters(String user, String secret, String device, SecretType type) {

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("user", user);
        map.add(type.parameterName, secret);
        map.add("application", Application.APPLICATION_NAME);
        if (device != null) {
            map.add("device", device);
        }
        return map;
    }

    private HttpHeaders createHeaders() {

        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "*/*");
        headers.add("Cache-Control", "no-cache");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    private enum SecretType {
        PASSWORD("pass"), PIN("pin"), TOKEN("token");

        private SecretType(String n) {
            parameterName = n;
        }

        private String parameterName;
    }
}
