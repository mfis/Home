package de.fimatas.home.client.request;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static final Log log = LogFactory.getLog(AppRequestMapping.class);

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
        log.debug("NEW TOKEN: " + StringUtils.substring(result.getNewToken(), 0, 50));
        return model;
    }

    @GetMapping(value = "/getAppModel")
    public HomeViewModel getModel(@RequestParam("viewTarget") String viewTarget) {

        log.debug("getModel()");
        HouseModel houseModel = ModelObjectDAO.getInstance().readHouseModel();
        try {
            if (houseModel == null) {
                throw new IllegalStateException("State error - " + ModelObjectDAO.getInstance().getLastHouseModelState());
            } else {
                Model model = new ExtendedModelMap();
                houseView.fillViewModel(model, houseModel, ModelObjectDAO.getInstance().readHistoryModel(),
                    ModelObjectDAO.getInstance().readLightsModel());
                return appViewService.mapAppModel(model, AppViewService.AppViewTarget.valueOf(viewTarget.toUpperCase()));
            }
        } catch (Exception e) {
            log.error("sending empty app model due to exception while mapping.", e);
            return appViewService.newEmptyModel();
        }
    }

}