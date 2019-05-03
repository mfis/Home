package homecontroller.dao;

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
		cameraModel = new CameraModel();
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

	public void write(Device device, CameraMode cameraMode, CameraPicture cameraPicture) {
		switch (cameraMode) {
		case LIVE:
			cameraModel.getLivePictures().put(device, cameraPicture);
			break;
		case EVENT:
			cameraModel.getEventPictures().put(device, cameraPicture);
			break;
		default:
			throw new IllegalArgumentException("Unknown CameraMode: " + cameraMode);
		}
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

	public CameraPicture readCameraPicture(Device device, CameraMode cameraMode, long eventTimestamp) {
		switch (cameraMode) {
		case LIVE:
			if (!cameraModel.getLivePictures().containsKey(device) || new Date().getTime()
					- cameraModel.getLivePictures().get(device).getTimestamp() > 1000 * 5) {
				return null;
			} else {
				return cameraModel.getLivePictures().get(device);
			}
		case EVENT:
			if (!cameraModel.getLivePictures().containsKey(device)) {
				return null;
			} else {
				long timestampDiff = Math
						.abs(cameraModel.getLivePictures().get(device).getTimestamp() - eventTimestamp);
				if (timestampDiff > 1000 * 60) {
					return null;
				} else {
					return cameraModel.getEventPictures().get(device);
				}
			}
		default:
			throw new IllegalArgumentException("Unknown CameraMode: " + cameraMode);
		}
	}
}
