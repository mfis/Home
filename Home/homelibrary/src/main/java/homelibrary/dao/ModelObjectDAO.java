package homelibrary.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import homecontroller.domain.model.CameraMode;
import homecontroller.domain.model.CameraModel;
import homecontroller.domain.model.CameraPicture;
import homecontroller.domain.model.Device;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.SettingsModel;

public class ModelObjectDAO {

	private static ModelObjectDAO instance;

	private HouseModel houseModel;

	private HistoryModel historyModel;

	private CameraModel cameraModel;

	private Map<String, SettingsModel> settingsModels = new HashMap<>();

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
		if (houseModel == null || new Date().getTime() - houseModel.getDateTime() > 1000 * 60 * 3) {
			return null; // Too old. Should never happen
		} else {
			return houseModel;
		}
	}

	public HistoryModel readHistoryModel() {
		if (historyModel == null || new Date().getTime() - historyModel.getDateTime() > 1000 * 60 * 15) {
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
			if (cameraModel.getLivePicture() != null
					&& cameraModel.getLivePicture().getTimestamp() >= eventTimestamp) {
				return cameraModel.getLivePicture();
			}
			return null;
		case EVENT:
			if (cameraModel.getEventPictures() != null) {
				for (CameraPicture cameraPicture : cameraModel.getEventPictures()) {
					if (cameraPicture.getDevice() == device) {
						long timestampDiff = Math.abs(cameraPicture.getTimestamp() - eventTimestamp);
						System.out.println("diff==" + timestampDiff);
						if (timestampDiff < 1000 * 30) {
							return cameraPicture;
						}
					}
				}
			}
			return null;
		default:
			throw new IllegalArgumentException("Unknown CameraMode: " + cameraMode);
		}
	}

	public SettingsModel readSettingsModels(String user) {
		if (settingsModels == null) {
			settingsModels = new HashMap<>();
		}
		return settingsModels.get(user);
	}
}
