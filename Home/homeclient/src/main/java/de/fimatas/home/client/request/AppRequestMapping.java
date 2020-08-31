package de.fimatas.home.client.request;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import de.fimatas.home.client.Application;
import de.fimatas.home.client.model.AppTokenCreationModel;
import de.fimatas.home.client.service.UserService;

@RestController
public class AppRequestMapping {

    public static final String URI_CREATE_AUTH_TOKEN = "/createAuthToken";

    public static final String URI_WHOAMI = "/whoami";

    @Autowired
    private UserService userService;

    @GetMapping(value = URI_WHOAMI)
    public ResponseEntity<String> whoami() {
        return new ResponseEntity<>(Application.APPLICATION_NAME, HttpStatus.OK);
    }

    @PostMapping(value = URI_CREATE_AUTH_TOKEN)
    public AppTokenCreationModel createAuthToken(@RequestParam("user") String user,
            @RequestParam("pass") String pass, @RequestParam("device") String device) {

        String token = userService.createAppToken(user, pass, device);
        AppTokenCreationModel model = new AppTokenCreationModel();
        model.setSuccess(token != null);
        model.setToken(StringUtils.trimToEmpty(token));
        return model;
    }

}