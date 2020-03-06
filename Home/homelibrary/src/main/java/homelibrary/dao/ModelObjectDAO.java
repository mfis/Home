package homelibrary.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import homecontroller.domain.model.CameraMode;
import homecontroller.domain.model.CameraModel;
import homecontroller.domain.model.CameraPicture;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.SettingsModel;
import homelibrary.homematic.model.Device;

public class ModelObjectDAO {

	private static ModelObjectDAO instance;

	private HouseModel houseModel;

	private HistoryModel historyModel;

	private CameraModel cameraModel;

	private Map<String, SettingsModel> settingsModels = new HashMap<>();

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

	public void write(SettingsModel newModel) {
		settingsModels.put(newModel.getUser(), newModel);
	}

	public HouseModel readHouseModel() {
		long newestTimestamp = Math.max(houseModel == null ? 0 : houseModel.getDateTime(),
				getLastLongPollingTimestamp());
		if (houseModel == null) {
			lastHouseModelState = "No data-model set.";
			return null;
		} else if (new Date().getTime() - newestTimestamp > 1000 * 60 * 3) {
			lastHouseModelState = "Data state is too old: "
					+ ((new Date().getTime() - newestTimestamp) / 1000) + " sec.";
			return null; // Too old. Should never happen
		} else {
			lastHouseModelState = "OK";
			return houseModel;
		}
	}

	public HistoryModel readHistoryModel() {
		long newestTimestamp = Math.max(historyModel == null ? 0 : historyModel.getDateTime(),
				getLastLongPollingTimestamp());
		if (historyModel == null || new Date().getTime() - newestTimestamp > 1000 * 60 * 15) {
			return null; // Too old. Should never happen
		} else {
			return historyModel;
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
				if (cameraPicture.getDevice() == device
						&& isActualEventPicture(eventTimestamp, cameraPicture)) {
					return cameraPicture;
				}
			}
			return null;
		default:
			throw new IllegalArgumentException("Unknown CameraMode: " + cameraMode);
		}
	}

	private boolean isActualEventPicture(long eventTimestamp, CameraPicture cameraPicture) {
		return Math.abs(cameraPicture.getTimestamp() - eventTimestamp) < 1000 * 30;
	}

	private boolean isActualLivePicture(long eventTimestamp) {
		return cameraModel.getLivePicture() != null
				&& cameraModel.getLivePicture().getTimestamp() >= eventTimestamp;
	}

	public SettingsModel readSettingsModels(String user) {
		if (settingsModels == null) {
			settingsModels = new HashMap<>();
		}
		return settingsModels.get(user);
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
