package homelibrary.dao;

import java.util.Date;

import homecontroller.domain.model.CameraMode;
import homecontroller.domain.model.CameraModel;
import homecontroller.domain.model.CameraPicture;
import homecontroller.domain.model.Device;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.HouseModel;

public class ModelObjectDAO {

	private static ModelObjectDAO instance;

	private HouseModel houseModel;

	private HistoryModel historyModel;

	private CameraModel cameraModel;

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
	}

	public void write(HistoryModel newModel) {
		historyModel = newModel;
	}

	public void write(CameraModel newModel) {
		cameraModel = newModel;
	}

	public HouseModel readHouseModel() {
		if (houseModel == null || new Date().getTime() - houseModel.getDateTime() > 1000 * 60 * 3) {
			return null; // Too old. Should never happen
		} else {
			return houseModel;
		}
	}

	public HistoryModel readHistoryModel() {
		if (historyModel == null || new Date().getTime() - historyModel.getDateTime() > 1000 * 60 * 60 * 25) {
			return null; // Too old. Should never happen
		} else {
			return historyModel;
		}
	}

	public CameraModel readCameraModel() {
		return cameraModel;
	}

	public CameraPicture readCameraPicture(Device device, CameraMode cameraMode, long eventTimestamp) {
		if (cameraModel == null) {
			return null;
		}
		switch (cameraMode) {
		case LIVE:
			return cameraModel.getLivePicture();
		case EVENT:
			for (CameraPicture cameraPicture : cameraModel.getEventPictures()) {
				if (cameraPicture.getDevice() == device) {
					long timestampDiff = Math.abs(cameraPicture.getTimestamp() - eventTimestamp);
					if (timestampDiff < 1000 * 30) {
						return cameraPicture;
					}
				}
			}
			return null;
		default:
			throw new IllegalArgumentException("Unknown CameraMode: " + cameraMode);
		}
	}
}
