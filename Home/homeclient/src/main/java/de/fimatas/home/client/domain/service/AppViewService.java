package de.fimatas.home.client.domain.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import de.fimatas.home.client.domain.model.ClimateView;
import de.fimatas.home.client.domain.model.LockView;
import de.fimatas.home.client.domain.model.PowerView;
import de.fimatas.home.client.domain.model.SwitchView;
import de.fimatas.home.client.domain.model.View;
import de.fimatas.home.client.domain.model.WindowSensorView;
import de.fimatas.home.client.model.HomeViewModel;
import de.fimatas.home.client.model.HomeViewModel.HomeViewActionModel;
import de.fimatas.home.client.model.HomeViewModel.HomeViewPlaceModel;
import de.fimatas.home.client.model.HomeViewModel.HomeViewValueModel;
import de.fimatas.home.client.request.HomeRequestMapping;
import de.fimatas.home.library.domain.model.Place;
import de.fimatas.home.library.domain.model.Tendency;

@Component
public class AppViewService {

    public HomeViewModel mapAppModel(Model model, String viewTarget) {

        Set<Place> placesOrder = lookupPlacesOrder(viewTarget);
        HomeViewModel appModel = newEmptyModel();

        for (Place placeInOrder : placesOrder) {
            for (Object value : model.asMap().values()) {
                if (value instanceof View) {
                    mapView(appModel, placeInOrder, value);
                }
            }
        }

        return appModel;
    }

    public HomeViewModel newEmptyModel() {

        HomeViewModel appModel = new HomeViewModel();
        appModel.setTimestamp(HomeRequestMapping.TS_FORMATTER.format(LocalDateTime.now()));
        appModel.setDefaultAccent(mapAccent(""));
        return appModel;
    }

    private void mapView(HomeViewModel appModel, Place placeInOrder, Object value) {

        View view = (View) value;
        if (view.getPlace().equals(placeInOrder.getPlaceName())) {
            HomeViewPlaceModel placeModel = lookupPlaceModel(appModel, placeInOrder);
            if (view instanceof ClimateView) {
                mapClimateView(placeInOrder, (ClimateView) view, placeModel);
            } else if (view instanceof PowerView) {
                mapPowerView(placeInOrder, (PowerView) view, placeModel);
            } else if (view instanceof LockView) {
                mapLockView(placeInOrder, (LockView) view, placeModel);
            } else if (view instanceof SwitchView) {
                mapSwitchView(placeInOrder, (SwitchView) view, placeModel);
            } else if (view instanceof WindowSensorView) {
                mapWindowView(placeInOrder, (WindowSensorView) view, placeModel);
            }
        }
    }

    private void mapSwitchView(Place placeInOrder, SwitchView view, HomeViewPlaceModel placeModel) {

        placeModel.setName(placeInOrder.getPlaceName());
        placeModel.getValues().add(mapSwitchStatus(placeInOrder, view));
        placeModel.getActions().addAll(mapSwitchActions(placeInOrder, view));
    }

    private void mapWindowView(Place placeInOrder, WindowSensorView view, HomeViewPlaceModel placeModel) {

        placeModel.setName(placeInOrder.getPlaceName());
        placeModel.getValues().add(mapWindowStatus(placeInOrder, view));

    }

    private void mapLockView(Place placeInOrder, LockView view, HomeViewPlaceModel placeModel) {
        placeModel.setName("Haustür");
        placeModel.getValues().add(mapLockStatus(placeInOrder, view));
        placeModel.getActions().addAll(mapLockActions(placeInOrder, view));
    }

    private void mapPowerView(Place placeInOrder, PowerView view, HomeViewPlaceModel placeModel) {

        if (placeInOrder == Place.HOUSE) {
            placeModel.setName("Strom Gesamt");
        } else {
            placeModel.setName(placeInOrder.getPlaceName());
        }
        placeModel.getValues().add(mapActualPower(placeInOrder, view));
        placeModel.getValues().add(mapTodayPower(placeInOrder, view));
    }

    private void mapClimateView(Place placeInOrder, ClimateView view, HomeViewPlaceModel placeModel) {

        placeModel.setName(placeInOrder.getPlaceName());
        placeModel.getValues().add(mapTemperature(placeInOrder, view));
        if (StringUtils.isNotBlank(view.getStateHumidity())) {
            placeModel.getValues().add(mapHumidity(placeInOrder, view));
        }
    }

    private Set<Place> lookupPlacesOrder(String viewTarget) {

        Set<Place> placesOrder = new LinkedHashSet<>();
        switch (viewTarget) {
        case "watch":
            placesOrder.add(Place.LIVINGROOM);
            placesOrder.add(Place.KITCHEN);
            placesOrder.add(Place.BATHROOM);
            placesOrder.add(Place.KIDSROOM);
            placesOrder.add(Place.BEDROOM);
            placesOrder.add(Place.LAUNDRY);
            placesOrder.add(Place.GUESTROOM);
            placesOrder.add(Place.WORKSHOP);
            placesOrder.add(Place.OUTSIDE);
            placesOrder.add(Place.FRONTDOOR);
            placesOrder.add(Place.HOUSE);
            placesOrder.add(Place.WALLBOX);
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
            // name is set in mapper
            appModel.getPlaces().add(placeModel);
        }
        return placeModel;
    }

    private HomeViewValueModel mapTemperature(Place place, ClimateView view) {
        HomeViewValueModel hvm = new HomeViewModel().new HomeViewValueModel();
        hvm.setId(place.getPlaceName() + "#temp");
        hvm.setKey("Wärme");
        hvm.setValue(view.getStateTemperature());
        hvm.setAccent(mapAccent(view.getColorClass()));
        hvm.setTendency(Tendency.nameFromCssClass(view.getTendencyIconTemperature()));
        return hvm;
    }

    private HomeViewValueModel mapHumidity(Place place, ClimateView view) {
        HomeViewValueModel hvm = new HomeViewModel().new HomeViewValueModel();
        hvm.setId(place.getPlaceName() + "#humi");
        hvm.setKey("Feuchte");
        hvm.setValue(view.getStateHumidity());
        hvm.setAccent(mapAccent(view.getColorClassHumidity()));
        hvm.setTendency(Tendency.nameFromCssClass(view.getTendencyIconHumidity()));
        return hvm;
    }

    private HomeViewValueModel mapActualPower(Place place, PowerView view) {
        HomeViewValueModel hvm = new HomeViewModel().new HomeViewValueModel();
        hvm.setId(place.getPlaceName() + "#actPowerSum");
        hvm.setKey("Aktuell");
        hvm.setValue(view.getState().replace("Watt", "W"));
        hvm.setAccent(mapAccent(view.getColorClass()));
        hvm.setTendency(Tendency.nameFromCssClass(view.getTendencyIcon()));
        return hvm;
    }

    private HomeViewValueModel mapTodayPower(Place place, PowerView view) {
        HomeViewValueModel hvm = new HomeViewModel().new HomeViewValueModel();
        hvm.setId(place.getPlaceName() + "#todayPowerSum");
        hvm.setKey("Heute");
        if (BooleanUtils.toBoolean(view.getUnreach())) {
            hvm.setValue(StringUtils.EMPTY);
        } else if (view.getTodayConsumption() == null) {
            hvm.setValue("0" + ViewFormatter.K_W_H);
        }else {
            hvm.setValue(view.getTodayConsumption().getLabel().replace(ViewFormatter.SUM_SIGN, "").trim());
        }
        hvm.setAccent(mapAccent(view.getColorClass()));
        return hvm;
    }

    private HomeViewValueModel mapLockStatus(Place place, LockView view) {
        HomeViewValueModel hvm = new HomeViewModel().new HomeViewValueModel();
        hvm.setId(place.getPlaceName() + "#lockStatus");
        hvm.setKey("Zustand");
        hvm.setValue(Boolean.TRUE.toString().equalsIgnoreCase(view.getBusy()) ? ". . ." : view.getState());
        hvm.setAccent(mapAccent(view.getColorClass()));
        return hvm;
    }

    private List<List<HomeViewActionModel>> mapLockActions(Place place, LockView view) {

        if (BooleanUtils.toBoolean(view.getUnreach())) {
            return new LinkedList<>();
        }

        List<HomeViewActionModel> actionsState = new LinkedList<>();
        HomeViewActionModel actionLock = new HomeViewModel().new HomeViewActionModel();
        actionLock.setId(place.getPlaceName() + "#lockActionLock");
        actionLock.setName("Verriegeln");
        actionLock.setLink(view.getLinkLock());
        actionsState.add(actionLock);
        HomeViewActionModel actionUnlock = new HomeViewModel().new HomeViewActionModel();
        actionUnlock.setId(place.getPlaceName() + "#lockActionUnlock");
        actionUnlock.setName("Entriegeln");
        actionUnlock.setLink(view.getLinkUnlock());
        actionsState.add(actionUnlock);
        HomeViewActionModel actionOpen = new HomeViewModel().new HomeViewActionModel();
        actionOpen.setId(place.getPlaceName() + "#lockActionOpen");
        actionOpen.setName("Öffnen");
        actionOpen.setLink(view.getLinkOpen());
        actionsState.add(actionOpen);
        List<HomeViewActionModel> actionsControl = new LinkedList<>();
        HomeViewActionModel actionAuto = new HomeViewModel().new HomeViewActionModel();
        actionAuto.setId(place.getPlaceName() + "#lockActionAuto");
        actionAuto.setName("Automatisch");
        actionAuto.setLink(view.getLinkAuto());
        actionsControl.add(actionAuto);
        HomeViewActionModel actionManu = new HomeViewModel().new HomeViewActionModel();
        actionManu.setId(place.getPlaceName() + "#lockActionManu");
        actionManu.setName("Manuell");
        actionManu.setLink(view.getLinkManual());
        actionsControl.add(actionManu);
        HomeViewActionModel actionEvent = new HomeViewModel().new HomeViewActionModel();
        actionEvent.setId(place.getPlaceName() + "#lockActionEvent");
        actionEvent.setName("Ereignis");
        actionEvent.setLink(view.getLinkAutoEvent());
        actionsControl.add(actionEvent);
        List<List<HomeViewActionModel>> actions = new LinkedList<>();
        actions.add(actionsState);
        actions.add(actionsControl);
        return actions;
    }

    private HomeViewValueModel mapSwitchStatus(Place place, SwitchView view) {
        HomeViewValueModel hvm = new HomeViewModel().new HomeViewValueModel();
        hvm.setId(place.getPlaceName() + "#switchStatus");
        hvm.setKey(view.getShortName());
        hvm.setValue(view.getStateShort());
        hvm.setAccent(mapAccent(view.getColorClass()));
        return hvm;
    }

    private HomeViewValueModel mapWindowStatus(Place place, WindowSensorView view) {
        HomeViewValueModel hvm = new HomeViewModel().new HomeViewValueModel();
        hvm.setId(place.getPlaceName() + "#windowStatus");
        hvm.setKey(view.getShortName());
        hvm.setValue(view.getStateShort());
        hvm.setAccent(mapAccent(view.getColorClass()));
        return hvm;
    }

    private List<List<HomeViewActionModel>> mapSwitchActions(Place place, SwitchView view) {

        if (BooleanUtils.toBoolean(view.getUnreach())) {
            return new LinkedList<>();
        }

        List<HomeViewActionModel> actionsOnOff = new LinkedList<>();
        HomeViewActionModel actionOn = new HomeViewModel().new HomeViewActionModel();
        actionOn.setId(place.getPlaceName() + "#switchActionOn");
        actionOn.setName("Ein");
        actionOn.setLink(view.getLinkOn());
        actionsOnOff.add(actionOn);
        HomeViewActionModel actionOff = new HomeViewModel().new HomeViewActionModel();
        actionOff.setId(place.getPlaceName() + "#switchActionOff");
        actionOff.setName("Aus");
        actionOff.setLink(view.getLinkOff());
        actionsOnOff.add(actionOff);
        List<HomeViewActionModel> actionsControl = new LinkedList<>();
        HomeViewActionModel actionAuto = new HomeViewModel().new HomeViewActionModel();
        actionAuto.setId(place.getPlaceName() + "#switchActionAuto");
        actionAuto.setName("Automatisch");
        actionAuto.setLink(view.getLinkAuto());
        actionsControl.add(actionAuto);
        HomeViewActionModel actionManu = new HomeViewModel().new HomeViewActionModel();
        actionManu.setId(place.getPlaceName() + "#switchActionManu");
        actionManu.setName("Manuell");
        actionManu.setLink(view.getLinkManual());
        actionsControl.add(actionManu);
        List<List<HomeViewActionModel>> actions = new LinkedList<>();
        actions.add(actionsOnOff);
        actions.add(actionsControl);
        return actions;
    }

    private String mapAccent(String colorClass) {

        switch (colorClass) {
        case HouseViewService.COLOR_CLASS_GREEN:
            return "66ff66";
        case HouseViewService.COLOR_CLASS_ORANGE:
            return "ffb84d";
        case HouseViewService.COLOR_CLASS_RED:
            return "ff6666";
        case HouseViewService.COLOR_CLASS_BLUE:
            return "66b3ff";
        default:
            return "bebebe";
        }
    }
}
