package de.fimatas.home.library.dao;

import java.util.Collection;
import java.util.Date;
import de.fimatas.home.library.domain.model.CameraMode;
import de.fimatas.home.library.domain.model.CameraModel;
import de.fimatas.home.library.domain.model.CameraPicture;
import de.fimatas.home.library.domain.model.HistoryModel;
import de.fimatas.home.library.domain.model.HouseModel;
import de.fimatas.home.library.domain.model.LightsModel;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.model.SettingsContainer;
import de.fimatas.home.library.model.SettingsModel;
import de.fimatas.home.library.util.HomeAppConstants;

public class ModelObjectDAO {

    private static ModelObjectDAO instance;

    private HouseModel houseModel;

    private HistoryModel historyModel;

    private CameraModel cameraModel;

    private LightsModel lightsModel;

    private SettingsContainer settingsContainer;

    private String lastHouseModelState;

    private long lastLongPollingTimestamp = 0;

    private ModelObjectDAO() {
        super();
    }

    public static synchronized ModelObjectDAO getInstance() {
        if (instance == null) {
            instance = new ModelObjectDAO();
        }
        return instance;
    }

    public void write(HouseModel newModel) {
        houseModel = newModel;
        houseModel.setDateTime(new Date().getTime());
    }

    public void write(HistoryModel newModel) {
        historyModel = newModel;
        historyModel.setDateTime(new Date().getTime());
    }

    public void write(CameraModel newModel) {
        cameraModel = newModel;
    }

    public void write(LightsModel newModel) {
        lightsModel = newModel;
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

    public HistoryModel readHistoryModel() {
        long newestTimestamp = historyModel == null ? 0 : historyModel.getDateTime();
        if (historyModel == null || new Date().getTime() - newestTimestamp > 1000 * HomeAppConstants.HISTORY_OUTDATED_SECONDS) {
            return null; // Too old. Should never happen
        } else {
            return historyModel;
        }
    }

    public LightsModel readLightsModel() {
        long newestTimestamp = lightsModel == null ? 0 : historyModel.getDateTime();
        if (lightsModel == null || new Date().getTime() - newestTimestamp > 1000 * HomeAppConstants.MODEL_OUTDATED_SECONDS) {
            return null; // Too old. Should never happen
        } else {
            return lightsModel;
        }
    }

    public CameraModel readCameraModel() {
        if (cameraModel == null) {
            cameraModel = new CameraModel();
        }
        return cameraModel;
    }

    public CameraPicture readCameraPicture(Device device, CameraMode cameraMode, long eventTimestamp) {

        if (cameraModel == null) {
            return null;
        }
        switch (cameraMode) {
        case LIVE:
            if (isActualLivePicture(eventTimestamp)) {
                return cameraModel.getLivePicture();
            }
            return null;
        case EVENT:
            for (CameraPicture cameraPicture : cameraModel.getEventPictures()) {
                if (cameraPicture.getDevice() == device && isActualEventPicture(eventTimestamp, cameraPicture)) {
                    return cameraPicture;
                }
            }
            return null;
        default:
            throw new IllegalArgumentException("Unknown CameraMode: " + cameraMode);
        }
    }

    public Collection<SettingsModel> readAllSettings() {
        return settingsContainer.getSettings();
    }

    public boolean isKnownPushToken(String pushToken) {
        return settingsContainer.getSettings().stream().anyMatch(model -> model.getToken().equals(pushToken));
    }

    private boolean isActualEventPicture(long eventTimestamp, CameraPicture cameraPicture) {
        return Math.abs(cameraPicture.getTimestamp() - eventTimestamp) < 1000 * 30;
    }

    private boolean isActualLivePicture(long eventTimestamp) {
        return cameraModel.getLivePicture() != null && cameraModel.getLivePicture().getTimestamp() >= eventTimestamp;
    }

    public String getLastHouseModelState() {
        return lastHouseModelState;
    }

    public long getLastLongPollingTimestamp() {
        return lastLongPollingTimestamp;
    }

    public void setLastLongPollingTimestamp(long lastLongPollingTimestamp) {
        this.lastLongPollingTimestamp = lastLongPollingTimestamp;
    }
}
