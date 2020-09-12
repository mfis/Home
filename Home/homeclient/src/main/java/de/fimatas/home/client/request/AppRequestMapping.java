package de.fimatas.home.client.request;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
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
import de.fimatas.home.client.domain.model.ClimateView;
import de.fimatas.home.client.domain.model.View;
import de.fimatas.home.client.domain.service.HouseViewService;
import de.fimatas.home.client.model.AppTokenCreationModel;
import de.fimatas.home.client.model.HomeViewModel;
import de.fimatas.home.client.model.HomeViewModel.HomeViewPlaceModel;
import de.fimatas.home.client.model.HomeViewModel.HomeViewValueModel;
import de.fimatas.home.client.service.UserService;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.HouseModel;
import de.fimatas.home.library.domain.model.Place;

@RestController
public class AppRequestMapping {

    public static final String URI_CREATE_AUTH_TOKEN = "/createAuthToken";

    public static final String URI_WHOAMI = "/whoami";

    @Autowired
    private UserService userService;

    @Autowired
    private HouseViewService houseView;

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
    public HomeViewModel getModel() {

        Model model = new ExtendedModelMap();
        HouseModel houseModel = ModelObjectDAO.getInstance().readHouseModel();
        if (houseModel == null) {
            throw new IllegalStateException("State error - " + ModelObjectDAO.getInstance().getLastHouseModelState());
        } else {
            houseView.fillViewModel(model, houseModel, ModelObjectDAO.getInstance().readHistoryModel());
            return mapAppModel(model);
        }
    }

    private HomeViewModel mapAppModel(Model model) {

        Set<Place> places = new LinkedHashSet<>();
        places.add(Place.LIVINGROOM);
        places.add(Place.BATHROOM);
        places.add(Place.KIDSROOM);

        HomeViewModel appModel = new HomeViewModel();
        appModel.setTimestamp(HomeRequestMapping.TS_FORMATTER.format(LocalDateTime.now()));

        Collection<Object> values = model.asMap().values();

        for (Place place : places) {
            for (Object value : values) {
                if (value instanceof View) {
                    View view = (View) value;
                    if (view.getPlace().contentEquals(place.getPlaceName())) {
                        // FIXME: Search for existing place from other device
                        HomeViewPlaceModel placeModel = appModel.new HomeViewPlaceModel();
                        placeModel.setId(place.name());
                        placeModel.setName(place.getPlaceName());
                        if (view instanceof ClimateView) {
                            placeModel.getValues().add(mapTemperature(appModel, place, view));
                            if (StringUtils.isNotBlank(((ClimateView) view).getStateHumidity())) {
                                placeModel.getValues().add(mapHumidity(appModel, place, view));
                            }
                        }

                        appModel.getPlaces().add(placeModel);
                    }
                }
            }
        }

        return appModel;
    }

    private HomeViewValueModel mapTemperature(HomeViewModel appModel, Place place, View view) {
        HomeViewValueModel hvm = appModel.new HomeViewValueModel();
        hvm.setId(place.getPlaceName() + "#temp");
        hvm.setKey("Temperatur");
        hvm.setValue(((ClimateView) view).getStateTemperature());
        // FIXME: accent
        return hvm;
    }

    private HomeViewValueModel mapHumidity(HomeViewModel appModel, Place place, View view) {
        HomeViewValueModel hvm = appModel.new HomeViewValueModel();
        hvm.setId(place.getPlaceName() + "#humi");
        hvm.setKey("Luftfeuchtigkeit");
        hvm.setValue(((ClimateView) view).getStateHumidity());
        // FIXME: accent
        return hvm;
    }

}