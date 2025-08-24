package de.fimatas.home.library.dao;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;

import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.model.*;
import de.fimatas.home.library.util.HomeAppConstants;
import de.fimatas.home.library.util.HomeUtils;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

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

    private ControllerStateModel controllerStateModel;

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
        houseModel.setTimestamp(new Date().getTime());
    }

    public void write(HistoryModel newModel) {
        historyModel = newModel;
        historyModel.setTimestamp(new Date().getTime());
    }

    public void write(PresenceModel newModel) {
        presenceModel = newModel;
        presenceModel.setTimestamp(new Date().getTime());
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
        tasksModel.setTimestamp(new Date().getTime());
    }

    public void write(PvAdditionalDataModel newModel) {
        pvAdditionalDataModel = newModel;
        pvAdditionalDataModel.setTimestamp(new Date().getTime());
    }

    public void write(ControllerStateModel newModel) {
        controllerStateModel = newModel;
        controllerStateModel.setTimestamp(new Date().getTime());
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
        long newestTimestamp = houseModel == null ? 0 : houseModel.getTimestamp();
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
        long newestTimestamp = historyModel == null ? 0 : historyModel.getTimestamp();
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
        long newestTimestamp = presenceModel == null ? 0 : presenceModel.getTimestamp();
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
        long newestTimestamp = weatherForecastModel == null ? 0 : weatherForecastModel.getTimestamp();
        if (weatherForecastModel == null || new Date().getTime() - newestTimestamp > 1000 * 60 * 125) {
            return null; // Too old. Should never happen
        } else {
            return weatherForecastModel;
        }
    }

    public TasksModel readTasksModel() {
        long newestTimestamp = tasksModel == null ? 0 : tasksModel.getTimestamp();
        if (tasksModel == null || new Date().getTime() - newestTimestamp > 1000 * HomeAppConstants.MODEL_TASKS_OUTDATED_SECONDS) {
            return null; // Too old. Should never happen
        } else {
            return tasksModel;
        }
    }

    public PvAdditionalDataModel readPvAdditionalDataModel() {
        long newestTimestamp = pvAdditionalDataModel == null ? 0 : pvAdditionalDataModel.getLastCollectionTimeReadMillis();
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

    public ControllerStateModel readControllerStateModel() {
        return controllerStateModel;
    }

    public long calculateModelTimestamp(){
        return models().values().stream().filter(Objects::nonNull).map(AbstractSystemModel::getTimestamp).max(Long::compare).orElse(0L);
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

    private Map<String, AbstractSystemModel> models() {

        Field[] fields = this.getClass().getDeclaredFields();
        Map<String, AbstractSystemModel> modelList = new LinkedHashMap<>();
        try {
            for (Field field : fields) {
                if (field.getType().getSuperclass() != null && field.getType().getSuperclass().equals(AbstractSystemModel.class)) {
                    modelList.put(field.getName(), (AbstractSystemModel) field.get(this));
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalArgumentException("Exception collecting fields:", e);
        }
        return modelList;
    }

    public String printModelState(){
        StringBuilder sb = new StringBuilder();
        var models = models();
        int maxLength = models.keySet().stream().mapToInt(String::length).max().orElse(0);
        models.keySet().forEach(m -> {
            sb.append(StringUtils.rightPad(m, maxLength, '.')).append(": ");
            if(models.get(m) != null){
                Instant givenTime = Instant.ofEpochMilli(models.get(m).getTimestamp());
                sb.append(HomeUtils.durationSinceFormatted(givenTime, true, false, false));
            }else {
                sb.append("null");
            }
            sb.append("\n");
        });
        return sb.toString();
    }

}
