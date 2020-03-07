package home.domain.service;

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import home.domain.model.ChartEntry;
import home.domain.model.HistoryEntry;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.PowerConsumptionMonth;
import homecontroller.domain.model.TemperatureHistory;

@Component
public class HistoryViewService {

	private static final BigDecimal BD100 = new BigDecimal(100);
	private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");
	private static final int COMPARE_PERCENTAGE_GREEN_UNTIL = -1;
	private static final int COMPARE_PERCENTAGE_GRAY_UNTIL = +2;
	private static final int COMPARE_PERCENTAGE_ORANGE_UNTIL = +15;

	@Autowired
	private ViewFormatter viewFormatter;

	public void fillHistoryViewModel(Model model, HistoryModel history, HouseModel house, String key) {

		if (key.equals(house.getElectricalPowerConsumption().getDevice().programNamePrefix())) {
			fillPowerHistoryMonthViewModel(model, history);
			List<ChartEntry> dayViewModel = viewFormatter
					.fillPowerHistoryDayViewModel(history.getElectricPowerConsumptionDay(), true);
			model.addAttribute("chartEntries", dayViewModel);
		} else if (key.equals(house.getConclusionClimateFacadeMin().getDevice().programNamePrefix())) {
			fillTemperatureHistoryViewModel(model, history.getOutsideTemperature());
		} else if (key.equals(house.getClimateBedRoom().getDevice().programNamePrefix())) {
			fillTemperatureHistoryViewModel(model, history.getBedRoomTemperature());
		} else if (key.equals(house.getClimateKidsRoom().getDevice().programNamePrefix())) {
			fillTemperatureHistoryViewModel(model, history.getKidsRoomTemperature());
		} else if (key.equals(house.getClimateLaundry().getDevice().programNamePrefix())) {
			fillTemperatureHistoryViewModel(model, history.getLaundryTemperature());
		}
	}

	private void fillPowerHistoryMonthViewModel(Model model, HistoryModel history) {

		List<HistoryEntry> list = new LinkedList<>();
		DecimalFormat decimalFormat = new DecimalFormat("0");
		int index = 0;
		for (PowerConsumptionMonth pcm : history.getElectricPowerConsumptionMonth()) {
			if (pcm.getPowerConsumption() != null) {
				HistoryEntry entry = new HistoryEntry();
				Long calculated = null;
				entry.setLineOneLabel(MONTH_YEAR_FORMATTER.format(pcm.measurePointMaxDateTime()));
				entry.setLineOneValue(
						decimalFormat.format(pcm.getPowerConsumption() / ViewFormatter.KWH_FACTOR)
								+ ViewFormatter.K_W_H);
				lookupCollapsablePowerMonth(history, index, entry);
				boolean calculateDifference = true;
				if (index == history.getElectricPowerConsumptionMonth().size() - 1) {
					if (pcm.measurePointMaxDateTime().getDayOfMonth() > 1) {
						entry.setLineTwoLabel("Hochgerechnet");
						entry.setBadgeLabel("Vergleich Vorjahr");
						calculated = calculateProjectedConsumption(entry, pcm.measurePointMaxDateTime(), pcm);
					} else {
						calculateDifference = false;
					}
					entry.setColorClass(" list-group-item-secondary");
					entry.setLineOneLabel(entry.getLineOneLabel() + " bisher");
				}
				if (calculateDifference) {
					calculatePreviousYearDifference(entry, pcm, history.getElectricPowerConsumptionMonth(),
							pcm.getPowerConsumption(), calculated);
				}
				list.add(entry);
			}
			index++;
		}

		Collections.reverse(list);
		model.addAttribute("historyEntries", list);
	}

	private Long calculateProjectedConsumption(HistoryEntry entry, LocalDateTime dateTime,
			PowerConsumptionMonth pcm) {

		YearMonth yearMonthObject = YearMonth.of(dateTime.getYear(), dateTime.getMonthValue());
		int daysInMonth = yearMonthObject.lengthOfMonth();
		int hoursAgo = ((dateTime.getDayOfMonth() - 1) * 24) + dateTime.getHour();
		int hoursToGo = (daysInMonth * 24) - hoursAgo;
		if (hoursAgo > 0) {
			BigDecimal actualValue = new BigDecimal(pcm.getPowerConsumption());
			BigDecimal calculated = actualValue
					.add(actualValue.divide(new BigDecimal(hoursAgo), 2, RoundingMode.HALF_UP)
							.multiply(new BigDecimal(hoursToGo)));
			entry.setLineTwoValue(
					"≈" + new DecimalFormat("0").format(calculated.longValue() / ViewFormatter.KWH_FACTOR)
							+ ViewFormatter.K_W_H);
			return calculated.longValue();
		}
		return null;
	}

	private void calculatePreviousYearDifference(HistoryEntry entry, PowerConsumptionMonth pcm,
			List<PowerConsumptionMonth> history, Long actual, Long calculated) {

		DecimalFormat decimalFormat = new DecimalFormat("+0;-0");
		LocalDateTime baseDateTime = pcm.measurePointMaxDateTime();
		Long baseValue = calculated != null ? calculated : actual;
		Long compareValue = null;

		for (PowerConsumptionMonth historyEntry : history) {
			LocalDateTime otherDateTime = historyEntry.measurePointMaxDateTime();
			if (otherDateTime.getYear() + 1 == baseDateTime.getYear()
					&& otherDateTime.getMonthValue() == baseDateTime.getMonthValue()) {
				compareValue = historyEntry.getPowerConsumption();
				break;
			}
		}

		if (baseValue != null && compareValue != null) {
			long diff = baseValue - compareValue;
			BigDecimal percentage = new BigDecimal(diff)
					.divide(new BigDecimal(baseValue), 4, RoundingMode.HALF_UP).multiply(BD100);
			entry.setBadgeValue(decimalFormat.format(percentage) + "%");
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

	private void lookupCollapsablePowerMonth(HistoryModel history, int index, HistoryEntry entry) {
		if (index < history.getElectricPowerConsumptionMonth().size() - 3) {
			entry.setCollapse(" collapse multi-collapse historyTarget");
		}
	}

	private void fillTemperatureHistoryViewModel(Model model, List<TemperatureHistory> historyList) {

		List<HistoryEntry> list = new LinkedList<>();
		int index = 0;
		for (TemperatureHistory th : historyList) {
			HistoryEntry entry = new HistoryEntry();
			entry.setLineOneValueIcon("fas fa-moon");
			entry.setLineTwoValueIcon("fas fa-sun");
			LocalDate date = Instant.ofEpochMilli(th.getDate()).atZone(ZoneId.systemDefault()).toLocalDate();
			if (th.isSingleDay()) {
				if (date.compareTo(LocalDate.now()) == 0) {
					entry.setLineOneLabel("Heute");
				} else if (date.compareTo(LocalDate.now().minusDays(1)) == 0) {
					entry.setLineOneLabel("Gestern");
				} else {
					entry.setLineOneLabel(ViewFormatter.DAY_MONTH_YEAR_FORMATTER.format(date));
				}
				entry.setColorClass(" list-group-item-secondary");
			} else {
				entry.setLineOneLabel(MONTH_YEAR_FORMATTER.format(date));
			}
			entry.setLineOneValue(viewFormatter.formatTemperatures(th.getNightMin(), th.getNightMax()));
			entry.setLineTwoValue(viewFormatter.formatTemperatures(th.getDayMin(), th.getDayMax()));
			if (index > 2) {
				entry.setCollapse(" collapse multi-collapse historyTarget");
			}
			list.add(entry);
			index++;
		}
		model.addAttribute("historyEntries", list);
	}

}