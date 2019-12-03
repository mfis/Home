package homecontroller.domain.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import homecontroller.dao.HistoryDatabaseDAO;
import homecontroller.dao.HistoryDatabaseDAO.ExtremValueType;
import homecontroller.dao.HistoryDatabaseDAO.TimeRange;
import homecontroller.database.mapper.TimestampValuePair;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.PowerConsumptionMonth;
import homecontroller.domain.model.TemperatureHistory;
import homecontroller.service.HomematicAPI;
import homelibrary.dao.ModelObjectDAO;
import homelibrary.homematic.model.Datapoint;
import homelibrary.homematic.model.Device;
import homelibrary.homematic.model.History;

@Component
public class HistoryService {

	@Autowired
	private HistoryDatabaseDAO historyDAO;

	@Autowired
	private UploadService uploadService;

	@Autowired
	private HomematicAPI api;

	private static final int HOURS_IN_DAY = 24;

	private static final long HIGHEST_OUTSIDE_TEMPERATURE_PERIOD_HOURS = HOURS_IN_DAY;

	@PostConstruct
	public void init() {
		CompletableFuture.runAsync(() -> {
			try {
				refreshHistoryModelComplete();
			} catch (Exception e) {
				LogFactory.getLog(HistoryService.class)
						.error("Could not initialize HistoryService completly.", e);
			}
		});
	}

	public void saveNewValues() {
		for (History history : History.values()) {
			historyDAO.addEntry(history.getCommand(), new TimestampValuePair(api.getCurrentValuesTimestamp(),
					api.getAsBigDecimal(history.getCommand())));
		}
	}

	@Scheduled(cron = "* 0 0 * * *")
	public void persistValues() {
		for (History history : History.values()) {
			historyDAO.persistEntry(history.getCommand());
		}
	}

	@Scheduled(cron = "5 10 0 * * *")
	public synchronized void refreshHistoryModelComplete() {

		HistoryModel oldModel = ModelObjectDAO.getInstance().readHistoryModel();
		if (oldModel != null) {
			oldModel.setInitialized(false);
		}

		HistoryModel newModel = new HistoryModel();

		calculateElectricPowerConsumption(newModel, null);
		calculateTemperatureHistory(newModel.getOutsideTemperature(), Device.AUSSENTEMPERATUR,
				Datapoint.VALUE);
		calculateTemperatureHistory(newModel.getKidsRoomTemperature(), Device.THERMOMETER_KINDERZIMMER,
				Datapoint.ACTUAL_TEMPERATURE);
		calculateTemperatureHistory(newModel.getBedRoomTemperature(), Device.THERMOMETER_SCHLAFZIMMER,
				Datapoint.ACTUAL_TEMPERATURE);
		calculateTemperatureHistory(newModel.getLaundryTemperature(), Device.THERMOMETER_WASCHKUECHE,
				Datapoint.ACTUAL_TEMPERATURE);

		newModel.setInitialized(true);
		ModelObjectDAO.getInstance().write(newModel);

		refreshHistoryModel();
	}

	// FIXME: @Scheduled(fixedDelay = (1000 * 60 * 5))
	private synchronized void refreshHistoryModel() {

		HistoryModel model = ModelObjectDAO.getInstance().readHistoryModel();
		if (model == null) {
			return;
		}

		BigDecimal maxValue = historyDAO.readExtremValueBetween(Device.AUSSENTEMPERATUR, Datapoint.VALUE,
				ExtremValueType.MAX, LocalDateTime.now().minusHours(HIGHEST_OUTSIDE_TEMPERATURE_PERIOD_HOURS),
				null);
		model.setHighestOutsideTemperatureInLast24Hours(maxValue);

		if (!model.isInitialized()) {
			return;
		}

		calculateElectricPowerConsumption(model, model.getElectricPowerConsumption()
				.get(model.getElectricPowerConsumption().size() - 1).measurePointMaxDateTime());

		updateTemperatureHistory(model.getOutsideTemperature(), Device.AUSSENTEMPERATUR, Datapoint.VALUE);
		updateTemperatureHistory(model.getKidsRoomTemperature(), Device.THERMOMETER_KINDERZIMMER,
				Datapoint.ACTUAL_TEMPERATURE);
		updateTemperatureHistory(model.getBedRoomTemperature(), Device.THERMOMETER_SCHLAFZIMMER,
				Datapoint.ACTUAL_TEMPERATURE);
		updateTemperatureHistory(model.getLaundryTemperature(), Device.THERMOMETER_WASCHKUECHE,
				Datapoint.ACTUAL_TEMPERATURE);

		model.updateDateTime();
		uploadService.upload(model);
	}

	private void updateTemperatureHistory(List<TemperatureHistory> history, Device device,
			Datapoint datapoint) {

		if (history.isEmpty()) {
			history.add(readDayTemperatureHistory(LocalDateTime.now(), device, datapoint));
		} else {
			history.set(0, readDayTemperatureHistory(LocalDateTime.now(), device, datapoint));
		}
	}

	private void calculateTemperatureHistory(List<TemperatureHistory> history, Device device,
			Datapoint datapoint) {

		history.clear();
		LocalDateTime base = LocalDateTime.now();

		TemperatureHistory today = readDayTemperatureHistory(base, device, datapoint);

		history.add(today);
		TemperatureHistory yesterday = readDayTemperatureHistory(base.minusHours(HOURS_IN_DAY), device,
				datapoint);
		history.add(yesterday);

		TemperatureHistory monthHistory;
		YearMonth yearMonth = YearMonth.now();
		do {
			monthHistory = readMonthTemperatureHistory(yearMonth, device, datapoint);
			if (!monthHistory.empty()) {
				history.add(monthHistory);
				yearMonth = yearMonth.minusMonths(1);
			}
		} while (!monthHistory.empty());
	}

	private TemperatureHistory readDayTemperatureHistory(LocalDateTime localDateTime, Device device,
			Datapoint datapoint) {

		LocalDateTime nightStart = toFixedHour(localDateTime, 0);
		LocalDateTime dayEnd = toFixedHour(localDateTime, HOURS_IN_DAY);
		return readTemperatureHistory(localDateTime.toLocalDate(), true, nightStart, dayEnd, device,
				datapoint);
	}

	private TemperatureHistory readMonthTemperatureHistory(YearMonth yearMonth, Device device,
			Datapoint datapoint) {

		LocalDateTime monthStart = LocalDateTime.of(yearMonth.atDay(1),
				toFixedHour(LocalDateTime.now(), 0).toLocalTime());
		LocalDateTime monthEnd = toFixedHour(LocalDateTime.of(yearMonth.atEndOfMonth(), LocalTime.now()),
				HOURS_IN_DAY);
		return readTemperatureHistory(yearMonth.atDay(1), false, monthStart, monthEnd, device, datapoint);
	}

	private TemperatureHistory readTemperatureHistory(LocalDate base, boolean singleDay,
			LocalDateTime monthStart, LocalDateTime monthEnd, Device device, Datapoint datapoint) {

		BigDecimal nightMin = historyDAO.readExtremValueInTimeRange(device, datapoint, ExtremValueType.MIN,
				TimeRange.NIGHT, monthStart, monthEnd);
		if (nightMin == null) {
			nightMin = historyDAO.readFirstValueBefore(device, datapoint, monthStart, 48);
		}

		BigDecimal nightMax = historyDAO.readExtremValueInTimeRange(device, datapoint, ExtremValueType.MAX,
				TimeRange.NIGHT, monthStart, monthEnd);
		if (nightMax == null) {
			nightMax = nightMin;
		}

		BigDecimal dayMin = historyDAO.readExtremValueInTimeRange(device, datapoint, ExtremValueType.MIN,
				TimeRange.DAY, monthStart, monthEnd);
		if (dayMin == null) {
			dayMin = nightMin;
		}

		BigDecimal dayMax = historyDAO.readExtremValueInTimeRange(device, datapoint, ExtremValueType.MAX,
				TimeRange.DAY, monthStart, monthEnd);
		if (dayMax == null) {
			dayMax = nightMax;
		}

		if (nightMax != null && dayMin != null && nightMax.compareTo(dayMin) > 0) {
			// night 8-25, day 13-34
			// switch values
			BigDecimal temp = nightMax;
			nightMax = dayMin;
			dayMin = temp;
			if (nightMax.compareTo(nightMin) < 0) {
				temp = nightMax;
				nightMax = nightMin;
				nightMin = temp;
			}
			if (dayMax.compareTo(dayMin) < 0) {
				temp = dayMax;
				dayMax = dayMin;
				dayMin = temp;
			}
		}

		TemperatureHistory temperatureHistory = new TemperatureHistory();
		if (nightMin != null || nightMax != null || dayMin != null || dayMax != null) {
			temperatureHistory
					.setDate(Date.from(base.atStartOfDay(ZoneId.systemDefault()).toInstant()).getTime());
			temperatureHistory.setSingleDay(singleDay);
			temperatureHistory.setNightMin(nightMin);
			temperatureHistory.setNightMax(nightMax);
			temperatureHistory.setDayMin(dayMin);
			temperatureHistory.setDayMax(dayMax);
		}

		return temperatureHistory;
	}

	private LocalDateTime toFixedHour(LocalDateTime localDateTime, int hour) {

		LocalDateTime ldt = LocalDateTime.from(localDateTime);

		if (hour == HOURS_IN_DAY) {
			ldt = ldt.plusHours(hour);
			hour = 0;
		}

		ldt = ldt.minusNanos(ldt.getNano());
		ldt = ldt.minusSeconds(ldt.getSecond());
		ldt = ldt.minusMinutes(ldt.getMinute());
		ldt = ldt.withHour(hour);
		return ldt;
	}

	private void calculateElectricPowerConsumption(HistoryModel newModel, LocalDateTime fromDateTime) {

		List<TimestampValuePair> timestampValues = historyDAO.readValues(Device.STROMZAEHLER,
				Datapoint.ENERGY_COUNTER, fromDateTime);

		for (TimestampValuePair pair : timestampValues) {
			PowerConsumptionMonth dest = null;
			for (PowerConsumptionMonth pcm : newModel.getElectricPowerConsumption()) {
				if (isSameMonth(pair.getTimestamp(), pcm.measurePointMaxDateTime())) {
					dest = pcm;
				}
			}
			if (dest == null) {
				dest = new PowerConsumptionMonth();
				if (!newModel.getElectricPowerConsumption().isEmpty()) {
					dest.setMeasurePointMin(newModel.getElectricPowerConsumption()
							.get(newModel.getElectricPowerConsumption().size() - 1).getMeasurePointMax());
					dest.setLastSingleValue(newModel.getElectricPowerConsumption()
							.get(newModel.getElectricPowerConsumption().size() - 1).getLastSingleValue());
				}
				newModel.getElectricPowerConsumption().add(dest);
			}
			addMeasurePoint(dest, pair);
		}
	}

	private void addMeasurePoint(PowerConsumptionMonth pcm, TimestampValuePair measurePoint) {

		if (pcm.getLastSingleValue() != null) {
			if (pcm.getLastSingleValue() < measurePoint.getValue().longValue()) {
				pcm.setPowerConsumption((pcm.getPowerConsumption() != null ? pcm.getPowerConsumption() : 0)
						+ (measurePoint.getValue().longValue() - pcm.getLastSingleValue()));
			} else if (pcm.getLastSingleValue().compareTo(measurePoint.getValue().longValue()) > 0) {
				// overflow
				pcm.setPowerConsumption(pcm.getPowerConsumption() + measurePoint.getValue().longValue());
			}
		}
		pcm.setLastSingleValue(measurePoint.getValue().longValue());
		pcm.setMeasurePointMax(
				measurePoint.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
	}

	private boolean isSameMonth(LocalDateTime date1, LocalDateTime date2) {
		return date1.getYear() == date2.getYear() && date1.getMonthValue() == date2.getMonthValue();
	}
}
