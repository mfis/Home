package de.fimatas.home.client.domain.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import de.fimatas.home.client.domain.model.ClimateView;
import de.fimatas.home.client.domain.model.View;
import de.fimatas.home.client.model.HomeViewModel;
import de.fimatas.home.client.model.HomeViewModel.HomeViewPlaceModel;
import de.fimatas.home.client.model.HomeViewModel.HomeViewValueModel;
import de.fimatas.home.client.request.HomeRequestMapping;
import de.fimatas.home.library.domain.model.Place;

@Component
public class AppViewService {

    public HomeViewModel mapAppModel(Model model, String viewTarget) {

        Set<Place> placesOrder = lookupPlacesOrder(viewTarget);
        HomeViewModel appModel = new HomeViewModel();
        appModel.setTimestamp(HomeRequestMapping.TS_FORMATTER.format(LocalDateTime.now()));

        for (Place placeInOrder : placesOrder) {
            for (Object value : model.asMap().values()) {
                if (value instanceof View) {
                    mapView(appModel, placeInOrder, value);
                }
            }
        }

        return appModel;
    }

    private void mapView(HomeViewModel appModel, Place placeInOrder, Object value) {

        View view = (View) value;
        if (view.getPlace().contentEquals(placeInOrder.getPlaceName())) {
            HomeViewPlaceModel placeModel = lookupPlaceModel(appModel, placeInOrder);
            if (view instanceof ClimateView) {
                mapClimateView(appModel, placeInOrder, (ClimateView) view, placeModel);
            }
            appModel.getPlaces().add(placeModel);
        }
    }

    private void mapClimateView(HomeViewModel appModel, Place placeInOrder, ClimateView view, HomeViewPlaceModel placeModel) {

        placeModel.getValues().add(mapTemperature(appModel, placeInOrder, view));
        if (StringUtils.isNotBlank(view.getStateHumidity())) {
            placeModel.getValues().add(mapHumidity(appModel, placeInOrder, view));
        }
    }

    private Set<Place> lookupPlacesOrder(String viewTarget) {

        Set<Place> placesOrder = new LinkedHashSet<>();
        switch (viewTarget) {
        case "watch":
            placesOrder.add(Place.LIVINGROOM);
            placesOrder.add(Place.BATHROOM);
            placesOrder.add(Place.KIDSROOM);
            break;
        case "widget":
            break;
        default:
            // none
        }
        return placesOrder;
    }

    private HomeViewPlaceModel lookupPlaceModel(HomeViewModel appModel, Place placeInOrder) {

        HomeViewPlaceModel placeModel = null;
        // search for existing place model in target root model
        for (HomeViewPlaceModel placeModelSearch : appModel.getPlaces()) {
            if (placeModelSearch.getId().equals(placeInOrder.name())) {
                placeModel = placeModelSearch;
                break;
            }
        }
        // if no existing place model is found, create a new one
        if (placeModel == null) {
            placeModel = appModel.new HomeViewPlaceModel();
            placeModel.setId(placeInOrder.name());
            placeModel.setName(placeInOrder.getPlaceName());
        }
        return placeModel;
    }

    private HomeViewValueModel mapTemperature(HomeViewModel appModel, Place place, ClimateView view) {
        HomeViewValueModel hvm = appModel.new HomeViewValueModel();
        hvm.setId(place.getPlaceName() + "#temp");
        hvm.setKey("Temperatur");
        hvm.setValue(view.getStateTemperature());
        hvm.setAccent(mapAccent(view.getColorClass()));
        return hvm;
    }

    private HomeViewValueModel mapHumidity(HomeViewModel appModel, Place place, ClimateView view) {
        HomeViewValueModel hvm = appModel.new HomeViewValueModel();
        hvm.setId(place.getPlaceName() + "#humi");
        hvm.setKey("Feuchtigkeit");
        hvm.setValue(view.getStateHumidity());
        hvm.setAccent(mapAccent(view.getColorClassHumidity()));
        return hvm;
    }

    private String mapAccent(String colorClass) {

        switch (colorClass) {
        case HouseViewService.COLOR_CLASS_GREEN:
            return "66ff66";
        case HouseViewService.COLOR_CLASS_ORANGE:
            return "ffb84d";
        case HouseViewService.COLOR_CLASS_RED:
            return "ff6666";
        default:
            return "bebebe";
        }
    }
}
