package de.fimatas.home.client.request;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import de.fimatas.home.client.domain.service.AppViewService;
import de.fimatas.home.client.domain.service.HouseViewService;
import de.fimatas.home.client.model.AppTokenCreationModel;
import de.fimatas.home.client.model.HomeViewModel;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.HouseModel;
import mfi.files.api.DeviceType;
import mfi.files.api.TokenResult;
import mfi.files.api.UserService;

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

    @Value("${application.identifier}")
    private String applicationIdentifier;

    @GetMapping(value = URI_WHOAMI)
    public ResponseEntity<String> whoami() {
        return new ResponseEntity<>(applicationIdentifier, HttpStatus.OK);
    }

    @PostMapping(value = URI_CREATE_AUTH_TOKEN)
    public AppTokenCreationModel createAuthToken(@RequestParam("user") String user, @RequestParam("pass") String pass,
            @RequestParam("device") String device) {

        TokenResult result = userService.createToken(user, pass, device, DeviceType.APP);
        AppTokenCreationModel model = new AppTokenCreationModel();
        model.setSuccess(result.isCheckOk());
        model.setToken(StringUtils.trimToEmpty(result.getNewToken()));
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