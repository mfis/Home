package homecontroller.dao;

import java.util.Date;

import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.HouseModel;

public class ModelDAO {

	private static ModelDAO instance;

	private HouseModel houseModel;

	private HistoryModel historyModel;

	private ModelDAO() {
		super();
	}

	public static synchronized ModelDAO getInstance() {
		if (instance == null) {
			instance = new ModelDAO();
		}
		return instance;
	}

	public void write(HouseModel newModel) {
		houseModel = newModel;
	}

	public void write(HistoryModel newModel) {
		historyModel = newModel;
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
}
