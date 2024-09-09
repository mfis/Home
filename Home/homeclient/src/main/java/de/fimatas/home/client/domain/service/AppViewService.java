package de.fimatas.home.client.domain.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.fimatas.home.client.domain.model.*;
import de.fimatas.home.library.domain.model.HeatpumpPreset;
import de.fimatas.home.library.homematic.model.Type;
import de.fimatas.home.library.model.ConditionColor;
import de.fimatas.home.library.util.ViewFormatterUtils;
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

import jakarta.annotation.PostConstruct;

@SuppressWarnings({"unused", "ExtractMethodRecommender"})
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
        watch.add(new PlaceDirectives(Place.ELECTROVEHICLE, PlaceDirective.WATCH_LABEL));

        Set<PlaceDirectives> widget = new LinkedHashSet<>();
        targetPlaceDirectives.put(AppViewTarget.WIDGET, widget);

        widget.add(new PlaceDirectives(Place.OUTSIDE, PlaceDirective.WIDGET_LABEL_SMALL, PlaceDirective.WIDGET_LABEL_MEDIUM, PlaceDirective.WIDGET_LABEL_LARGE, PlaceDirective.WIDGET_SYMBOL, PlaceDirective.WIDGET_LOCKSCREEN_CIRCULAR));
        widget.add(new PlaceDirectives(Place.WIDGET_UPPER_FLOOR_TEMPERATURE, PlaceDirective.WIDGET_LABEL_SMALL, PlaceDirective.WIDGET_LABEL_MEDIUM, PlaceDirective.WIDGET_LABEL_LARGE));
        widget.add(new PlaceDirectives(Place.WIDGET_GRIDS, PlaceDirective.WIDGET_LABEL_MEDIUM, PlaceDirective.WIDGET_LABEL_LARGE));
        widget.add(new PlaceDirectives(Place.WIDGET_ENERGY, PlaceDirective.WIDGET_LABEL_MEDIUM, PlaceDirective.WIDGET_LABEL_LARGE));
        widget.add(new PlaceDirectives(Place.WIDGET_SYMBOLS, PlaceDirective.WIDGET_SYMBOL));
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
        appModel.setDefaultAccent(ViewFormatterUtils.mapAppColorAccent(""));
        return appModel;
    }

    private void mapView(HomeViewModel appModel, PlaceDirectives placeDirectives, Object value, Model completeModel, AppViewTarget viewTarget) {

        View view = (View) value;
        if (view.getPlaceID().equals(placeDirectives.place.name())) {
            HomeViewPlaceModel placeModel = lookupPlaceModel(appModel, placeDirectives, completeModel);
            if (view instanceof ClimateView) {
                mapClimateView(placeDirectives, (ClimateView) view, placeModel, viewTarget);
            } else if (view instanceof WidgetGroupView) {
                mapGroupView(placeDirectives, (WidgetGroupView) view, placeModel, viewTarget);
            } else if (view instanceof PowerView) {
                mapPowerView(placeDirectives, (PowerView) view, placeModel, Optional.empty(), null);
            } else if (view instanceof OverallElectricPowerHouseView) {
                mapPowerView(placeDirectives, ((OverallElectricPowerHouseView) view).getGridPurchase(), placeModel, Optional.of("Bez"), ConditionColor.ORANGE.getUiClass());
                mapPowerView(placeDirectives, ((OverallElectricPowerHouseView) view).getGridFeed(), placeModel, Optional.of("Ein"), ConditionColor.GREEN.getUiClass());
                mapPV(((OverallElectricPowerHouseView) view).getPv(), placeModel, "PV");
                mapPV(((OverallElectricPowerHouseView) view).getGridFeed(), placeModel, "Stromnetz");
                mapPVBattery(((OverallElectricPowerHouseView) view), placeModel);
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
            } else if(view instanceof LightsView){
                mapLightsView(placeDirectives, (LightsView) view, placeModel, viewTarget);
            } else if(view instanceof ChargingView){
                mapChargingView(placeDirectives, (ChargingView) view, placeModel, viewTarget);
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

    private void mapPowerView(PlaceDirectives placeDirectives, PowerView view, HomeViewPlaceModel placeModel, Optional<String> direction, String overrideColorClass) {

        if (placeDirectives.place == Place.HOUSE) {
            placeModel.setName("Netze / PV");
        }
        placeModel.getValues().add(mapTodayPower(placeDirectives, view, direction, overrideColorClass));
    }

    private void mapPV(PowerView view, HomeViewPlaceModel placeModel, String caption) {

        placeModel.getValues().add(mapActualPV(view, caption));
    }

    private void mapPVBattery(OverallElectricPowerHouseView view, HomeViewPlaceModel placeModel) {

        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId("pvbattery");
        hvm.setKey("Speicher");
        hvm.setValue(view.getBatteryStateOfCharge());
        hvm.setAccent(ViewFormatterUtils.mapAppColorAccent(view.getBatteryColorClass()));
        placeModel.getValues().add(hvm);
    }

    private void mapClimateView(PlaceDirectives placeDirectives, ClimateView view, HomeViewPlaceModel placeModel, AppViewTarget viewTarget) {

        placeModel.getValues().add(mapTemperature(placeDirectives, view));
        if (StringUtils.isNotBlank(view.getStateHumidity()) && viewTarget != AppViewTarget.COMPLICATION) {
            placeModel.getValues().add(mapHumidity(placeDirectives, view));
        }
    }

    private void mapGroupView(PlaceDirectives placeDirectives, WidgetGroupView view, HomeViewPlaceModel placeModel, AppViewTarget viewTarget) {

        view.getCaptionAndValue().forEach((k, v) -> {
            var hvm = new HomeViewValueModel();
            hvm.setId(v.getId());
            //noinspection StatementWithEmptyBody
            if(StringUtils.isBlank(v.getState()) && StringUtils.isNotBlank(v.getIconNativeClient())){
                // widget symbol header
            }else{
                hvm.getValueDirectives().addAll(Stream.of(ValueDirective.SYMBOL_SKIP).map(Enum::name).toList());
            }
            hvm.setKey(k);
            hvm.setValue(v.getState());
            hvm.setAccent(ViewFormatterUtils.mapAppColorAccent(v.getColorClass()));
            hvm.setSymbol(v.getIconNativeClient());
            placeModel.getValues().add(hvm);
        });
    }

    private void mapWeatherForecastsView(PlaceDirectives placeDirectives, WeatherForecastsView view, HomeViewPlaceModel placeModel, AppViewTarget viewTarget) {

        if(viewTarget == AppViewTarget.COMPLICATION){
            return;
        }

        placeModel.getValues().add(mapForecastTemperature(placeDirectives, view));
        if (StringUtils.isNotBlank(view.getStateEventWatch())) {
            placeModel.getValues().add(mapForecastEvent(placeDirectives, view));
        }

        if(viewTarget == AppViewTarget.WATCH) {
            placeModel.getValues().add(mapForecastShortTerm(placeDirectives, view));
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

    private void mapLightsView(PlaceDirectives placeDirectives, LightsView view, HomeViewPlaceModel placeModel, AppViewTarget viewTarget) {

        if(viewTarget != AppViewTarget.WATCH || view.getLights().isEmpty()){
            return;
        }
        placeModel.getValues().add(mapLights(placeDirectives, view));
        if(!Boolean.parseBoolean(view.getUnreach())) {
            placeModel.getActions().addAll(mapLightsActions(placeDirectives, view));
        }
    }

    private void mapChargingView(PlaceDirectives placeDirectives, ChargingView view, HomeViewPlaceModel placeModel, AppViewTarget viewTarget) {

        placeModel.setName("Ladung " + view.getName());
        placeModel.getValues().add(mapCharging(placeDirectives, view));
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
            placeModel.getPlaceDirectives().addAll(placeDirectives.directives.stream().map(Enum::name).toList());
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

    private HomeViewValueModel mapCharging(PlaceDirectives placeDirectives, ChargingView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(view.getId());
        hvm.setKey(view.getStateShortLabel());
        hvm.setValue(view.getStateShort());
        hvm.setAccent(ViewFormatterUtils.mapAppColorAccent(view.getColorClass()));
        return hvm;
    }

    private HomeViewValueModel mapTemperature(PlaceDirectives placeDirectives, ClimateView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(view.getId());
        hvm.getValueDirectives().addAll(Stream.of(ValueDirective.SYMBOL_SKIP).map(Enum::name).toList());
        hvm.setKey("Wärme");
        hvm.setValue(view.getStateTemperature());
        hvm.setValueShort(view.getStateShort());
        hvm.setAccent(ViewFormatterUtils.mapAppColorAccent(view.getColorClass()));
        hvm.setTendency(Tendency.nameFromCssClass(view.getTendencyIconTemperature()));
        hvm.setSymbol(Tendency.symbolFromCssClass(view.getTendencyIconTemperature()));
        return hvm;
    }

    private HomeViewValueModel mapHumidity(PlaceDirectives placeDirectives, ClimateView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(placeDirectives.place.name() + "-humi");
        hvm.getValueDirectives().addAll(Stream.of(ValueDirective.SYMBOL_SKIP, ValueDirective.WIDGET_SKIP, ValueDirective.LOCKSCREEN_SKIP).map(Enum::name).toList());
        hvm.setKey("Feuchte");
        hvm.setValue(view.getStateHumidity());
        hvm.setAccent(ViewFormatterUtils.mapAppColorAccent(view.getColorClassHumidity()));
        hvm.setTendency(Tendency.nameFromCssClass(view.getTendencyIconHumidity()));
        return hvm;
    }

    private HomeViewValueModel mapForecastTemperature(PlaceDirectives placeDirectives, WeatherForecastsView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(view.getId());
        hvm.getValueDirectives().addAll(Stream.of(ValueDirective.SYMBOL_SKIP).map(Enum::name).toList());
        hvm.setKey("2-Tage");
        hvm.setValue(view.getStateTemperatureWatch());
        hvm.setAccent(ViewFormatterUtils.mapAppColorAccent(view.getColorClass()));
        hvm.setSymbol(view.getIconNativeClient());
        return hvm;
    }

    private HomeViewValueModel mapForecastShortTerm(PlaceDirectives placeDirectives, WeatherForecastsView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(placeDirectives.place.name() + "-fcShortTerm");
        hvm.getValueDirectives().addAll(Stream.of(ValueDirective.SYMBOL_SKIP, ValueDirective.WIDGET_SKIP, ValueDirective.LOCKSCREEN_SKIP).map(Enum::name).toList());
        hvm.setKey("3-Stunden");
        hvm.setValue(view.getShortTermText());
        hvm.setAccent(ViewFormatterUtils.mapAppColorAccent(view.getShortTermColorClass()));
        return hvm;
    }

    private HomeViewValueModel mapPresence(PlaceDirectives placeDirectives, PresenceView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(placeDirectives.place.name() + "-presence");
        hvm.setKey(view.getName());
        hvm.setValue(view.getStateShort());
        hvm.setAccent(ViewFormatterUtils.mapAppColorAccent(view.getColorClass()));
        return hvm;
    }

    @SuppressWarnings("UnnecessaryUnicodeEscape")
    private HomeViewValueModel mapHeatpump(PlaceDirectives placeDirectives, HeatpumpView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(view.getId());
        hvm.setKey("Wärmepumpe");
        if(Boolean.parseBoolean(view.getBusy())) {
            hvm.setValue("...\u21BB...");
            hvm.setAccent(Strings.EMPTY);
        }else if(Boolean.parseBoolean(view.getUnreach())) {
            hvm.setValue("???");
            hvm.setAccent(Strings.EMPTY);
        }else{
            hvm.setValue(view.getStateShort());
            hvm.setAccent(ViewFormatterUtils.mapAppColorAccent(view.getColorClass()));
        }
        return hvm;
    }

    private HomeViewValueModel mapLights(PlaceDirectives placeDirectives, LightsView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(placeDirectives.place.name() + "-light");
        hvm.setKey("Licht");
        if(Boolean.parseBoolean(view.getUnreach())) {
            hvm.setValue("???");
            hvm.setAccent(Strings.EMPTY);
        }else{
            hvm.setValue(view.getStateShort());
            hvm.setAccent(ViewFormatterUtils.mapAppColorAccent(view.getColorClass()));
        }
        return hvm;
    }

    private HomeViewValueModel mapForecastEvent(PlaceDirectives placeDirectives, WeatherForecastsView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(placeDirectives.place.name() + "-fcEvent");
        hvm.getValueDirectives().addAll(Stream.of(ValueDirective.SYMBOL_SKIP, ValueDirective.WIDGET_SKIP).map(Enum::name).toList());
        hvm.setKey("Ereignis");
        hvm.setValue(view.getStateEventWatch());
        hvm.setAccent(ViewFormatterUtils.mapAppColorAccent(view.getColorClass()));
        hvm.setSymbol(view.getIconNativeClient());
        return hvm;
    }

    private HomeViewValueModel mapActualPower(PlaceDirectives placeDirectives, PowerView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(view.getDevice().name() + "-" + placeDirectives.place.name() + "-actPowerSum");
        hvm.setKey("Aktuell");
        hvm.setValue(view.getState());
        hvm.setAccent(ViewFormatterUtils.mapAppColorAccent(view.getColorClass()));
        hvm.setTendency(Tendency.nameFromCssClass(view.getTendencyIcon()));
        return hvm;
    }

    private HomeViewValueModel mapTodayPower(PlaceDirectives placeDirectives, PowerView view, Optional<String> direction, String overrideColorClass) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(view.getId());
        hvm.setKey(view.getDevice().getType() == Type.GAS_POWER ? "Gas" : "Strom");
        if(view.getDevice().getType() == Type.GAS_POWER){
            hvm.setKey("Gas");
        }else{
            hvm.setKey("Strom");
        }
        direction.ifPresent(d -> {
            hvm.setKey(hvm.getKey() + " " + d);
            hvm.setId(hvm.getId() + d);
        });

        if (BooleanUtils.toBoolean(view.getUnreach())) {
            hvm.setValue(StringUtils.EMPTY);
        } else if (view.getTodayConsumption() == null) {
            hvm.setValue("0" + ViewFormatter.powerConsumptionUnit(view.getDevice()));
        }else {
            hvm.setValue(view.getTodayConsumption().getLabel().replace(ViewFormatter.SUM_SIGN, "").trim());
        }
        hvm.setAccent(ViewFormatterUtils.mapAppColorAccent(overrideColorClass != null ? overrideColorClass : view.getColorClass()));
        return hvm;
    }

    private HomeViewValueModel mapActualPV(PowerView view, String caption) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId("pv" + caption.replace(" ", ""));
        hvm.setKey(caption);
        hvm.setValue(view.getState());
        hvm.setAccent(ViewFormatterUtils.mapAppColorAccent(view.getColorClass()));
        return hvm;
    }

    private HomeViewValueModel mapLockStatus(PlaceDirectives placeDirectives, LockView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(placeDirectives.place.name() + "-lockStatus");
        hvm.setKey("Tür");
        hvm.setValue(Boolean.TRUE.toString().equalsIgnoreCase(view.getBusy()) ? ". . ." : view.getState());
        hvm.setAccent(ViewFormatterUtils.mapAppColorAccent(view.getColorClass()));
        return hvm;
    }

    private List<List<HomeViewActionModel>> mapLockActions(PlaceDirectives placeDirectives, LockView view) {

        if (BooleanUtils.toBoolean(view.getUnreach())) {
            return new LinkedList<>();
        }

        List<HomeViewActionModel> actionsState = new LinkedList<>();
        HomeViewActionModel actionSwitchCaption = new HomeViewActionModel();
        actionSwitchCaption.setId(placeDirectives.place.name() + "-lockStateCaption");
        actionSwitchCaption.setName("Tür Status");
        actionSwitchCaption.setLink(Strings.EMPTY);
        actionsState.add(actionSwitchCaption);
        HomeViewActionModel actionLock = new HomeViewActionModel();
        actionLock.setId(placeDirectives.place.name() + "-lockActionLock");
        actionLock.setName("Verriegeln");
        actionLock.setLink(view.getLinkLock());
        actionsState.add(actionLock);
        HomeViewActionModel actionUnlock = new HomeViewActionModel();
        actionUnlock.setId(placeDirectives.place.name() + "-lockActionUnlock");
        actionUnlock.setName("Entriegeln");
        actionUnlock.setLink(view.getLinkUnlock());
        actionsState.add(actionUnlock);
        HomeViewActionModel actionOpen = new HomeViewActionModel();
        actionOpen.setId(placeDirectives.place.name() + "-lockActionOpen");
        actionOpen.setName("Öffnen");
        actionOpen.setLink(view.getLinkOpen());
        actionsState.add(actionOpen);
        List<HomeViewActionModel> actionsControl = new LinkedList<>();
        HomeViewActionModel actionModeCaption = new HomeViewActionModel();
        actionModeCaption.setId(placeDirectives.place.name() + "-lockModeCaption");
        actionModeCaption.setName("Tür Modus");
        actionModeCaption.setLink(Strings.EMPTY);
        actionsControl.add(actionModeCaption);
        HomeViewActionModel actionAuto = new HomeViewActionModel();
        actionAuto.setId(placeDirectives.place.name() + "-lockActionAuto");
        actionAuto.setName("Automatisch");
        actionAuto.setLink(view.getLinkAuto());
        actionsControl.add(actionAuto);
        HomeViewActionModel actionManu = new HomeViewActionModel();
        actionManu.setId(placeDirectives.place.name() + "-lockActionManu");
        actionManu.setName("Manuell");
        actionManu.setLink(view.getLinkManual());
        actionsControl.add(actionManu);
        HomeViewActionModel actionEvent = new HomeViewActionModel();
        actionEvent.setId(placeDirectives.place.name() + "-lockActionEvent");
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
        hvm.setId(placeDirectives.place.name() + "-switchStatus-" + view.getId());
        hvm.setKey(view.getShortName());
        hvm.setValue(view.getStateShort());
        hvm.setAccent(ViewFormatterUtils.mapAppColorAccent(view.getColorClass()));
        return hvm;
    }

    private HomeViewValueModel mapWindowStatus(PlaceDirectives placeDirectives, WindowSensorView view) {
        HomeViewValueModel hvm = new HomeViewValueModel();
        hvm.setId(placeDirectives.place.name() + "-windowStatus");
        hvm.setKey(view.getShortName());
        hvm.setValue(view.getStateShort());
        hvm.setAccent(ViewFormatterUtils.mapAppColorAccent(view.getColorClass()));
        return hvm;
    }

    private List<List<HomeViewActionModel>> mapSwitchActions(PlaceDirectives placeDirectives, SwitchView view) {

        if (BooleanUtils.toBoolean(view.getUnreach())) {
            return new LinkedList<>();
        }

        List<HomeViewActionModel> actionsOnOff = new LinkedList<>();
        HomeViewActionModel actionSwitchCaption = new HomeViewActionModel();
        actionSwitchCaption.setId(placeDirectives.place.name() + "-switchStateCaption");
        actionSwitchCaption.setName(view.getShortName() + " Schalter Status");
        actionSwitchCaption.setLink(Strings.EMPTY);
        actionsOnOff.add(actionSwitchCaption);
        HomeViewActionModel actionOn = new HomeViewActionModel();
        actionOn.setId(placeDirectives.place.name() + "-switchActionOn");
        actionOn.setName("Ein");
        actionOn.setLink(view.getLinkOn());
        actionsOnOff.add(actionOn);
        HomeViewActionModel actionOff = new HomeViewActionModel();
        actionOff.setId(placeDirectives.place.name() + "-switchActionOff");
        actionOff.setName("Aus");
        actionOff.setLink(view.getLinkOff());
        actionsOnOff.add(actionOff);
        List<HomeViewActionModel> actionsControl = new LinkedList<>();
        HomeViewActionModel actionModeCaption = new HomeViewActionModel();
        actionModeCaption.setId(placeDirectives.place.name() + "-switchModeCaption");
        actionModeCaption.setName(view.getShortName() + " Schalter Modus");
        actionModeCaption.setLink(Strings.EMPTY);
        actionsControl.add(actionModeCaption);
        if(StringUtils.isNotBlank(view.getLinkAuto())){
            HomeViewActionModel actionAuto = new HomeViewActionModel();
            actionAuto.setId(placeDirectives.place.name() + "-switchActionAuto");
            actionAuto.setName("Automatisch");
            actionAuto.setLink(view.getLinkAuto());
            actionsControl.add(actionAuto);
        }
        if(StringUtils.isNotBlank(view.getLinkManual())){
            HomeViewActionModel actionManu = new HomeViewActionModel();
            actionManu.setId(placeDirectives.place.name() + "-switchActionManu");
            actionManu.setName("Manuell");
            actionManu.setLink(view.getLinkManual());
            actionsControl.add(actionManu);
        }
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
        actionSwitchCaption.setId(placeDirectives.place.name() + "-hpSwitchesCaption-" + idSuffix);
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
        actionsDirect.add(mapHeatpumpActionSinglePreset(placeDirectives, view, other, HeatpumpPreset.DRY_TIMER, idSuffix));
        actionsDirect.add(mapHeatpumpActionSinglePreset(placeDirectives, view, other, HeatpumpPreset.OFF, idSuffix));

        return actionsDirect;
    }

    private HomeViewActionModel mapHeatpumpActionSinglePreset(PlaceDirectives placeDirectives, HeatpumpView view, List<ValueWithCaption> other, HeatpumpPreset preset, String idSuffix) {

        HomeViewActionModel hpActionSwitch = new HomeViewActionModel();
        hpActionSwitch.setId(placeDirectives.place.name() + "-hpSwitch-" + preset + "-" + idSuffix);
        hpActionSwitch.setName(preset.getShortText());
        var link = switch (preset) {
            case COOL_AUTO -> view.getLinkCoolAuto();
            case COOL_MIN -> view.getLinkCoolMin();
            case HEAT_AUTO -> view.getLinkHeatAuto();
            case HEAT_MIN -> view.getLinkHeatMin();
            case DRY_TIMER -> view.getLinkDryTimer();
            case OFF -> view.getLinkOff();
            default -> "#";
        };
        hpActionSwitch.setLink(link);
        if(!other.isEmpty() && !link.equals("#")){
            other.forEach(o -> hpActionSwitch.setLink(hpActionSwitch.getLink() + o.getValue() + ","));
        }
        return hpActionSwitch;
    }

    private List<List<HomeViewActionModel>> mapLightsActions(PlaceDirectives placeDirectives, LightsView view) {

        List<List<HomeViewActionModel>> actions = new LinkedList<>();

        view.getLights().forEach(light -> {
            List<HomeViewActionModel> lightAction = new LinkedList<>();
            HomeViewActionModel actionLightCaption = new HomeViewActionModel();
            actionLightCaption.setId(light.getId() + "-Caption");
            actionLightCaption.setName("Licht " + light.getName());
            actionLightCaption.setLink(Strings.EMPTY);
            lightAction.add(actionLightCaption);
            HomeViewActionModel actionOn = new HomeViewActionModel();
            actionOn.setId(light.getId() + "-ActionOn");
            actionOn.setName("Ein");
            actionOn.setLink(light.getLinkOn());
            lightAction.add(actionOn);
            HomeViewActionModel actionOff = new HomeViewActionModel();
            actionOff.setId(light.getId() + "-ActionOff");
            actionOff.setName("Aus");
            actionOff.setLink(light.getLinkOff());
            lightAction.add(actionOff);
            actions.add(lightAction);
        });

        return actions;
    }

    private boolean directiveContainsOnly(PlaceDirectives placeDirectives, @SuppressWarnings("SameParameterValue") PlaceDirective directive){
        return placeDirectives.directives.size()==1 && placeDirectives.directives.contains(directive);
    }

    private boolean isColorClassOrangeOrRed(View view){
        return view.getColorClass().equalsIgnoreCase(ConditionColor.ORANGE.getUiClass())
                || view.getColorClass().equalsIgnoreCase(ConditionColor.RED.getUiClass());
    }

    public enum AppViewTarget{
        WATCH, COMPLICATION, WIDGET
    }

    private enum PlaceDirective {
        // Watch
        WATCH_LABEL, WATCH_SYMBOL,
        // Widget HomeScreen
        WIDGET_LABEL_SMALL, WIDGET_LABEL_MEDIUM, WIDGET_LABEL_LARGE, WIDGET_SYMBOL,
        // Widget LockScreen
        WIDGET_LOCKSCREEN_CIRCULAR
    }

    private enum ValueDirective {
        SYMBOL_SKIP, WIDGET_SKIP, LOCKSCREEN_SKIP
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
