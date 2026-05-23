package de.fimatas.home.controller.domain.service;

import de.fimatas.home.controller.api.HomematicAPI;
import de.fimatas.home.controller.command.AbstractCommand;
import de.fimatas.home.controller.command.HomematicCommand;
import de.fimatas.home.controller.command.HomematicCommandBuilder;
import de.fimatas.home.controller.command.PersistentCacheCommand;
import de.fimatas.home.controller.dao.ApplicationDatabaseDAO;
import de.fimatas.home.controller.dao.PersistentCacheDAO;
import de.fimatas.home.controller.database.mapper.TimestampValuePair;
import de.fimatas.home.controller.database.mapper.TimestampValuePairComparator;
import de.fimatas.home.controller.model.History;
import de.fimatas.home.controller.model.HistoryElement;
import de.fimatas.home.controller.model.HistoryValueType;
import de.fimatas.home.controller.service.UploadService;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.homematic.model.Datapoint;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.util.HomeUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.Map.Entry;

@Component
@DependsOn(ApplicationDatabaseDAO.APPLICATION_DATABASE_DAO)
public class HistoryService {

    @Autowired
    private ApplicationDatabaseDAO historyDAO;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private HomematicAPI api;

    @Autowired
    private PersistentCacheDAO persistentCacheDAO;

    @Autowired
    private HomematicCommandBuilder homematicCommandBuilder;

    @Autowired
    private History history;

    private final Map<AbstractCommand, List<TimestampValuePair>> entryCache = new HashMap<>();

    private static final int HOURS_IN_DAY = 24;

    public static final int MAX_HOURS_REVERSE_FOR_TIMERANGE = 48;

    private static final long HIGHEST_OUTSIDE_TEMPERATURE_PERIOD_HOURS = HOURS_IN_DAY;

    public static final long MAX_DIFF_KWH_POWER_READ_FAILURE = 10;

    private static final Log LOG = LogFactory.getLog(HistoryService.class);

    @PostConstruct
    public void init() {

        for (HistoryElement historyElement : history.list()) {
            if (!entryCache.containsKey(historyElement.getCommand())) {
                entryCache.put(historyElement.getCommand(), new LinkedList<>());
            }
        }
    }

    public void saveNewValues() {
        for (HistoryElement historyElement : history.list()) {
            if(historyElement.getCommand() instanceof HomematicCommand hc) {
                if(!api.isDeviceUnreachableOrNotSending(hc.getDevice())){
                    addEntry(hc, new TimestampValuePair(api.getCurrentValuesTimestamp(),
                            api.getAsBigDecimal(hc), de.fimatas.home.controller.model.HistoryValueType.SINGLE));
                }
            } else if(historyElement.getCommand() instanceof PersistentCacheCommand pcc) {
                var value = persistentCacheDAO.read(pcc.getPersistentCacheKey(), BigDecimal.class);
                if(value != null) {
                    addEntry(pcc, new TimestampValuePair(api.getCurrentValuesTimestamp(),
                            value.getValue(), de.fimatas.home.controller.model.HistoryValueType.SINGLE));
                }
            } else {
                throw new IllegalArgumentException("Unknown command type class: " +  historyElement.getClass().getSimpleName());
            }

        }
    }

    @PreDestroy
    @Scheduled(cron = "0 0 * * * *")
    public synchronized void persistCashedValues() {

        Map<AbstractCommand, List<TimestampValuePair>> toInsert = new HashMap<>();
        for (HistoryElement historyElement : history.list()) {

            List<TimestampValuePair> pairs = new LinkedList<>();

            switch (historyElement.getStrategy()) {
            case MIN:
                diffValueCheckedAdd(historyElement, min(entryCache.get(historyElement.getCommand())), pairs);
                break;
            case MAX:
                diffValueCheckedAdd(historyElement, max(entryCache.get(historyElement.getCommand())), pairs);
                break;
            case AVG:
                diffValueCheckedAdd(historyElement, avg(entryCache.get(historyElement.getCommand())), pairs);
                break;
            }
            toInsert.put(historyElement.getCommand(), pairs);
        }

        try {
            historyDAO.persistEntries(toInsert);
        } catch (Exception e) {
            LOG.error("Could not persistCashedValues(): ", e);
            increaseTimestamps(toInsert);
            historyDAO.persistEntries(toInsert);
        }
        for (HistoryElement historyElement : history.list()) {
            entryCache.get(historyElement.getCommand()).clear();
        }
    }

    @Scheduled(cron = "30 0 * * * *")
    public synchronized void refreshHistoryModelComplete() {

        HistoryModel oldModel = ModelObjectDAO.getInstance().readHistoryModel();
        if (oldModel != null) {
            oldModel.setInitialized(false);
        }

        HistoryModel newModel = new HistoryModel();

        calculatePowerConsumption(newModel.getPurchasedElectricPowerConsumptionDay(),
            newModel.getPurchasedElectricPowerConsumptionMonth(), history.get(History.HIST_STROM_BEZUG).getCommand(), null);
        calculatePowerConsumption(newModel.getFeedElectricPowerConsumptionDay(),
                newModel.getFeedElectricPowerConsumptionMonth(), history.get(History.HIST_STROM_EINSPEISUNG).getCommand(), null);

        calculatePowerConsumption(newModel.getSelfusedElectricPowerConsumptionDay(),
                newModel.getSelfusedElectricPowerConsumptionMonth(), history.get(History.HIST_STROM_CONSUMPTION).getCommand(), null);
        calculatePowerConsumption(newModel.getProducedElectricPowerDay(),
                newModel.getProducedElectricPowerMonth(), history.get(History.HIST_STROM_PRODUCTION).getCommand(), null);

        calculatePowerConsumption(newModel.getHeatpumpBasementElectricPowerConsumptionDay(),
                newModel.getHeatpumpBasementElectricPowerConsumptionMonth(), history.get(History.HIST_HEATPUMP_BASEMENT_CONSUMPTION).getCommand(), null);

        calculatePowerConsumption(newModel.getHeatpumpBasementWarmthPowerProductionDay(),
                newModel.getHeatpumpBasementWarmthPowerProductionMonth(), history.get(History.HIST_HEATPUMP_BASEMENT_PRODUCTION).getCommand(), null);

        calculatePowerConsumption(newModel.getWallboxElectricPowerConsumptionDay(),
            newModel.getWallboxElectricPowerConsumptionMonth(), history.get(History.HIST_STROM_WALLBOX).getCommand(), null);

        calculatePowerConsumption(newModel.getGasConsumptionDay(),
                newModel.getGasConsumptionMonth(), history.get(History.HIST_GAS).getCommand(), null);

        calculateTemperatureHistory(newModel.getOutsideTemperature(), Device.AUSSENTEMPERATUR, Datapoint.VALUE);
        calculateTemperatureHistory(newModel.getKidsRoom1Temperature(), Device.THERMOMETER_KINDERZIMMER_1,
            Datapoint.ACTUAL_TEMPERATURE);
        calculateTemperatureHistory(newModel.getKidsRoom2Temperature(), Device.THERMOMETER_KINDERZIMMER_2,
                Datapoint.ACTUAL_TEMPERATURE);
        calculateTemperatureHistory(newModel.getBedRoomTemperature(), Device.THERMOMETER_SCHLAFZIMMER,
            Datapoint.ACTUAL_TEMPERATURE);
        calculateTemperatureHistory(newModel.getLaundryTemperature(), Device.THERMOMETER_WASCHKUECHE,
            Datapoint.ACTUAL_TEMPERATURE);

        BigDecimal maxValue =
                readExtremValueBetweenWithCache(homematicCommandBuilder.read(Device.AUSSENTEMPERATUR, Datapoint.VALUE),
                        HistoryValueType.MAX, LocalDateTime.now().minusHours(HIGHEST_OUTSIDE_TEMPERATURE_PERIOD_HOURS), null, null);
        newModel.setHighestOutsideTemperatureInLast24Hours(maxValue);

        newModel.setInitialized(true);
        ModelObjectDAO.getInstance().write(newModel);
        refreshHistoryModel();
    }

    @Scheduled(cron = "30 1-59 * * * *")
    public synchronized void refreshHistoryModel() {
        HistoryModel model = ModelObjectDAO.getInstance().readHistoryModel();
        if (model == null || !model.isInitialized()) {
            return;
        }

        calculatePowerConsumption(model.getPurchasedElectricPowerConsumptionDay(),
            model.getPurchasedElectricPowerConsumptionMonth(), history.get(History.HIST_STROM_BEZUG).getCommand(),
            model.getPurchasedElectricPowerConsumptionMonth().isEmpty() ? null : model.getPurchasedElectricPowerConsumptionMonth()
                .get(model.getPurchasedElectricPowerConsumptionMonth().size() - 1).measurePointMaxDateTime());

        calculatePowerConsumption(model.getFeedElectricPowerConsumptionDay(),
                model.getFeedElectricPowerConsumptionMonth(), history.get(History.HIST_STROM_EINSPEISUNG).getCommand(),
                model.getFeedElectricPowerConsumptionMonth().isEmpty() ? null : model.getFeedElectricPowerConsumptionMonth()
                        .get(model.getFeedElectricPowerConsumptionMonth().size() - 1).measurePointMaxDateTime());

        calculatePowerConsumption(model.getSelfusedElectricPowerConsumptionDay(),
                model.getSelfusedElectricPowerConsumptionMonth(), history.get(History.HIST_STROM_CONSUMPTION).getCommand(),
                model.getSelfusedElectricPowerConsumptionMonth().isEmpty() ? null : model.getSelfusedElectricPowerConsumptionMonth()
                        .get(model.getSelfusedElectricPowerConsumptionMonth().size() - 1).measurePointMaxDateTime());

        calculatePowerConsumption(model.getProducedElectricPowerDay(),
                model.getProducedElectricPowerMonth(), history.get(History.HIST_STROM_PRODUCTION).getCommand(),
                model.getProducedElectricPowerMonth().isEmpty() ? null : model.getProducedElectricPowerMonth()
                        .get(model.getProducedElectricPowerMonth().size() - 1).measurePointMaxDateTime());

        calculatePowerConsumption(model.getHeatpumpBasementElectricPowerConsumptionDay(),
                model.getHeatpumpBasementElectricPowerConsumptionMonth(), history.get(History.HIST_HEATPUMP_BASEMENT_CONSUMPTION).getCommand(),
                model.getHeatpumpBasementElectricPowerConsumptionMonth().isEmpty() ? null : model.getHeatpumpBasementElectricPowerConsumptionMonth()
                        .get(model.getHeatpumpBasementElectricPowerConsumptionMonth().size() - 1).measurePointMaxDateTime());

        calculatePowerConsumption(model.getHeatpumpBasementWarmthPowerProductionDay(),
                model.getHeatpumpBasementWarmthPowerProductionMonth(), history.get(History.HIST_HEATPUMP_BASEMENT_PRODUCTION).getCommand(),
                model.getHeatpumpBasementWarmthPowerProductionMonth().isEmpty() ? null : model.getHeatpumpBasementWarmthPowerProductionMonth()
                        .get(model.getHeatpumpBasementWarmthPowerProductionMonth().size() - 1).measurePointMaxDateTime());

        calculatePowerConsumption(model.getWallboxElectricPowerConsumptionDay(),
            model.getWallboxElectricPowerConsumptionMonth(), history.get(History.HIST_STROM_WALLBOX).getCommand(),
            model.getWallboxElectricPowerConsumptionMonth().isEmpty() ? null : model.getWallboxElectricPowerConsumptionMonth()
                .get(model.getWallboxElectricPowerConsumptionMonth().size() - 1).measurePointMaxDateTime());

        calculatePowerConsumption(model.getGasConsumptionDay(),
                model.getGasConsumptionMonth(), history.get(History.HIST_GAS).getCommand(),
                model.getGasConsumptionMonth().isEmpty() ? null : model.getGasConsumptionMonth()
                        .get(model.getGasConsumptionMonth().size() - 1).measurePointMaxDateTime());

        updateTemperatureHistory(model.getOutsideTemperature(), Device.AUSSENTEMPERATUR, Datapoint.VALUE);
        updateTemperatureHistory(model.getKidsRoom1Temperature(), Device.THERMOMETER_KINDERZIMMER_1, Datapoint.ACTUAL_TEMPERATURE);
        updateTemperatureHistory(model.getKidsRoom2Temperature(), Device.THERMOMETER_KINDERZIMMER_2, Datapoint.ACTUAL_TEMPERATURE);
        updateTemperatureHistory(model.getBedRoomTemperature(), Device.THERMOMETER_SCHLAFZIMMER, Datapoint.ACTUAL_TEMPERATURE);
        updateTemperatureHistory(model.getLaundryTemperature(), Device.THERMOMETER_WASCHKUECHE, Datapoint.ACTUAL_TEMPERATURE);

        model.updateDateTime();
        uploadService.uploadToClient(model);
    }

    private void increaseTimestamps(Map<AbstractCommand, List<TimestampValuePair>> toInsert) {

        for (Entry<AbstractCommand, List<TimestampValuePair>> entry : toInsert.entrySet()) {
            for (TimestampValuePair pair : entry.getValue()) {
                if (pair != null) {
                    pair.setTimestamp(pair.getTimestamp().plusSeconds(1));
                }
            }
        }
    }

    private void addEntry(AbstractCommand command, TimestampValuePair pair) {
        if (pair != null && pair.getValue() != null) {
            if (!entryCache.containsKey(command)) {
                entryCache.put(command, new LinkedList<>());
            }
            entryCache.get(command).add(pair);
        }
    }

    private void updateTemperatureHistory(List<TemperatureHistory> history, Device device, Datapoint datapoint) {

        if (history.isEmpty()) {
            history.add(readDayTemperatureHistory(LocalDateTime.now(), device, datapoint));
        } else {
            history.set(0, readDayTemperatureHistory(LocalDateTime.now(), device, datapoint));
        }
    }

    private void calculateTemperatureHistory(List<TemperatureHistory> history, Device device, Datapoint datapoint) {

        history.clear();
        LocalDateTime base = LocalDateTime.now();

        TemperatureHistory today = readDayTemperatureHistory(base, device, datapoint);
        history.add(today);

        TemperatureHistory yesterday = readDayTemperatureHistory(base.minusHours(HOURS_IN_DAY), device, datapoint);
        if (!yesterday.empty()) {
            history.add(yesterday);
        }

        TemperatureHistory beforeYesterday = readDayTemperatureHistory(base.minusHours(HOURS_IN_DAY * 2), device, datapoint);
        if (!beforeYesterday.empty()) {
            history.add(beforeYesterday);
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

    private TemperatureHistory readDayTemperatureHistory(LocalDateTime localDateTime, Device device, Datapoint datapoint) {

        LocalDateTime nightStart = toFixedHour(localDateTime, 0);
        LocalDateTime dayEnd = toFixedHour(localDateTime, HOURS_IN_DAY);
        return readTemperatureHistory(localDateTime.toLocalDate(), true, nightStart, dayEnd, device, datapoint);
    }

    private TemperatureHistory readMonthTemperatureHistory(YearMonth yearMonth, Device device, Datapoint datapoint) {

        LocalDateTime monthStart = LocalDateTime.of(yearMonth.atDay(1), toFixedHour(LocalDateTime.now(), 0).toLocalTime());
        LocalDateTime monthEnd = toFixedHour(LocalDateTime.of(yearMonth.atEndOfMonth(), LocalTime.now()), HOURS_IN_DAY);
        return readTemperatureHistory(yearMonth.atDay(1), false, monthStart, monthEnd, device, datapoint);
    }

    private TemperatureHistory readTemperatureHistory(LocalDate base, boolean singleDay, LocalDateTime monthStart,
            LocalDateTime monthEnd, Device device, Datapoint datapoint) {

        BigDecimal min = readExtremValueBetweenWithCache(homematicCommandBuilder.read(device, datapoint),
            HistoryValueType.MIN, monthStart, monthEnd, null);
        if (min == null) { // special case directly after month change
            min = readFirstValueBeforeWithCache(homematicCommandBuilder.read(device, datapoint), monthStart);
        }

        BigDecimal max = readExtremValueBetweenWithCache(homematicCommandBuilder.read(device, datapoint),
            HistoryValueType.MAX, monthStart, monthEnd, null);
        if (max == null) {
            max = min;
        }

        TemperatureHistory temperatureHistory = new TemperatureHistory();
        if (min != null || max != null) {
            temperatureHistory.setDate(Date.from(base.atStartOfDay(ZoneId.systemDefault()).toInstant()).getTime());
            temperatureHistory.setSingleDay(singleDay);
            temperatureHistory.setMin(min);
            temperatureHistory.setMax(max);
        }

        return temperatureHistory;
    }

    private LocalDateTime toFixedHour(LocalDateTime localDateTime, int hour) {

        //noinspection RedundantJavaTimeOperations
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

    private void calculatePowerConsumption(List<PowerConsumptionDay> day, List<PowerConsumptionMonth> month,
                                           AbstractCommand command, LocalDateTime fromDateTime) {

        List<TimestampValuePair> timestampValues = readValuesWithCache(command, fromDateTime);

        if (timestampValues.isEmpty()) {
            return;
        }

        LocalDateTime dayFrom = toFixedHour(LocalDateTime.now().minusDays(31), 0);
        LocalDateTime dayTo = LocalDateTime.now();

        BigDecimal lastSingleValue = timestampValues.get(0).getValue();

        for (TimestampValuePair pair : timestampValues) {
            calculateElectricPowerConsumptionDay(day, pair, dayFrom, dayTo,
                lastSingleValue);
            calculateElectricPowerConsumptionMonth(month, pair, lastSingleValue);
            lastSingleValue = pair.getValue();
        }
    }

    private void calculateElectricPowerConsumptionDay(List<PowerConsumptionDay> powerConsumptionDay, TimestampValuePair pair,
            LocalDateTime dayFrom,
            LocalDateTime dayTo, BigDecimal lastSingleValue) {

        if (pair.getTimestamp().isAfter(dayTo) || pair.getTimestamp().isBefore(dayFrom)) {
            return;
        }

        PowerConsumptionDay dest = null;
        for (PowerConsumptionDay pcd : powerConsumptionDay) {
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
            powerConsumptionDay.add(dest);
        }
        addPowerCounterMeasurePointDay(dest, pair, lastSingleValue);
    }

    private void calculateElectricPowerConsumptionMonth(List<PowerConsumptionMonth> powerConsumptionMonth,
            TimestampValuePair pair,
            BigDecimal lastSingleValue) {

        PowerConsumptionMonth dest = null;
        for (PowerConsumptionMonth pcm : powerConsumptionMonth) {
            if (HomeUtils.isSameMonth(pair.getTimestamp(), pcm.measurePointMaxDateTime())) {
                dest = pcm;
                break;
            }
        }
        if (dest == null) {
            dest = new PowerConsumptionMonth();
            if (!powerConsumptionMonth.isEmpty()) {
                dest.setPowerConsumption(BigDecimal.ZERO);
                //dest.setMeasurePointMin(powerConsumptionMonth.get(powerConsumptionMonth.size() - 1).getMeasurePointMax());
            }
            powerConsumptionMonth.add(dest);
        }
        addPowerCounterMeasurePointMonth(dest, pair, lastSingleValue);
    }

    private void addPowerCounterMeasurePointDay(PowerConsumptionDay pcd, TimestampValuePair measurePoint, BigDecimal lastSingleValue) {

        TimeRange timeRange = TimeRange.fromDateTime(measurePoint.getTimestamp());
        if (lastSingleValue.longValue() <= measurePoint.getValue().longValue()) {
            pcd.getValues().put(timeRange,
                pcd.getValues().get(timeRange).add(measurePoint.getValue().subtract(lastSingleValue)));
        } else {
            // overflow
            long diff = lastSingleValue.longValue() - measurePoint.getValue().longValue();
            if(diff > 0 && diff <= MAX_DIFF_KWH_POWER_READ_FAILURE){
                // failure value read, ignore
                LOG.debug("addPowerCounterMeasurePointDay - failure value read, ignore - diff = " + diff);
            } else {
                // real overflow
                pcd.getValues().put(timeRange, pcd.getValues().get(timeRange).add(measurePoint.getValue()));
            }
        }
        pcd.setMeasurePointMax(measurePoint.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    private void addPowerCounterMeasurePointMonth(PowerConsumptionMonth pcm, TimestampValuePair measurePoint, BigDecimal lastSingleValue) {

        if (lastSingleValue != null) {
            if (lastSingleValue.compareTo(measurePoint.getValue()) < 0) {
                pcm.setPowerConsumption((pcm.getPowerConsumption() != null ? pcm.getPowerConsumption() : BigDecimal.ZERO)
                        .add(measurePoint.getValue().subtract(lastSingleValue)));
            } else if (lastSingleValue.compareTo(measurePoint.getValue()) > 0) {
                // overflow
                if(pcm.getPowerConsumption() == null){
                    // no pcm values recorded (after restart)
                    pcm.setPowerConsumption(lastSingleValue.add(measurePoint.getValue()));
                }else {
                    long diff = lastSingleValue.longValue() - measurePoint.getValue().longValue();
                    if(diff > 0 && diff <= MAX_DIFF_KWH_POWER_READ_FAILURE){
                        // failure value read, ignore
                        LOG.debug("addPowerCounterMeasurePointMonth - failure value read, ignore - diff = " + diff);
                    } else {
                        // real overflow
                        pcm.setPowerConsumption(pcm.getPowerConsumption().add(measurePoint.getValue()));
                    }
                }

            }
        }
        pcm.setMeasurePointMax(measurePoint.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
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
            long minTime = list.get(0).getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long maxTime = list.get(list.size() - 1).getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long avgTime = minTime + ((maxTime - minTime) / 2);
            avgDateTime = Instant.ofEpochMilli(avgTime).atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return new TimestampValuePair(avgDateTime,
            sum.divide(new BigDecimal(list.size()), new MathContext(3, RoundingMode.HALF_UP)), HistoryValueType.AVG);
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

    protected BigDecimal readExtremValueBetweenWithCache(HomematicCommand command, HistoryValueType historyValueType,
            LocalDateTime fromDateTime, LocalDateTime untilDateTime, List<TimeRange> timeranges) {

        List<TimestampValuePair> cacheCopy = new LinkedList<>(entryCache.get(command));

        TimestampValuePair dbPair;
        if (timeranges != null) {
            dbPair = historyDAO.readExtremValueInTimeRange(command, historyValueType, fromDateTime, untilDateTime, timeranges);
        } else {
            dbPair = historyDAO.readExtremValueBetween(command, historyValueType, fromDateTime, untilDateTime);
        }

        List<TimestampValuePair> combined = new LinkedList<>();
        if (dbPair != null) {
            combined.add(dbPair);
        }
        for (TimestampValuePair pair : cacheCopy) {
            if (lookupIsTimeBetween(fromDateTime, untilDateTime, pair, timeranges)) {
                combined.add(pair);
            }
        }

        TimestampValuePair result = switch (historyValueType) {
            case MIN -> min(combined);
            case MAX -> max(combined);
            case AVG -> avg(combined);
            default -> throw new IllegalArgumentException("HistoryValueType not expected:" + historyValueType);
        };
        if (result == null) {
            return null;
        } else {
            return result.getValue();
        }
    }

    private boolean lookupIsTimeBetween(LocalDateTime fromDateTime, LocalDateTime untilDateTime,
            TimestampValuePair pair, List<TimeRange> timeranges) {

        boolean isBetween = fromDateTime == null || !pair.getTimestamp().isBefore(fromDateTime);

        if (isBetween && untilDateTime != null && pair.getTimestamp().isAfter(untilDateTime)) {
            isBetween = false;
        }
        if (isBetween && timeranges != null && !TimeRange.hoursIntList(timeranges).contains(pair.getTimestamp().getHour())) {
            isBetween = false;
        }
        return isBetween;
    }

    protected BigDecimal readFirstValueBeforeWithCache(HomematicCommand command, LocalDateTime localDateTime) {

        List<TimestampValuePair> cacheCopy = new LinkedList<>(entryCache.get(command));
        Collections.reverse(cacheCopy);

        TimestampValuePair dbPair = historyDAO.readFirstValueBefore(command, localDateTime, MAX_HOURS_REVERSE_FOR_TIMERANGE);

        TimestampValuePair cachedPair = null;
        LocalDateTime compareDateTime = localDateTime.minusHours(MAX_HOURS_REVERSE_FOR_TIMERANGE);
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
        combined.sort(new TimestampValuePairComparator());
        return combined.get(combined.size() - 1).getValue();
    }

    protected List<TimestampValuePair> readValuesWithCache(AbstractCommand command, LocalDateTime optionalFromDateTime) {

        List<TimestampValuePair> cacheCopy = new LinkedList<>(entryCache.get(command));

        List<TimestampValuePair> db = historyDAO.readValues(command, optionalFromDateTime);
        List<TimestampValuePair> combined = new LinkedList<>(db);

        for (TimestampValuePair cachePair : cacheCopy) {
            if (optionalFromDateTime == null || optionalFromDateTime.isBefore(cachePair.getTimestamp())) {
                combined.add(cachePair);
            }
        }
        combined.sort(new TimestampValuePairComparator());

        return combined;
    }

    public void diffValueCheckedAdd(HistoryElement history, TimestampValuePair pair, List<TimestampValuePair> dest) {

        TimestampValuePair lastValue = historyDAO.readLatestValue(history.getCommand());

        if (pair == null || pair.getValue() == null) {
            return;
        }

        if (lastValue == null || lastValue.getValue() == null) {
            dest.add(pair);
            return;
        }

        if(lastValue.getValue().subtract(pair.getValue()).abs().compareTo(history.getValueDifferenceToSave()) >= 0){
            dest.add(pair);
            return;
        }

        long lastValueRounded = lastValue.getValue().setScale(0, RoundingMode.HALF_UP).longValue();
        long actualValueRounded = pair.getValue().setScale(0, RoundingMode.HALF_UP).longValue();
        if (new BigDecimal(Math.abs(actualValueRounded - lastValueRounded)).compareTo(history.getValueDifferenceToSave()) >= 0) {
            dest.add(pair);
            return;
        }

        if (TimeRange.fromDateTime(pair.getTimestamp()) != TimeRange.fromDateTime(lastValue.getTimestamp())) {
            dest.add(pair);
        }
    }

    protected Map<AbstractCommand, List<TimestampValuePair>> getEntryCache() {
        return entryCache;
    }
}
