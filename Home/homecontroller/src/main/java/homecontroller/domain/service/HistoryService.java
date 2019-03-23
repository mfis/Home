package homecontroller.domain.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import homecontroller.dao.ModelDAO;
import homecontroller.database.mapper.TimestampValuePair;
import homecontroller.domain.model.Datapoint;
import homecontroller.domain.model.Device;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.PowerConsumptionMonth;
import homecontroller.domain.model.TemperatureHistory;
import homecontroller.domain.service.HistoryDAO.ExtremValueType;
import homecontroller.domain.service.HistoryDAO.TimeRange;

@Component
public class HistoryService {

	@Autowired
	private HistoryDAO historyDAO;

	private static final int HOURS_IN_DAY = 24;

	private static final long HIGHEST_OUTSIDE_TEMPERATURE_PERIOD_HOURS = HOURS_IN_DAY;

	@PostConstruct
	public void init() {

		try {
			refreshHistoryModelComplete();
		} catch (Exception e) {
			LogFactory.getLog(HistoryService.class).error("Could not initialize HistoryService completly.",
					e);
		}
	}

	@Scheduled(cron = "5 0 0 * * *")
	private void refreshHistoryModelComplete() {

		HistoryModel oldModel = ModelDAO.getInstance().readHistoryModel();
		if (oldModel != null) {
			oldModel.setInitialized(false);
		}

		HistoryModel newModel = new HistoryModel();

		calculateElectricPowerConsumption(newModel, null);
		calculateOutsideTemperatureHistory(newModel);

		newModel.setInitialized(true);
		ModelDAO.getInstance().write(newModel);
	}

	@Scheduled(fixedDelay = (1000 * 60 * 3))
	private void refreshHistoryModel() {

		HistoryModel model = ModelDAO.getInstance().readHistoryModel();
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

		if (model.getOutsideTemperature().isEmpty()) {
			model.getOutsideTemperature().add(readDayOutsideTemperatureHistory(LocalDateTime.now()));
		} else {
			model.getOutsideTemperature().set(0, readDayOutsideTemperatureHistory(LocalDateTime.now()));
		}
	}

	private void calculateOutsideTemperatureHistory(HistoryModel historyModel) {

		historyModel.getOutsideTemperature().clear();
		LocalDateTime base = LocalDateTime.now();

		TemperatureHistory today = readDayOutsideTemperatureHistory(base);

		historyModel.getOutsideTemperature().add(today);
		TemperatureHistory yesterday = readDayOutsideTemperatureHistory(base.minusHours(HOURS_IN_DAY));
		historyModel.getOutsideTemperature().add(yesterday);

		TemperatureHistory monthHistory;
		YearMonth yearMonth = YearMonth.now();
		do {
			monthHistory = readMonthOutsideTemperatureHistory(yearMonth);
			if (!monthHistory.empty()) {
				historyModel.getOutsideTemperature().add(monthHistory);
				yearMonth = yearMonth.minusMonths(1);
			}
		} while (!monthHistory.empty());
	}

	private TemperatureHistory readDayOutsideTemperatureHistory(LocalDateTime localDateTime) {

		LocalDateTime nightStart = toFixedHour(localDateTime, 0);
		LocalDateTime dayEnd = toFixedHour(localDateTime, HOURS_IN_DAY);
		return readOutsideTemperatureHistory(localDateTime.toLocalDate(), true, nightStart, dayEnd);
	}

	private TemperatureHistory readMonthOutsideTemperatureHistory(YearMonth yearMonth) {

		LocalDateTime monthStart = LocalDateTime.of(yearMonth.atDay(1),
				toFixedHour(LocalDateTime.now(), 0).toLocalTime());
		LocalDateTime monthEnd = toFixedHour(LocalDateTime.of(yearMonth.atEndOfMonth(), LocalTime.now()),
				HOURS_IN_DAY);
		return readOutsideTemperatureHistory(yearMonth.atDay(1), false, monthStart, monthEnd);
	}

	private TemperatureHistory readOutsideTemperatureHistory(LocalDate base, boolean singleDay,
			LocalDateTime monthStart, LocalDateTime monthEnd) {

		BigDecimal nightMin = historyDAO.readExtremValueInTimeRange(Device.AUSSENTEMPERATUR, Datapoint.VALUE,
				ExtremValueType.MIN, TimeRange.NIGHT, monthStart, monthEnd);
		if (nightMin == null) {
			nightMin = historyDAO.readFirstValueBefore(Device.AUSSENTEMPERATUR, Datapoint.VALUE, monthStart,
					48);
		}

		BigDecimal nightMax = historyDAO.readExtremValueInTimeRange(Device.AUSSENTEMPERATUR, Datapoint.VALUE,
				ExtremValueType.MAX, TimeRange.NIGHT, monthStart, monthEnd);
		if (nightMax == null) {
			nightMax = nightMin;
		}

		BigDecimal dayMin = historyDAO.readExtremValueInTimeRange(Device.AUSSENTEMPERATUR, Datapoint.VALUE,
				ExtremValueType.MIN, TimeRange.DAY, monthStart, monthEnd);
		if (dayMin == null) {
			dayMin = nightMin;
		}

		BigDecimal dayMax = historyDAO.readExtremValueInTimeRange(Device.AUSSENTEMPERATUR, Datapoint.VALUE,
				ExtremValueType.MAX, TimeRange.DAY, monthStart, monthEnd);
		if (dayMax == null) {
			dayMax = nightMax;
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
				if (isSameMonth(pair.getTimeatamp(), pcm.measurePointMaxDateTime())) {
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
			if (pcm.getLastSingleValue() < measurePoint.getValue()) {
				pcm.setPowerConsumption((pcm.getPowerConsumption() != null ? pcm.getPowerConsumption() : 0)
						+ (measurePoint.getValue() - pcm.getLastSingleValue()));
			} else if (pcm.getLastSingleValue().compareTo(measurePoint.getValue()) > 0) {
				// overflow
				pcm.setPowerConsumption(pcm.getPowerConsumption() + measurePoint.getValue());
			}
		}
		pcm.setLastSingleValue(measurePoint.getValue());
		pcm.setMeasurePointMax(
				measurePoint.getTimeatamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
	}

	private boolean isSameMonth(LocalDateTime date1, LocalDateTime date2) {
		return date1.getYear() == date2.getYear() && date1.getMonthValue() == date2.getMonthValue();
	}
}
