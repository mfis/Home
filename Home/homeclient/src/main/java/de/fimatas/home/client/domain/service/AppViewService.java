package de.fimatas.home.client.domain.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.fimatas.home.client.domain.model.*;
import de.fimatas.home.library.domain.model.HeatpumpPreset;
import de.fimatas.home.library.model.ConditionColor;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import de.fimatas.home.client.model.HomeViewModel;
import de.fimatas.home.client.model.HomeViewModel.HomeViewActionModel;
import de.fimatas.home.client.model.HomeViewModel.HomeViewPlaceModel;
import de.fimatas.home.client.model.HomeViewModel.HomeViewValueModel;
import de.fimatas.home.client.request.HomeRequestMapping;
import de.fimatas.home.library.domain.model.Place;
import de.fimatas.home.library.domain.model.Tendency;

import javax.annotation.PostConstruct;

@Component
public class AppViewService {

    private final Map<AppViewTarget, Set<PlaceDirectives>> targetPlaceDirectives = new HashMap<>();

    @PostConstruct
    void placesDirectives() {

        Set<PlaceDirectives> compliaction = new LinkedHashSet<>();
        targetPlaceDirectives.put(AppViewTarget.COMPLICATION, compliaction);

        compliaction.add(new PlaceDirectives(Place.OUTSIDE, PlaceDirective.WATCH_SYMBOL));

        Set<PlaceDirectives> watch = new LinkedHashSet<>();
        targetPlaceDirectives.put(AppViewTarget.WATCH, watch);

        watch.add(new PlaceDirectives(Place.OUTSIDE, PlaceDirective.WATCH_LABEL, PlaceDirective.WATCH_SYMBOL));
        watch.add(new PlaceDirectives(Place.FRONTDOOR, PlaceDirective.WATCH_LABEL));
        watch.add(new PlaceDirectives(Place.LIVINGROOM, PlaceDirective.WATCH_LABEL));
        watch.add(new PlaceDirectives(Place.KITCHEN, PlaceDirective.WATCH_LABEL));
        watch.add(new PlaceDirectives(Place.KIDSROOM_1, PlaceDirective.WATCH_LABEL));
        watch.add(new PlaceDirectives(Place.KIDSROOM_2, PlaceDirective.WATCH_LABEL));
        watch.add(new PlaceDirectives(Place.BEDROOM, PlaceDirective.WATCH_LABEL));
        watch.add(new PlaceDirectives(Place.BATHROOM, PlaceDirective.WATCH_LABEL));
        watch.add(new PlaceDirectives(Place.LAUNDRY, PlaceDirective.WATCH_LABEL));
        watch.add(new PlaceDirectives(Place.GUESTROOM, PlaceDirective.WATCH_LABEL));
        watch.add(new PlaceDirectives(Place.WORKSHOP, PlaceDirective.WATCH_LABEL));
        watch.add(new PlaceDirectives(Place.HOUSE, PlaceDirective.WATCH_LABEL));
        watch.add(new PlaceDirectives(Place.WALLBOX, PlaceDirective.WATCH_LABEL));

        Set<PlaceDirectives> widget = new LinkedHashSet<>();
        targetPlaceDirectives.put(AppViewTarget.WIDGET, widget);

        widget.add(new PlaceDirectives(Place.OUTSIDE, PlaceDirective.WIDGET_LABEL_SMALL, PlaceDirective.WIDGET_LABEL_MEDIUM, PlaceDirective.WIDGET_LABEL_LARGE, PlaceDirective.WIDGET_SYMBOL));
        widget.add(new PlaceDirectives(Place.UPPER_FLOOR_TEMPERATURE, PlaceDirective.WIDGET_LABEL_SMALL, PlaceDirective.WIDGET_LABEL_MEDIUM, PlaceDirective.WIDGET_LABEL_LARGE));
        widget.add(new PlaceDirectives(Place.FRONTDOOR, PlaceDirective.WIDGET_SYMBOL));
    }

    public HomeViewModel mapAppModel(Model model, AppViewTarget viewTarget) {

        HomeViewModel appModel = newEmptyModel();

        for (PlaceDirectives placeDirectives : targetPlaceDirectives.get(viewTarget)) {
            for (Object value : model.asMap().values()) {
                if (value instanceof View) {
                    mapView(appModel, placeDirectives, value, model, viewTarget);
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

    private void mapView(HomeViewModel appModel, PlaceDirectives placeDirectives, Object value, Model completeModel, AppViewTarget viewTarget) {

        View view = (View) value;
        if (view.getPlaceID().equals(placeDirectives.place.name())) {
            HomeViewPlaceModel placeModel = lookupPlaceModel(appModel, placeDirectives, completeModel);
            if (view instanceof ClimateView) {
                mapClimateView(placeDirectives, (ClimateView) view, placeModel, viewTarget);
            } else if (view instanceof PowerView) {
                mapPowerView(placeDirectives, (PowerView) view, placeModel, viewTarget);
            } else if (view instanceof LockView) {
                mapLockView(placeDirectives, (LockView) view, placeModel, viewTarget);
            } else if (view instanceof SwitchView) {
                mapSwitchView(placeDirectives, (SwitchView) view, placeModel, viewTarget);
            } else if (view instanceof WindowSensorView) {
                mapWindowView(placeDirectives, (WindowSensorView) view, placeModel, viewTarget);
            } else if (view instanceof WeatherForecastsView) {
                mapWeatherForecastsView(placeDirectives, (WeatherForecastsView) view, placeModel, viewTarget);
            } else if (view instanceof PresenceView) {
                mapPresenceView(placeDirectives, (PresenceView) view, placeModel, viewTarget);
            } else if(view instanceof HeatpumpView){
                mapHeatpumpView(placeDirectives, (HeatpumpView) view, placeModel, viewTarget);
            }
        }
    }

    private void mapSwitchView(PlaceDirectives placeDirectives, SwitchView view, HomeViewPlaceModel placeModel, AppViewTarget viewTarget) {

        placeModel.getValues().add(mapSwitchStatus(placeDirectives, view));
        placeModel.getActions().addAll(mapSwitchActions(placeDirectives, view));
    }

    private void mapWindowView(PlaceDirectives placeDirectives, WindowSensorView view, HomeViewPlaceModel placeModel, AppViewTarget viewTarget) {

        placeModel.getValues().add(mapWindowStatus(placeDirectives, view));
    }

    private void mapLockView(PlaceDirectives placeDirectives, LockView view, HomeViewPlaceModel placeModel, AppViewTarget viewTarget) {
        if(directiveContainsOnly(placeDirectives, PlaceDirective.WIDGET_SYMBOL) && isColorClassOrangeOrRed(view)){
            placeModel.setName("HaustuerOrangeOrRed");
            placeModel.getValues().add(mapLockStatus(placeDirectives, view));
        }else{
            placeModel.setName("Haus");
            placeModel.getValues().add(mapLockStatus(placeDirectives, view));
            if(viewTarget == AppViewTarget.WATCH){
                placeModel.getActions().addAll(mapLockActions(placeDirectives, view));
            }
        }
    }

    private void mapPowerView(PlaceDirectives placeDirectives, PowerView view, HomeViewPlaceModel placeModel, AppViewTarget viewTarget) {

        if (placeDirectives.place == Place.HOUSE) {
            placeModel.setName("Strom Gesamt");
        }
        placeModel.getValues().add(mapActualPower(placeDirectives, view));
        placeModel.getValues().add(mapTodayPower(placeDirectives, view));
    }

    private void mapClimateView(PlaceDirectives placeDirectives, ClimateView view, HomeViewPlaceModel placeModel, AppViewTarget viewTarget) {

        placeModel.getValues().add(mapTemperature(placeDirectives, view));
        if (StringUtils.isNotBlank(view.getStateHumidity()) && viewTarget != AppViewTarget.COMPLICATION) {
            placeModel.getValues().add(mapHumidity(placeDirectives, view));
        }
    }

    private void mapWeatherForecastsView(PlaceDirectives placeDirectives, WeatherForecastsView view, HomeViewPlaceModel placeModel, AppViewTarget viewTarget) {

        if(viewTarget == AppViewTarget.COMPLICATION){
            return;
        }

        placeModel.getValues().add(mapForecastTemperature(placeDirectives, view));
        if (StringUtils.isNotBlank(view.getStateEventWatch())) {
            placeModel.getValues().add(mapForecastEvent(placeDirectives, view));
        }
    }

    private void mapPresenceView(PlaceDirectives placeDirectives, PresenceView view, HomeViewPlaceModel placeModel, AppViewTarget viewTarget) {

        if(viewTarget == AppViewTarget.WATCH){
            placeModel.getValues().add(mapPresence(placeDirectives, view));
        }
    }

    private void mapHeatpumpView(PlaceDirectives placeDirectives, HeatpumpView view, HomeViewPlaceModel placeModel, AppViewTarget viewTarget) {

        placeModel.getValues().add(mapHeatpump(placeDirectives, view));
        if(!Boolean.parseBoolean(view.getBusy()) && !Boolean.parseBoolean(view.getUnreach())) {
            placeModel.getActions().addAll(mapHeatpumpActions(placeDirectives, view));
        }
    }

    private HomeViewPlaceModel lookupPlaceModel(HomeViewModel appModel, PlaceDirectives placeDirectives, Model completeModel) {

        HomeViewPlaceModel placeModel = null;
        // search for existing place model in target root model
        for (HomeViewPlaceModel placeModelSearch : appModel.getPlaces()) {
            if (placeModelSearch.getId().equals(placeDirectives.place.name())) {
                placeModel = placeModelSearch;
                break;
            }
        }
        // if no existing place model is found, create a new one
        if (placeModel == null) {
            placeModel = new HomeViewPlaceModel();
            placeModel.setId(placeDirectives.place.name());
            placeModel.getPlaceDirectives().addAll(placeDirectives.directives.stream().map(Enum::name).collect(Collectors.toList()));
            var subtitleKey = HouseViewService.PLACE_SUBTITLE_PREFIX + placeDirectives.place.name();
            if(completeModel.getAttribute(subtitleKey)!=null){
                placeModel.setName((String) completeModel.getAttribute(subtitleKey));
            }else{
                placeModel.setName(placeDirectives.place.getPlaceName());
            }
            // name is set in mapper
            appModel.getPlaces().add(placeModel);
        }
        return placeModel;
    }

    private HomeViewValueModel mapTemperature(PlaceDirectives placeDirectives, ClimateView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(placeDirectives.place.name() + "#temp");
        hvm.getValueDirectives().addAll(Stream.of(ValueDirective.SYMBOL_SKIP).map(Enum::name).collect(Collectors.toList()));
        hvm.setKey("Wärme");
        hvm.setValue(view.getStateTemperature());
        hvm.setValueShort(view.getStateShort());
        hvm.setAccent(mapAccent(view.getColorClass()));
        hvm.setTendency(Tendency.nameFromCssClass(view.getTendencyIconTemperature()));
        return hvm;
    }

    private HomeViewValueModel mapHumidity(PlaceDirectives placeDirectives, ClimateView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(placeDirectives.place.name() + "#humi");
        hvm.getValueDirectives().addAll(Stream.of(ValueDirective.SYMBOL_SKIP, ValueDirective.WIDGET_SKIP).map(Enum::name).collect(Collectors.toList()));
        hvm.setKey("Feuchte");
        hvm.setValue(view.getStateHumidity());
        hvm.setAccent(mapAccent(view.getColorClassHumidity()));
        hvm.setTendency(Tendency.nameFromCssClass(view.getTendencyIconHumidity()));
        return hvm;
    }

    private HomeViewValueModel mapForecastTemperature(PlaceDirectives placeDirectives, WeatherForecastsView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(placeDirectives.place.name() + "#fcTemp");
        hvm.getValueDirectives().addAll(Stream.of(ValueDirective.SYMBOL_SKIP).map(Enum::name).collect(Collectors.toList()));
        hvm.setKey("2-Tage");
        hvm.setValue(view.getStateTemperatureWatch());
        hvm.setAccent(mapAccent(view.getColorClass()));
        return hvm;
    }

    private HomeViewValueModel mapPresence(PlaceDirectives placeDirectives, PresenceView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(placeDirectives.place.name() + "#presence");
        hvm.setKey(view.getName());
        hvm.setValue(view.getStateShort());
        hvm.setAccent(mapAccent(view.getColorClass()));
        return hvm;
    }

    private HomeViewValueModel mapHeatpump(PlaceDirectives placeDirectives, HeatpumpView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(placeDirectives.place.name() + "#heatpump");
        hvm.setKey("W-Pumpe");
        if(Boolean.parseBoolean(view.getBusy())) {
            hvm.setValue("...\u21BB...");
            hvm.setAccent(Strings.EMPTY);
        }else if(Boolean.parseBoolean(view.getUnreach())) {
            hvm.setValue("???");
            hvm.setAccent(Strings.EMPTY);
        }else{
            hvm.setValue(view.getStateShort());
            hvm.setAccent(mapAccent(view.getColorClass()));
        }
        return hvm;
    }

    private HomeViewValueModel mapForecastEvent(PlaceDirectives placeDirectives, WeatherForecastsView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(placeDirectives.place.name() + "#fcEvent");
        hvm.getValueDirectives().addAll(Stream.of(ValueDirective.WIDGET_SKIP).map(Enum::name).collect(Collectors.toList()));
        hvm.setKey("Ereignis");
        hvm.setValue(view.getStateEventWatch());
        hvm.setAccent(mapAccent(view.getColorClass()));
        hvm.setSymbol(view.getIconNativeClient());
        return hvm;
    }

    private HomeViewValueModel mapActualPower(PlaceDirectives placeDirectives, PowerView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(placeDirectives.place.name() + "#actPowerSum");
        hvm.setKey("Aktuell");
        hvm.setValue(view.getState().replace("Watt", "W"));
        hvm.setAccent(mapAccent(view.getColorClass()));
        hvm.setTendency(Tendency.nameFromCssClass(view.getTendencyIcon()));
        return hvm;
    }

    private HomeViewValueModel mapTodayPower(PlaceDirectives placeDirectives, PowerView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(placeDirectives.place.name() + "#todayPowerSum");
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

    private HomeViewValueModel mapLockStatus(PlaceDirectives placeDirectives, LockView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(placeDirectives.place.name() + "#lockStatus");
        hvm.setKey("Tür");
        hvm.setValue(Boolean.TRUE.toString().equalsIgnoreCase(view.getBusy()) ? ". . ." : view.getState());
        hvm.setAccent(mapAccent(view.getColorClass()));
        return hvm;
    }

    private List<List<HomeViewActionModel>> mapLockActions(PlaceDirectives placeDirectives, LockView view) {

        if (BooleanUtils.toBoolean(view.getUnreach())) {
            return new LinkedList<>();
        }

        List<HomeViewActionModel> actionsState = new LinkedList<>();
        HomeViewActionModel actionSwitchCaption = new HomeViewActionModel();
        actionSwitchCaption.setId(placeDirectives.place.name() + "#lockStateCaption");
        actionSwitchCaption.setName("Tür Status");
        actionSwitchCaption.setLink(Strings.EMPTY);
        actionsState.add(actionSwitchCaption);
        HomeViewActionModel actionLock = new HomeViewActionModel();
        actionLock.setId(placeDirectives.place.name() + "#lockActionLock");
        actionLock.setName("Verriegeln");
        actionLock.setLink(view.getLinkLock());
        actionsState.add(actionLock);
        HomeViewActionModel actionUnlock = new HomeViewActionModel();
        actionUnlock.setId(placeDirectives.place.name() + "#lockActionUnlock");
        actionUnlock.setName("Entriegeln");
        actionUnlock.setLink(view.getLinkUnlock());
        actionsState.add(actionUnlock);
        HomeViewActionModel actionOpen = new HomeViewActionModel();
        actionOpen.setId(placeDirectives.place.name() + "#lockActionOpen");
        actionOpen.setName("Öffnen");
        actionOpen.setLink(view.getLinkOpen());
        actionsState.add(actionOpen);
        List<HomeViewActionModel> actionsControl = new LinkedList<>();
        HomeViewActionModel actionModeCaption = new HomeViewActionModel();
        actionModeCaption.setId(placeDirectives.place.name() + "#lockModeCaption");
        actionModeCaption.setName("Tür Modus");
        actionModeCaption.setLink(Strings.EMPTY);
        actionsControl.add(actionModeCaption);
        HomeViewActionModel actionAuto = new HomeViewActionModel();
        actionAuto.setId(placeDirectives.place.name() + "#lockActionAuto");
        actionAuto.setName("Automatisch");
        actionAuto.setLink(view.getLinkAuto());
        actionsControl.add(actionAuto);
        HomeViewActionModel actionManu = new HomeViewActionModel();
        actionManu.setId(placeDirectives.place.name() + "#lockActionManu");
        actionManu.setName("Manuell");
        actionManu.setLink(view.getLinkManual());
        actionsControl.add(actionManu);
        HomeViewActionModel actionEvent = new HomeViewActionModel();
        actionEvent.setId(placeDirectives.place.name() + "#lockActionEvent");
        actionEvent.setName("Ereignis");
        actionEvent.setLink(view.getLinkAutoEvent());
        actionsControl.add(actionEvent);
        List<List<HomeViewActionModel>> actions = new LinkedList<>();
        actions.add(actionsState);
        actions.add(actionsControl);
        return actions;
    }

    private HomeViewValueModel mapSwitchStatus(PlaceDirectives placeDirectives, SwitchView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(placeDirectives.place.name() + "#switchStatus");
        hvm.setKey(view.getShortName());
        hvm.setValue(view.getStateShort());
        hvm.setAccent(mapAccent(view.getColorClass()));
        return hvm;
    }

    private HomeViewValueModel mapWindowStatus(PlaceDirectives placeDirectives, WindowSensorView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(placeDirectives.place.name() + "#windowStatus");
        hvm.setKey(view.getShortName());
        hvm.setValue(view.getStateShort());
        hvm.setAccent(mapAccent(view.getColorClass()));
        return hvm;
    }

    private List<List<HomeViewActionModel>> mapSwitchActions(PlaceDirectives placeDirectives, SwitchView view) {

        if (BooleanUtils.toBoolean(view.getUnreach())) {
            return new LinkedList<>();
        }

        List<HomeViewActionModel> actionsOnOff = new LinkedList<>();
        HomeViewActionModel actionSwitchCaption = new HomeViewActionModel();
        actionSwitchCaption.setId(placeDirectives.place.name() + "#switchStateCaption");
        actionSwitchCaption.setName("Schalter Status");
        actionSwitchCaption.setLink(Strings.EMPTY);
        actionsOnOff.add(actionSwitchCaption);
        HomeViewActionModel actionOn = new HomeViewActionModel();
        actionOn.setId(placeDirectives.place.name() + "#switchActionOn");
        actionOn.setName("Ein");
        actionOn.setLink(view.getLinkOn());
        actionsOnOff.add(actionOn);
        HomeViewActionModel actionOff = new HomeViewActionModel();
        actionOff.setId(placeDirectives.place.name() + "#switchActionOff");
        actionOff.setName("Aus");
        actionOff.setLink(view.getLinkOff());
        actionsOnOff.add(actionOff);
        List<HomeViewActionModel> actionsControl = new LinkedList<>();
        HomeViewActionModel actionModeCaption = new HomeViewActionModel();
        actionModeCaption.setId(placeDirectives.place.name() + "#switchModeCaption");
        actionModeCaption.setName("Schalter Modus");
        actionModeCaption.setLink(Strings.EMPTY);
        actionsControl.add(actionModeCaption);
        HomeViewActionModel actionAuto = new HomeViewActionModel();
        actionAuto.setId(placeDirectives.place.name() + "#switchActionAuto");
        actionAuto.setName("Automatisch");
        actionAuto.setLink(view.getLinkAuto());
        actionsControl.add(actionAuto);
        HomeViewActionModel actionManu = new HomeViewActionModel();
        actionManu.setId(placeDirectives.place.name() + "#switchActionManu");
        actionManu.setName("Manuell");
        actionManu.setLink(view.getLinkManual());
        actionsControl.add(actionManu);
        List<List<HomeViewActionModel>> actions = new LinkedList<>();
        actions.add(actionsOnOff);
        actions.add(actionsControl);
        return actions;
    }

    private List<List<HomeViewActionModel>> mapHeatpumpActions(PlaceDirectives placeDirectives, HeatpumpView view) {

        if (BooleanUtils.toBoolean(view.getUnreach())) {
            return new LinkedList<>();
        }

        List<List<HomeViewActionModel>> actions = new LinkedList<>();

        // direct
        actions.add(mapHeatpumpActionsRoomCombination(placeDirectives, view, List.of()));

        // with one other room
        view.getOtherPlaces().forEach(other -> actions.add(mapHeatpumpActionsRoomCombination(placeDirectives, view, List.of(other))));

        // all
        actions.add(mapHeatpumpActionsRoomCombination(placeDirectives, view, view.getOtherPlaces()));

        return actions;
    }

    private List<HomeViewActionModel> mapHeatpumpActionsRoomCombination(PlaceDirectives placeDirectives, HeatpumpView view, List<ValueWithCaption> other) {

        List<HomeViewActionModel> actionsDirect = new LinkedList<>();

        HomeViewActionModel actionSwitchCaption = new HomeViewActionModel();

        var idSuffix = other.stream().map(ValueWithCaption::getValue).collect(Collectors.joining("#"));
        actionSwitchCaption.setId(placeDirectives.place.name() + "#hpSwitchesCaption#" + idSuffix);
        actionSwitchCaption.setName("Wärmepumpe\n" + (StringUtils.isBlank(view.getPlaceSubtitle()) ? view.getPlace() : "") + view.getPlaceSubtitle().trim());
        if(!other.isEmpty()){
            other.forEach(o -> actionSwitchCaption.setName(actionSwitchCaption.getName() + "\n" + (StringUtils.isNotBlank(o.getCssClass()) ? o.getCssClass().trim() : o.getCaption())));
        }
        actionSwitchCaption.setLink(Strings.EMPTY);
        actionsDirect.add(actionSwitchCaption);

        actionsDirect.add(mapHeatpumpActionSinglePreset(placeDirectives, view, other, HeatpumpPreset.COOL_AUTO, idSuffix));
        actionsDirect.add(mapHeatpumpActionSinglePreset(placeDirectives, view, other, HeatpumpPreset.COOL_MIN, idSuffix));
        actionsDirect.add(mapHeatpumpActionSinglePreset(placeDirectives, view, other, HeatpumpPreset.HEAT_AUTO, idSuffix));
        actionsDirect.add(mapHeatpumpActionSinglePreset(placeDirectives, view, other, HeatpumpPreset.HEAT_MIN, idSuffix));
        actionsDirect.add(mapHeatpumpActionSinglePreset(placeDirectives, view, other, HeatpumpPreset.FAN_AUTO, idSuffix));
        actionsDirect.add(mapHeatpumpActionSinglePreset(placeDirectives, view, other, HeatpumpPreset.FAN_MIN, idSuffix));
        actionsDirect.add(mapHeatpumpActionSinglePreset(placeDirectives, view, other, HeatpumpPreset.DRY_TIMER, idSuffix));
        actionsDirect.add(mapHeatpumpActionSinglePreset(placeDirectives, view, other, HeatpumpPreset.OFF, idSuffix));

        return actionsDirect;
    }

    private HomeViewActionModel mapHeatpumpActionSinglePreset(PlaceDirectives placeDirectives, HeatpumpView view, List<ValueWithCaption> other, HeatpumpPreset preset, String idSuffix) {

        HomeViewActionModel hpActionSwitch = new HomeViewActionModel();
        hpActionSwitch.setId(placeDirectives.place.name() + "#hpSwitch#" + preset + "#" + idSuffix);
        hpActionSwitch.setName(preset.getShortText());
        var link = "#";
        switch (preset){
            case COOL_AUTO:
                link = view.getLinkCoolAuto();
                break;
            case COOL_MIN:
                link = view.getLinkCoolMin();
                break;
            case HEAT_AUTO:
                link = view.getLinkHeatAuto();
                break;
            case HEAT_MIN:
                link = view.getLinkHeatMin();
                break;
            case FAN_AUTO:
                link = view.getLinkFanAuto();
                break;
            case FAN_MIN:
                link = view.getLinkFanMin();
                break;
            case DRY_TIMER:
                link = view.getLinkTimer();
                break;
            case OFF:
                link = view.getLinkOff();
                break;
        }
        hpActionSwitch.setLink(link);
        if(!other.isEmpty() && !link.equals("#")){
            other.forEach(o -> hpActionSwitch.setLink(hpActionSwitch.getLink() + o.getValue() + ","));
        }
        return hpActionSwitch;
    }

    private boolean directiveContainsOnly(PlaceDirectives placeDirectives, @SuppressWarnings("SameParameterValue") PlaceDirective directive){
        return placeDirectives.directives.size()==1 && placeDirectives.directives.contains(directive);
    }

    private boolean isColorClassOrangeOrRed(View view){
        return view.getColorClass().equalsIgnoreCase(ConditionColor.ORANGE.getUiClass())
                || view.getColorClass().equalsIgnoreCase(ConditionColor.RED.getUiClass());
    }

    private String mapAccent(String colorClass) {

        final ConditionColor conditionColor = ConditionColor.fromUiName(colorClass);
        if(conditionColor==null){
            return StringUtils.EMPTY;
        }

        switch (conditionColor) {
        case GREEN:
            return ".green";
        case ORANGE:
            return ".orange";
        case RED:
            return ".red";
        case BLUE:
            return ".blue";
        case LIGHT:
        case COLD:
            return ".purple";
        default:
            return StringUtils.EMPTY;
        }
    }

    public enum AppViewTarget{
        WATCH, COMPLICATION, WIDGET
    }

    private enum PlaceDirective {
        // Watch
        WATCH_LABEL, WATCH_SYMBOL,
        // Widget
        WIDGET_LABEL_SMALL, WIDGET_LABEL_MEDIUM, WIDGET_LABEL_LARGE, WIDGET_SYMBOL,
        //
    }

    private enum ValueDirective {
        SYMBOL_SKIP, WIDGET_SKIP
    }

    private static class PlaceDirectives{
        public Place place;
        public List<PlaceDirective> directives;
        public PlaceDirectives(Place place, PlaceDirective... directives){
            this.place = place;
            this.directives = List.of(directives);
        }
    }
}
