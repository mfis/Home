package homecontroller.domain.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import homecontroller.command.HomematicCommand;
import homecontroller.command.HomematicCommandBuilder;
import homecontroller.dao.HistoryDatabaseDAO;
import homecontroller.database.mapper.TimestampValuePair;
import homecontroller.database.mapper.TimestampValuePairComparator;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.PowerConsumptionDay;
import homecontroller.domain.model.PowerConsumptionMonth;
import homecontroller.domain.model.TemperatureHistory;
import homecontroller.domain.model.TimeRange;
import homecontroller.model.History;
import homecontroller.model.HistoryElement;
import homecontroller.model.HistoryValueType;
import homecontroller.service.HomematicAPI;
import homecontroller.util.HomeUtils;
import homelibrary.dao.ModelObjectDAO;
import homelibrary.homematic.model.Datapoint;
import homelibrary.homematic.model.Device;

@Component
public class HistoryService {

	@Autowired
	private HistoryDatabaseDAO historyDAO;

	@Autowired
	private UploadService uploadService;

	@Autowired
	private HomematicAPI api;
	
	@Autowired
	private HomematicCommandBuilder homematicCommandBuilder;
	
	@Autowired
	private History history;

	private Map<HomematicCommand, List<TimestampValuePair>> entryCache = new HashMap<>();

	private static final int HOURS_IN_DAY = 24;

	private static final long HIGHEST_OUTSIDE_TEMPERATURE_PERIOD_HOURS = HOURS_IN_DAY;

	private static final Log LOG = LogFactory.getLog(HistoryService.class);

	@PostConstruct
	public void init() {

		for (HistoryElement history : history.list()) {
			if (!entryCache.containsKey(history.getCommand())) {
				entryCache.put(history.getCommand(), new LinkedList<TimestampValuePair>());
			}
		}

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
		for (HistoryElement history : history.list()) {
			addEntry(history.getCommand(), new TimestampValuePair(api.getCurrentValuesTimestamp(),
					api.getAsBigDecimal(history.getCommand()), homecontroller.model.HistoryValueType.SINGLE));
		}
	}

	@PreDestroy
	@Scheduled(cron = "0 0 * * * *")
	public void persistCashedValues() {

		Map<HomematicCommand, List<TimestampValuePair>> toInsert = new HashMap<>();
		for (HistoryElement history : history.list()) {

			List<TimestampValuePair> pairs = new LinkedList<>();

			switch (history.getStrategy()) {
			case MIN:
				diffValueCheckedAdd(history, min(entryCache.get(history.getCommand())), pairs);
				break;
			case MAX:
				diffValueCheckedAdd(history, max(entryCache.get(history.getCommand())), pairs);
				break;
			case MIN_MAX:
				diffValueCheckedAdd(history, min(entryCache.get(history.getCommand())), pairs);
				diffValueCheckedAdd(history, max(entryCache.get(history.getCommand())), pairs);
				break;
			case AVG:
				diffValueCheckedAdd(history, avg(entryCache.get(history.getCommand())), pairs);
				break;
			}
			toInsert.put(history.getCommand(), pairs);
		}
		try {
			historyDAO.persistEntries(toInsert);
		} catch (Exception e) {
			LOG.error("Could not persistCashedValues(): ", e);
			increaseTimestamps(toInsert);
			historyDAO.persistEntries(toInsert);
		}
		for (HistoryElement history : history.list()) {
			entryCache.get(history.getCommand()).clear();
		}
	}

	private void increaseTimestamps(Map<HomematicCommand, List<TimestampValuePair>> toInsert) {

		for (Entry<HomematicCommand, List<TimestampValuePair>> entry : toInsert.entrySet()) {
			for (TimestampValuePair pair : entry.getValue()) {
				if (pair != null) {
					pair.setTimestamp(pair.getTimestamp().plusSeconds(1));
				}
			}
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

	private void addEntry(HomematicCommand command, TimestampValuePair pair) {
		if (pair != null && pair.getValue() != null) {
			if (!entryCache.containsKey(command)) {
				entryCache.put(command, new LinkedList<TimestampValuePair>());
			}
			entryCache.get(command).add(pair);
		}
	}

	@Scheduled(fixedDelay = ((1000 * 60 * 5) + 13))
	private synchronized void refreshExtremValues() {
		HistoryModel model = ModelObjectDAO.getInstance().readHistoryModel();
		if (model == null) {
			return;
		}

		BigDecimal maxValue = readExtremValueBetweenWithCache(
				homematicCommandBuilder.read(Device.AUSSENTEMPERATUR, Datapoint.VALUE), HistoryValueType.MAX,
				LocalDateTime.now().minusHours(HIGHEST_OUTSIDE_TEMPERATURE_PERIOD_HOURS), null, null);
		model.setHighestOutsideTemperatureInLast24Hours(maxValue);
	}

	@Scheduled(fixedDelay = (1000 * 60))
	private synchronized void refreshHistoryModel() {
		HistoryModel model = ModelObjectDAO.getInstance().readHistoryModel();
		if (model == null || !model.isInitialized()) {
			return;
		}

		calculateElectricPowerConsumption(model,
				model.getElectricPowerConsumptionMonth().isEmpty() ? null
						: model.getElectricPowerConsumptionMonth()
								.get(model.getElectricPowerConsumptionMonth().size() - 1)
								.measurePointMaxDateTime());

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
		if (!yesterday.empty()) {
			history.add(yesterday);
		}

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

		BigDecimal nightMin = readExtremValueBetweenWithCache(homematicCommandBuilder.read(device, datapoint),
				HistoryValueType.MIN, monthStart, monthEnd, TimeRange.NIGHT);
		if (nightMin == null) { // special case directly after month change
			nightMin = readFirstValueBeforeWithCache(homematicCommandBuilder.read(device, datapoint), monthStart,
					48);
		}

		BigDecimal nightMax = readExtremValueBetweenWithCache(homematicCommandBuilder.read(device, datapoint),
				HistoryValueType.MAX, monthStart, monthEnd, TimeRange.NIGHT);
		if (nightMax == null) {
			nightMax = nightMin;
		}

		BigDecimal dayMin = readExtremValueBetweenWithCache(homematicCommandBuilder.read(device, datapoint),
				HistoryValueType.MIN, monthStart, monthEnd, TimeRange.DAY);
		if (dayMin == null) {
			dayMin = nightMin;
		}

		BigDecimal dayMax = readExtremValueBetweenWithCache(homematicCommandBuilder.read(device, datapoint),
				HistoryValueType.MAX, monthStart, monthEnd, TimeRange.DAY);
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

		List<TimestampValuePair> timestampValues = readValuesWithCache(
				homematicCommandBuilder.read(Device.STROMZAEHLER, Datapoint.ENERGY_COUNTER), fromDateTime);

		if (timestampValues.isEmpty()) {
			return;
		}

		LocalDateTime dayFrom = toFixedHour(LocalDateTime.now().minusDays(31), 0);
		LocalDateTime dayTo = LocalDateTime.now();

		BigDecimal lastSingleValue = timestampValues.get(0).getValue();

		for (TimestampValuePair pair : timestampValues) {
			calculateElectricPowerConsumptionDay(newModel, pair, dayFrom, dayTo, lastSingleValue);
			calculateElectricPowerConsumptionMonth(newModel, pair, lastSingleValue);
			lastSingleValue = pair.getValue();
		}
	}

	private void calculateElectricPowerConsumptionDay(HistoryModel newModel, TimestampValuePair pair,
			LocalDateTime dayFrom, LocalDateTime dayTo, BigDecimal lastSingleValue) {

		if (pair.getTimestamp().isAfter(dayTo) || pair.getTimestamp().isBefore(dayFrom)) {
			return;
		}

		PowerConsumptionDay dest = null;
		for (PowerConsumptionDay pcd : newModel.getElectricPowerConsumptionDay()) {
			if (HomeUtils.isSameDay(pair.getTimestamp(), pcd.measurePointMaxDateTime())) {
				dest = pcd;
				break;
			}
		}
		if (dest == null) {
			dest = new PowerConsumptionDay();
			for (TimeRange range : TimeRange.values()) {
				dest.getValues().put(range, BigDecimal.ZERO);
			}
			newModel.getElectricPowerConsumptionDay().add(dest);
		}
		addMeasurePointDay(dest, pair, lastSingleValue);
	}

	private void calculateElectricPowerConsumptionMonth(HistoryModel newModel, TimestampValuePair pair,
			BigDecimal lastSingleValue) {

		PowerConsumptionMonth dest = null;
		for (PowerConsumptionMonth pcm : newModel.getElectricPowerConsumptionMonth()) {
			if (HomeUtils.isSameMonth(pair.getTimestamp(), pcm.measurePointMaxDateTime())) {
				dest = pcm;
				break;
			}
		}
		if (dest == null) {
			dest = new PowerConsumptionMonth();
			if (!newModel.getElectricPowerConsumptionMonth().isEmpty()) {
				dest.setPowerConsumption(0L);
				dest.setMeasurePointMin(newModel.getElectricPowerConsumptionMonth()
						.get(newModel.getElectricPowerConsumptionMonth().size() - 1).getMeasurePointMax());
			}
			newModel.getElectricPowerConsumptionMonth().add(dest);
		}
		addMeasurePointMonth(dest, pair, lastSingleValue);
	}

	private void addMeasurePointDay(PowerConsumptionDay pcd, TimestampValuePair measurePoint,
			BigDecimal lastSingleValue) {

		TimeRange timeRange = TimeRange.fromDateTime(measurePoint.getTimestamp());
		if (lastSingleValue.longValue() <= measurePoint.getValue().longValue()) {
			pcd.getValues().put(timeRange,
					pcd.getValues().get(timeRange).add(measurePoint.getValue().subtract(lastSingleValue)));
		} else {
			// overflow
			pcd.getValues().put(timeRange, pcd.getValues().get(timeRange).add(measurePoint.getValue()));
		}
		pcd.setMeasurePointMax(
				measurePoint.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
	}

	private void addMeasurePointMonth(PowerConsumptionMonth pcm, TimestampValuePair measurePoint,
			BigDecimal lastSingleValue) {

		if (lastSingleValue != null) {
			if (lastSingleValue.longValue() < measurePoint.getValue().longValue()) {
				pcm.setPowerConsumption((pcm.getPowerConsumption() != null ? pcm.getPowerConsumption() : 0)
						+ (measurePoint.getValue().longValue() - lastSingleValue.longValue()));
			} else if (lastSingleValue.compareTo(measurePoint.getValue()) > 0) {
				// overflow
				pcm.setPowerConsumption(pcm.getPowerConsumption() + measurePoint.getValue().longValue());
			}
		}
		pcm.setMeasurePointMax(
				measurePoint.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
	}

	protected TimestampValuePair min(List<TimestampValuePair> list) {

		list = cleanList(list);
		if (list.isEmpty()) {
			return null;
		}

		TimestampValuePair cmp = null;
		for (TimestampValuePair pair : list) {
			if (pair != null && (cmp == null || cmp.getValue().compareTo(pair.getValue()) > 0)) {
				cmp = pair;
			}
		}
		if (cmp == null) {
			return null;
		} else {
			return new TimestampValuePair(cmp.getTimestamp(), cmp.getValue(), HistoryValueType.MIN);
		}
	}

	protected TimestampValuePair max(List<TimestampValuePair> list) {

		list = cleanList(list);
		if (list.isEmpty()) {
			return null;
		}

		TimestampValuePair cmp = null;
		for (TimestampValuePair pair : list) {
			if (pair != null && (cmp == null || cmp.getValue().compareTo(pair.getValue()) < 0)) {
				cmp = pair;
			}
		}
		if (cmp == null) {
			return null;
		} else {
			return new TimestampValuePair(cmp.getTimestamp(), cmp.getValue(), HistoryValueType.MAX);
		}
	}

	protected TimestampValuePair avg(List<TimestampValuePair> list) {

		list = cleanList(list);
		if (list.isEmpty()) {
			return null;
		}

		BigDecimal sum = BigDecimal.ZERO;
		for (TimestampValuePair pair : list) {
			sum = sum.add(pair.getValue());
		}
		LocalDateTime avgDateTime = null;
		if (list.get(0).getTimestamp() != null && list.get(list.size() - 1).getTimestamp() != null) {
			long minTime = list.get(0).getTimestamp().atZone(ZoneId.systemDefault()).toInstant()
					.toEpochMilli();
			long maxTime = list.get(list.size() - 1).getTimestamp().atZone(ZoneId.systemDefault()).toInstant()
					.toEpochMilli();
			long avgTime = minTime + ((maxTime - minTime) / 2);
			avgDateTime = Instant.ofEpochMilli(avgTime).atZone(ZoneId.systemDefault()).toLocalDateTime();
		}
		return new TimestampValuePair(avgDateTime,
				sum.divide(new BigDecimal(list.size()), new MathContext(3, RoundingMode.HALF_UP)),
				HistoryValueType.AVG);
	}

	protected List<TimestampValuePair> cleanList(List<TimestampValuePair> list) {

		if (list == null || list.isEmpty()) {
			return new LinkedList<>();
		}

		for (int i = list.size() - 1; i >= 0; i--) {
			if (list.get(i) == null) {
				list.remove(i);
			}
		}
		return list;
	}

	protected BigDecimal readExtremValueBetweenWithCache(HomematicCommand command,
			HistoryValueType historyValueType, LocalDateTime fromDateTime, LocalDateTime untilDateTime,
			TimeRange timerange) {

		List<TimestampValuePair> cacheCopy = new LinkedList<>();
		cacheCopy.addAll(entryCache.get(command));

		TimestampValuePair dbPair;
		if (timerange != null) {
			dbPair = historyDAO.readExtremValueInTimeRange(command, historyValueType, timerange, fromDateTime,
					untilDateTime);
		} else {
			dbPair = historyDAO.readExtremValueBetween(command, historyValueType, fromDateTime,
					untilDateTime);
		}

		List<TimestampValuePair> combined = new LinkedList<>();
		if (dbPair != null) {
			combined.add(dbPair);
		}
		for (TimestampValuePair pair : cacheCopy) {
			if (lookupIsTimeBetween(fromDateTime, untilDateTime, timerange, pair)) {
				combined.add(pair);
			}
		}

		TimestampValuePair result = null;
		switch (historyValueType) {
		case MIN:
			result = min(combined);
			break;
		case MAX:
			result = max(combined);
			break;
		case AVG:
			result = avg(combined);
			break;
		default:
			throw new IllegalArgumentException(
					"HistoryValueType not expected:" + historyValueType.toString());
		}
		if (result == null) {
			return null;
		} else {
			return result.getValue();
		}
	}

	private boolean lookupIsTimeBetween(LocalDateTime fromDateTime, LocalDateTime untilDateTime,
			TimeRange timerange, TimestampValuePair pair) {

		boolean isBetween = true;
		if (fromDateTime != null && pair.getTimestamp().isBefore(fromDateTime)) {
			isBetween = false;
		}
		if (isBetween && untilDateTime != null && pair.getTimestamp().isAfter(untilDateTime)) {
			isBetween = false;
		}
		if (isBetween && timerange != null
				&& !timerange.getHoursIntList().contains(pair.getTimestamp().getHour())) {
			isBetween = false;
		}
		return isBetween;
	}

	protected BigDecimal readFirstValueBeforeWithCache(HomematicCommand command, LocalDateTime localDateTime,
			int maxHoursReverse) {

		List<TimestampValuePair> cacheCopy = new LinkedList<>();
		cacheCopy.addAll(entryCache.get(command));
		Collections.reverse(cacheCopy);

		TimestampValuePair dbPair = historyDAO.readFirstValueBefore(command, localDateTime, maxHoursReverse);

		TimestampValuePair cachedPair = null;
		LocalDateTime compareDateTime = localDateTime.minusHours(maxHoursReverse);
		for (TimestampValuePair pair : cacheCopy) {
			if (pair.getTimestamp().isBefore(localDateTime) && pair.getTimestamp().isAfter(compareDateTime)) {
				cachedPair = pair;
				break;
			}
		}

		List<TimestampValuePair> combined = new LinkedList<>();
		if (dbPair != null) {
			combined.add(dbPair);
		}
		if (cachedPair != null) {
			combined.add(cachedPair);
		}
		if (combined.isEmpty()) {
			return null;
		}
		Collections.sort(combined, new TimestampValuePairComparator());
		return combined.get(combined.size() - 1).getValue();
	}

	protected List<TimestampValuePair> readValuesWithCache(HomematicCommand command,
			LocalDateTime optionalFromDateTime) {

		List<TimestampValuePair> cacheCopy = new LinkedList<>();
		cacheCopy.addAll(entryCache.get(command));

		List<TimestampValuePair> db = historyDAO.readValues(command, optionalFromDateTime);
		List<TimestampValuePair> combined = new LinkedList<>();
		combined.addAll(db);

		for (TimestampValuePair cachePair : cacheCopy) {
			if (optionalFromDateTime == null || optionalFromDateTime.isBefore(cachePair.getTimestamp())) {
				combined.add(cachePair);
			}
		}
		Collections.sort(combined, new TimestampValuePairComparator());

		return combined;
	}

	public void diffValueCheckedAdd(HistoryElement history, TimestampValuePair pair, List<TimestampValuePair> dest) {

		TimestampValuePair lastValue = historyDAO.readLatestValue(history.getCommand());

		if (lastValue == null) {
			dest.add(pair);
			return;
		}

		long lastValueRounded = lastValue.getValue().setScale(0, RoundingMode.HALF_UP).longValue();
		long actualValueRounded = pair.getValue().setScale(0, RoundingMode.HALF_UP).longValue();
		if ((actualValueRounded - lastValueRounded) >= history.getValueDifferenceToSave()) {
			dest.add(pair);
			return;
		}

		if (TimeRange.fromDateTime(pair.getTimestamp()) != TimeRange.fromDateTime(lastValue.getTimestamp())) {
			dest.add(pair);
		}
	}

	protected Map<HomematicCommand, List<TimestampValuePair>> getEntryCache() {
		return entryCache;
	}
}
