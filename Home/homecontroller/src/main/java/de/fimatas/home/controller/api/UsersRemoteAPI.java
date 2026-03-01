package de.fimatas.home.controller.api;

import de.fimatas.home.library.util.HomeAppConstants;
import de.fimatas.users.api.UsersConstants;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
@CommonsLog
public class UsersRemoteAPI {

    @Autowired
    @Qualifier("restTemplateHue")
    private RestTemplate restTemplateHue;

    @Autowired
    private Environment env;

    @Value("${client.hostName:}")
    private String host;

    public boolean checkPIN(String username, String pin) {

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = host + UsersConstants.USERS_CHECK_PIN_PATH;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN,
                    env.getProperty(HomeAppConstants.CONTROLLER_CLIENT_COMM_TOKEN));

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("username", username);
            map.add("pin", pin);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

}
