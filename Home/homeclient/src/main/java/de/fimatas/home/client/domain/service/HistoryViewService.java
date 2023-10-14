package de.fimatas.home.client.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.fimatas.home.library.homematic.model.Device;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import de.fimatas.home.client.domain.model.ChartEntry;
import de.fimatas.home.client.domain.model.HistoryEntry;
import de.fimatas.home.library.domain.model.HistoryModel;
import de.fimatas.home.library.domain.model.HouseModel;
import de.fimatas.home.library.domain.model.PowerConsumptionMonth;
import de.fimatas.home.library.domain.model.TemperatureHistory;

import static de.fimatas.home.library.util.HomeUtils.buildDecimalFormat;

@Component
@CommonsLog
public class HistoryViewService {

    private static final BigDecimal BD100 = new BigDecimal(100);

    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

    private static final int COMPARE_PERCENTAGE_GREEN_UNTIL = -1;

    private static final int COMPARE_PERCENTAGE_GRAY_UNTIL = 2;

    private static final int COMPARE_PERCENTAGE_ORANGE_UNTIL = 15;

    @Autowired
    private ViewFormatter viewFormatter;

    public void fillHistoryViewModel(Model model, HistoryModel history, HouseModel house, String key) {

        if (key.equals(Device.STROMZAEHLER_BEZUG.historyKeyPrefix())) {
            fillPowerHistoryMonthViewModel(model, Device.STROMZAEHLER_BEZUG, history.getPurchasedElectricPowerConsumptionMonth());
            List<ChartEntry> dayViewModel =
                    viewFormatter.fillPowerHistoryDayViewModel(Device.STROMZAEHLER_BEZUG, history.getPurchasedElectricPowerConsumptionDay(), true, false);
            model.addAttribute("chartEntries", dayViewModel);

        } else if (key.equals(Device.STROMZAEHLER_EINSPEISUNG.historyKeyPrefix())) {
            fillPowerHistoryMonthViewModel(model, Device.STROMZAEHLER_EINSPEISUNG, history.getFeedElectricPowerConsumptionMonth());
            List<ChartEntry> dayViewModel =
                        viewFormatter.fillPowerHistoryDayViewModel(Device.STROMZAEHLER_EINSPEISUNG, history.getFeedElectricPowerConsumptionDay(), true, false);
            model.addAttribute("chartEntries", dayViewModel);

        } else if (key.equals(Device.ELECTRIC_POWER_CONSUMPTION_COUNTER_HOUSE.historyKeyPrefix())) {
            fillPowerHistoryMonthViewModel(model, Device.ELECTRIC_POWER_CONSUMPTION_COUNTER_HOUSE, history.getSelfusedElectricPowerConsumptionMonth());
            List<ChartEntry> dayViewModel =
                    viewFormatter.fillPowerHistoryDayViewModel(Device.ELECTRIC_POWER_CONSUMPTION_COUNTER_HOUSE, history.getSelfusedElectricPowerConsumptionDay(), true, false);
            model.addAttribute("chartEntries", dayViewModel);

        } else if (key.equals(Device.ELECTRIC_POWER_PRODUCTION_COUNTER_HOUSE.historyKeyPrefix())) {
            fillPowerHistoryMonthViewModel(model, Device.ELECTRIC_POWER_PRODUCTION_COUNTER_HOUSE, history.getProducedElectricPowerMonth());
            List<ChartEntry> dayViewModel =
                    viewFormatter.fillPowerHistoryDayViewModel(Device.ELECTRIC_POWER_PRODUCTION_COUNTER_HOUSE, history.getProducedElectricPowerDay(), true, false);
            model.addAttribute("chartEntries", dayViewModel);

        } else if (key.equals(house.getWallboxElectricalPowerConsumption().getDevice().historyKeyPrefix())) {
            fillPowerHistoryMonthViewModel(model, house.getWallboxElectricalPowerConsumption().getDevice(), history.getWallboxElectricPowerConsumptionMonth());
            List<ChartEntry> dayViewModel =
                viewFormatter.fillPowerHistoryDayViewModel(house.getWallboxElectricalPowerConsumption().getDevice(), history.getWallboxElectricPowerConsumptionDay(), true, false);
            model.addAttribute("chartEntries", dayViewModel);

        } else if (key.equals(house.getGasConsumption().getDevice().historyKeyPrefix())) {
            fillPowerHistoryMonthViewModel(model, house.getGasConsumption().getDevice(), history.getGasConsumptionMonth());
            List<ChartEntry> dayViewModel =
                    viewFormatter.fillPowerHistoryDayViewModel(house.getGasConsumption().getDevice(), history.getGasConsumptionDay(), true, false);
            model.addAttribute("chartEntries", dayViewModel);

        } else if (key.equals(house.getConclusionClimateFacadeMin().getDevice().historyKeyPrefix())) {
            fillTemperatureHistoryViewModel(model, history.getOutsideTemperature());

        } else if (key.equals(house.getClimateBedRoom().getDevice().historyKeyPrefix())) {
            fillTemperatureHistoryViewModel(model, history.getBedRoomTemperature());

        } else if (key.equals(house.getClimateKidsRoom1().getDevice().historyKeyPrefix())) {
            fillTemperatureHistoryViewModel(model, history.getKidsRoom1Temperature());

        } else if (key.equals(house.getClimateKidsRoom2().getDevice().historyKeyPrefix())) {
            fillTemperatureHistoryViewModel(model, history.getKidsRoom2Temperature());

        } else if (key.equals(house.getClimateLaundry().getDevice().historyKeyPrefix())) {
            fillTemperatureHistoryViewModel(model, history.getLaundryTemperature());

        } else {
            log.warn("unknown history key: " + key);
        }
    }

    private void fillPowerHistoryMonthViewModel(Model model, Device device, List<PowerConsumptionMonth> pcms) {

        List<HistoryEntry> list = new LinkedList<>();
        int index = 0;
        for (PowerConsumptionMonth pcm : pcms) {
            if (pcm.getPowerConsumption() != null) {
                HistoryEntry entry = new HistoryEntry();
                Long calculated = null;
                entry.setLineOneLabel(MONTH_YEAR_FORMATTER.format(pcm.measurePointMaxDateTime()));
                entry.setLineOneValue(
                        ViewFormatter.powerConsumptionValueForView(device, pcm.getPowerConsumption()) + ViewFormatter.powerConsumptionUnit(device));
                lookupCollapsablePowerMonth(pcms, index, entry);
                boolean calculateDifference = true;
                if (index == pcms.size() - 1) {
                    if (pcm.measurePointMaxDateTime().getDayOfMonth() > 1) {
                        entry.setLineTwoLabel("Hochgerechnet");
                        entry.setBadgeLabel("Vergleich Vorjahr");
                        calculated = calculateProjectedConsumption(entry, device, pcm.measurePointMaxDateTime(), pcm);
                    } else {
                        calculateDifference = false;
                    }
                    entry.setColorClass(" list-group-item-secondary");
                    entry.setLineOneLabel(entry.getLineOneLabel() + " bisher");
                }
                if (calculateDifference) {
                    calculatePreviousYearDifference(entry, pcm, pcms, calculated);
                }
                list.add(entry);
            }
            index++;
        }

        Collections.reverse(list);
        model.addAttribute("historyEntries", list);
    }

    private Long calculateProjectedConsumption(HistoryEntry entry, Device device, LocalDateTime dateTime, PowerConsumptionMonth pcm) {

        YearMonth yearMonthObject = YearMonth.of(dateTime.getYear(), dateTime.getMonthValue());
        int daysInMonth = yearMonthObject.lengthOfMonth();
        int hoursAgo = ((dateTime.getDayOfMonth() - 1) * 24) + dateTime.getHour();
        int hoursToGo = (daysInMonth * 24) - hoursAgo;
        if (hoursAgo > 0) {
            BigDecimal calculated = pcm.getPowerConsumption()
                .add(pcm.getPowerConsumption().divide(new BigDecimal(hoursAgo), 2, RoundingMode.HALF_UP).multiply(new BigDecimal(hoursToGo)));
            entry.setLineTwoValue(
                "≈" + ViewFormatter.powerConsumptionValueForView(device, calculated) + ViewFormatter.powerConsumptionUnit(device));
            return calculated.longValue();
        }
        return null;
    }

    void calculatePreviousYearDifference(HistoryEntry entry, PowerConsumptionMonth pcm,
            List<PowerConsumptionMonth> history, Long calculated) {

        DecimalFormat decimalFormat = buildDecimalFormat("+0;-0");
        LocalDateTime baseDateTime = pcm.measurePointMaxDateTime();
        long baseValue = calculated != null ? calculated : (pcm.getPowerConsumption() != null? pcm.getPowerConsumption().longValue() : 0L);
        Long compareValue = null;

        for (PowerConsumptionMonth historyEntry : history) {
            LocalDateTime otherDateTime = historyEntry.measurePointMaxDateTime();
            if (otherDateTime.getYear() + 1 == baseDateTime.getYear()
                && otherDateTime.getMonthValue() == baseDateTime.getMonthValue()) {
                compareValue = historyEntry.getPowerConsumption() == null ? null : historyEntry.getPowerConsumption().longValue();
                break;
            }
        }

        if (compareValue != null && compareValue != 0L) {
            BigDecimal percentage =
                    new BigDecimal(baseValue).divide(new BigDecimal(compareValue), 4, RoundingMode.HALF_UP).multiply(BD100).subtract(BD100);
            if(percentage.compareTo(BigDecimal.ZERO) == 0){
                entry.setBadgeValue("≈");
            }else{
                entry.setBadgeValue(decimalFormat.format(percentage) + "%");
            }
            if (percentage.intValue() <= COMPARE_PERCENTAGE_GREEN_UNTIL) {
                entry.setBadgeClass("badge-success");
            } else if (percentage.intValue() <= COMPARE_PERCENTAGE_GRAY_UNTIL) {
                entry.setBadgeClass("badge-secondary");
            } else if (percentage.intValue() <= COMPARE_PERCENTAGE_ORANGE_UNTIL) {
                entry.setBadgeClass("badge-warning");
            } else {
                entry.setBadgeClass("badge-danger");
            }
        }
    }

    private void lookupCollapsablePowerMonth(List<PowerConsumptionMonth> pcm, int index, HistoryEntry entry) {
        if (index < pcm.size() - 3) {
            entry.setCollapse(" collapse multi-collapse historyTarget");
        }
    }

    private void fillTemperatureHistoryViewModel(Model model, List<TemperatureHistory> historyList) {

        List<HistoryEntry> list = new LinkedList<>();
        int index = 0;
        for (TemperatureHistory th : historyList) {
            HistoryEntry entry = new HistoryEntry();
            LocalDate date = Instant.ofEpochMilli(th.getDate()).atZone(ZoneId.systemDefault()).toLocalDate();
            if (th.isSingleDay()) {
                entry.setLineOneLabel(StringUtils
                        .capitalize(viewFormatter.formatTimestamp(th.getDate(), ViewFormatter.TimestampFormat.SHORT)));
                entry.setColorClass(" list-group-item-secondary");
            } else {
                entry.setLineOneLabel(MONTH_YEAR_FORMATTER.format(date));
            }
            entry.setLineOneValue(viewFormatter.formatTemperatures(th.getMin(), th.getMax()));
            if (index > 3) {
                entry.setCollapse(" collapse multi-collapse historyTarget");
            }
            list.add(entry);
            index++;
        }
        model.addAttribute("historyEntries", list);
    }

}
