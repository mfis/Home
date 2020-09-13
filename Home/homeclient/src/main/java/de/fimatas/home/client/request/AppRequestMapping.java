package de.fimatas.home.client.request;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import de.fimatas.home.client.Application;
import de.fimatas.home.client.domain.service.AppViewService;
import de.fimatas.home.client.domain.service.HouseViewService;
import de.fimatas.home.client.model.AppTokenCreationModel;
import de.fimatas.home.client.model.HomeViewModel;
import de.fimatas.home.client.service.UserService;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.HouseModel;

@RestController
public class AppRequestMapping {

    public static final String URI_CREATE_AUTH_TOKEN = "/createAuthToken";

    public static final String URI_WHOAMI = "/whoami";

    @Autowired
    private UserService userService;

    @Autowired
    private HouseViewService houseView;

    @Autowired
    private AppViewService appViewService;

    @GetMapping(value = URI_WHOAMI)
    public ResponseEntity<String> whoami() {
        return new ResponseEntity<>(Application.APPLICATION_NAME, HttpStatus.OK);
    }

    @PostMapping(value = URI_CREATE_AUTH_TOKEN)
    public AppTokenCreationModel createAuthToken(@RequestParam("user") String user, @RequestParam("pass") String pass,
            @RequestParam("device") String device) {

        String token = userService.createAppToken(user, pass, device);
        AppTokenCreationModel model = new AppTokenCreationModel();
        model.setSuccess(token != null);
        model.setToken(StringUtils.trimToEmpty(token));
        return model;
    }

    @GetMapping(value = "/getAppModel")
    public HomeViewModel getModel(@RequestParam("viewTarget") String viewTarget) {

        Model model = new ExtendedModelMap();
        HouseModel houseModel = ModelObjectDAO.getInstance().readHouseModel();
        if (houseModel == null) {
            throw new IllegalStateException("State error - " + ModelObjectDAO.getInstance().getLastHouseModelState());
        } else {
            houseView.fillViewModel(model, houseModel, ModelObjectDAO.getInstance().readHistoryModel());
            return appViewService.mapAppModel(model, viewTarget);
        }
    }

}