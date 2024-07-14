package de.fimatas.home.library.dao;

import java.util.Collection;
import java.util.Date;
import java.util.stream.Stream;

import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.model.*;
import de.fimatas.home.library.util.HomeAppConstants;
import lombok.Getter;

public class ModelObjectDAO {

    private static ModelObjectDAO instance;

    private HouseModel houseModel;

    private HistoryModel historyModel;

    private LightsModel lightsModel;

    private PresenceModel presenceModel;

    private HeatpumpModel heatpumpModel;

    private WeatherForecastModel weatherForecastModel;

    private ElectricVehicleModel electricVehicleModel;

    private PushMessageModel pushMessageModel;

    private TasksModel tasksModel;

    private SettingsContainer settingsContainer;

    private PvAdditionalDataModel pvAdditionalDataModel;

    @Getter
    private String lastHouseModelState;

    private ModelObjectDAO() {
        super();
    }

    public static synchronized ModelObjectDAO getInstance() {
        if (instance == null) {
            instance = new ModelObjectDAO();
        }
        return instance;
    }

    public static void resetAll(){
        instance = new ModelObjectDAO();
    }

    public void write(HouseModel newModel) {
        houseModel = newModel;
        houseModel.setDateTime(new Date().getTime());
    }

    public void write(HistoryModel newModel) {
        historyModel = newModel;
        historyModel.setDateTime(new Date().getTime());
    }

    public void write(PresenceModel newModel) {
        presenceModel = newModel;
        presenceModel.setDateTime(new Date().getTime());
    }

    public void write(HeatpumpModel newModel) {
        heatpumpModel = newModel;
        heatpumpModel.setTimestamp(new Date().getTime());
    }

    public void write(ElectricVehicleModel newModel) {
        electricVehicleModel = newModel;
        electricVehicleModel.setTimestamp(new Date().getTime());
    }

    public void write(TasksModel newModel) {
        tasksModel = newModel;
        tasksModel.setDateTime(new Date().getTime());
    }

    public void write(PvAdditionalDataModel newModel) {
        pvAdditionalDataModel = newModel;
        pvAdditionalDataModel.setDateTime(new Date().getTime());
    }

    public void write(LightsModel newModel) {
        lightsModel = newModel;
    }

    public void write(WeatherForecastModel newModel) {
        weatherForecastModel = newModel;
    }

    public void write(PushMessageModel newModel) {
        if(newModel.isAdditionalEntries() && pushMessageModel != null){
            pushMessageModel.getList().addAll(0, newModel.getList());
            pushMessageModel.setTimestamp(newModel.getTimestamp());
        }else{
            pushMessageModel = newModel;
        }
    }

    public void write(SettingsContainer newSettingsContainer) {
        settingsContainer = newSettingsContainer;
    }

    public HouseModel readHouseModel() {
        long newestTimestamp = houseModel == null ? 0 : houseModel.getDateTime();
        if (houseModel == null) {
            lastHouseModelState = "No data-model set.";
            return null;
        } else if (new Date().getTime() - newestTimestamp > 1000 * HomeAppConstants.MODEL_OUTDATED_SECONDS) {
            lastHouseModelState = "Data state is too old: " + ((new Date().getTime() - newestTimestamp) / 1000) + " sec.";
            return null; // Too old. Should never happen
        } else {
            lastHouseModelState = "OK";
            return houseModel;
        }
    }

    public HouseModel readHouseModelIgnoringAge(){
        return houseModel;
    }

    public HistoryModel readHistoryModel() {
        long newestTimestamp = historyModel == null ? 0 : historyModel.getDateTime();
        if (historyModel == null || new Date().getTime() - newestTimestamp > 1000 * HomeAppConstants.HISTORY_OUTDATED_SECONDS) {
            return null; // Too old. Should never happen
        } else {
            return historyModel;
        }
    }

    public LightsModel readLightsModel() {
        long newestTimestamp = lightsModel == null ? 0 : lightsModel.getTimestamp();
        if (lightsModel == null || new Date().getTime() - newestTimestamp > 1000 * HomeAppConstants.MODEL_OUTDATED_SECONDS) {
            return null; // Too old. Should never happen
        } else {
            return lightsModel;
        }
    }

    public PresenceModel readPresenceModel() {
        long newestTimestamp = presenceModel == null ? 0 : presenceModel.getDateTime();
        if (presenceModel == null || new Date().getTime() - newestTimestamp > 1000 * HomeAppConstants.MODEL_PRESENCE_OUTDATED_SECONDS) {
            return null; // Too old. Should never happen
        } else {
            return presenceModel;
        }
    }

    public HeatpumpModel readHeatpumpModel() {
        long newestTimestamp = heatpumpModel == null ? 0 : heatpumpModel.getTimestamp();
        if (heatpumpModel == null || new Date().getTime() - newestTimestamp > 1000 * HomeAppConstants.MODEL_HEATPUMP_OUTDATED_SECONDS) {
            return null; // Too old. Should never happen
        } else {
            return heatpumpModel;
        }
    }

    public WeatherForecastModel readWeatherForecastModel() {
        long newestTimestamp = weatherForecastModel == null ? 0 : weatherForecastModel.getDateTime();
        if (weatherForecastModel == null || new Date().getTime() - newestTimestamp > 1000 * 60 * 70) {
            return null; // Too old. Should never happen
        } else {
            return weatherForecastModel;
        }
    }

    public TasksModel readTasksModel() {
        long newestTimestamp = tasksModel == null ? 0 : tasksModel.getDateTime();
        if (tasksModel == null || new Date().getTime() - newestTimestamp > 1000 * HomeAppConstants.MODEL_TASKS_OUTDATED_SECONDS) {
            return null; // Too old. Should never happen
        } else {
            return tasksModel;
        }
    }

    public PvAdditionalDataModel readPvAdditionalDataModel() {
        long newestTimestamp = pvAdditionalDataModel == null ? 0 : pvAdditionalDataModel.getDateTime();
        if (pvAdditionalDataModel == null || new Date().getTime() - newestTimestamp > 1000 * HomeAppConstants.MODEL_PV_OUTDATED_SECONDS) {
            return null; // Too old. Should never happen
        } else {
            return pvAdditionalDataModel;
        }
    }

    public PushMessageModel readPushMessageModel() {
        return pushMessageModel;
    }

    public ElectricVehicleModel readElectricVehicleModel() {
        return electricVehicleModel;
    }

    public long calculateModelTimestamp(){

        HouseModel hm = readHouseModel();
        LightsModel lm = readLightsModel();
        WeatherForecastModel wfm = readWeatherForecastModel();
        PresenceModel pm = readPresenceModel();
        HeatpumpModel hpm = readHeatpumpModel();
        ElectricVehicleModel evm = readElectricVehicleModel();
        PushMessageModel pmm = readPushMessageModel();
        TasksModel tm = readTasksModel();
        PvAdditionalDataModel padm = readPvAdditionalDataModel();

        return  Stream.of(
                hm==null?0:hm.getDateTime(),
                lm==null?0:lm.getTimestamp(),
                wfm==null?0:wfm.getDateTime(),
                pm==null?0:pm.getDateTime(),
                hpm==null?0:hpm.getTimestamp(),
                evm==null?0:evm.getTimestamp(),
                pmm==null?0:pmm.getTimestamp(),
                tm==null?0:tm.getDateTime(),
                padm==null?0:padm.getDateTime()
        ).max(Long::compare).get();
    }

    public Collection<SettingsModel> readAllSettings() {
        return settingsContainer == null ? null : settingsContainer.getSettings();
    }

    public boolean isKnownPushToken(String pushToken) {
        if(settingsContainer == null){
            return false;
        }
        return settingsContainer.getSettings().stream().anyMatch(model -> model.getToken().equals(pushToken));
    }

}
