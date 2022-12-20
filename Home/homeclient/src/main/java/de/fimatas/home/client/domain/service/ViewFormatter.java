package de.fimatas.home.client.domain.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;
import de.fimatas.home.client.domain.model.ChartEntry;
import de.fimatas.home.client.domain.model.ValueWithCaption;
import de.fimatas.home.library.domain.model.PowerConsumptionDay;
import de.fimatas.home.library.domain.model.TimeRange;
import de.fimatas.home.library.util.HomeUtils;

@Component
public class ViewFormatter {

    public static final String SUM_SIGN = "\u2211 ";

    public static final String DEGREE = "\u00b0";

    public static final String K_W_H = " kW/h";

    public static final long KWH_FACTOR = 1000L;

    private static final BigDecimal KWH_FACTOR_BD = new BigDecimal(ViewFormatter.KWH_FACTOR);

    private static final BigDecimal SPACER_VALUE = new BigDecimal("0.5");

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final BigDecimal ONE_POINT_NINE = new BigDecimal("1.9");

    private static final BigDecimal PLACEHOLDER_TIMERANGE_KWH = new BigDecimal("3");

    public static final DateTimeFormatter WEEKDAY_FORMATTER = DateTimeFormatter.ofPattern("EEEE", Locale.GERMAN);

    public static final DateTimeFormatter DAY_MONTH_YEAR_FORMATTER =
        DateTimeFormatter.ofPattern("E, dd.MM.yyyy", Locale.GERMAN);

    public static final DateTimeFormatter DAY_MONTH_YEAR_SHORT_FORMATTER =
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);

    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN);

    public enum TimestampFormat {
        DATE, DATE_TIME, SHORT, SHORT_WITH_TIME
    }


    public String formatTimestamp(long date, TimestampFormat format) {

        LocalDateTime ldt;
        if (date == NumberUtils.INTEGER_ZERO) {
            ldt = null;
        }else{
            ldt = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return formatTimestamp(ldt, format);
    }

    public String formatTimestamp(LocalDateTime date, TimestampFormat format) { // TODO: write unit tests

        if (date == null) {
            return "unbekannt";
        }

        LocalDateTime localDate2 = LocalDateTime.now();

        long between =
            ChronoUnit.DAYS.between(date.truncatedTo(ChronoUnit.DAYS), localDate2.truncatedTo(ChronoUnit.DAYS));

        String dayString;

        if(between == -1) {
            dayString = "morgen";
        } else if (between == 0 && format == TimestampFormat.SHORT_WITH_TIME) {
            dayString = TIME_FORMATTER.format(date) + " Uhr";
        } else if (between == 0) {
            dayString = "heute";
        } else if (between == 1) {
            dayString = "gestern";
        } else if (between == 2) {
            dayString = "vorgestern";
        } else if (between > -1 && between < 7) {
            dayString = WEEKDAY_FORMATTER.format(date);
        } else {
            if (format == TimestampFormat.SHORT || format == TimestampFormat.SHORT_WITH_TIME) {
                dayString = DAY_MONTH_YEAR_SHORT_FORMATTER.format(date);
            } else {
                dayString = DAY_MONTH_YEAR_FORMATTER.format(date);
            }
        }

        if (format == TimestampFormat.DATE_TIME) {
            dayString += ", " + TIME_FORMATTER.format(date) + " Uhr";
        }

        return dayString;
    }

    public String formatTemperatures(BigDecimal min, BigDecimal max) {

        if (min == null || max == null) {
            return "n/a";
        }

        String minFrmt = formatTemperature(min);
        String maxFrmt = formatTemperature(max);

        if (StringUtils.equals(minFrmt, maxFrmt)) {
            return minFrmt + DEGREE + "C";
        }

        return minFrmt + DEGREE + "C bis " + maxFrmt + DEGREE + "C";
    }

    public String formatTemperature(BigDecimal value) {

        DecimalFormat decimalFormat = new DecimalFormat("0");
        String frmt = decimalFormat.format(value);
        if ("-0".equals(frmt)) { // special case: some negative value roundet to
                                 // zero has a leading '-'
            frmt = "0";
        }
        return frmt;
    }

    public List<ChartEntry> fillPowerHistoryDayViewModel(List<PowerConsumptionDay> days, boolean historyView, boolean onlyToday) {

        DecimalFormat decimalFormat = new DecimalFormat("0.#");
        LocalDateTime today = LocalDateTime.now();
        TimeRange actualRange = TimeRange.fromDateTime(today);
        List<ChartEntry> chartEntries = new LinkedList<>();

        BigDecimal maxSum = calculateMaxSum(days, today, actualRange);
        if (maxSum.compareTo(BigDecimal.ZERO) == 0) {
            return chartEntries;
        }

        BigDecimal maxKwh = maxSum.divide(KWH_FACTOR_BD, new MathContext(3, RoundingMode.HALF_UP));
        int index = 0;

        for (PowerConsumptionDay pcd : days) {
            boolean isToday = HomeUtils.isSameDay(pcd.measurePointMaxDateTime(), today);
            BigDecimal percentageBase = HUNDRED.subtract(SPACER_VALUE.multiply(new BigDecimal(pcd.getValues().size())));
            BigDecimal chartValuePerPowerValue = percentageBase.divide(maxKwh, new MathContext(3, RoundingMode.HALF_UP));
            ChartEntry chartEntry = new ChartEntry();
            lookupCollapsablePowerDay(days, index, chartEntry);
            BigDecimal daySum = BigDecimal.ZERO;
            daySum = handleAllTimeRangesForOneDay(decimalFormat, actualRange, pcd, isToday, chartValuePerPowerValue, chartEntry,
                daySum);
            String sumCaption = sumCaption(historyView, decimalFormat, isToday, daySum);
            chartEntryLabels(historyView, pcd, chartEntry, sumCaption);
            chartEntry.setNumericValue(daySum);
            chartEntries.add(chartEntry);
            index++;
        }

        Collections.reverse(chartEntries);
        return chartEntries;
    }

    private void chartEntryLabels(boolean historyView, PowerConsumptionDay pcd, ChartEntry chartEntry, String sumCaption) {
        if (historyView) {
            chartEntry
                .setLabel(StringUtils.capitalize(formatTimestamp(pcd.getMeasurePointMax(), TimestampFormat.DATE)));
            chartEntry.setAdditionalLabel(sumCaption);
        } else {
            chartEntry.setLabel(chartEntry.getLabel() + " " + sumCaption + ViewFormatter.K_W_H);
        }
    }

    private BigDecimal calculateMaxSum(List<PowerConsumptionDay> days, LocalDateTime today, TimeRange actualRange) {

        BigDecimal maxSum = BigDecimal.ZERO;
        for (PowerConsumptionDay pcd : days) {
            boolean isToday = HomeUtils.isSameDay(pcd.measurePointMaxDateTime(), today);
            BigDecimal sum = BigDecimal.ZERO;
            for (Map.Entry<TimeRange, BigDecimal> entry : pcd.getValues().entrySet()) {
                if (isToday && entry.getValue().equals(BigDecimal.ZERO) && entry.getKey().ordinal() > actualRange.ordinal()) {
                    sum = sum.add(PLACEHOLDER_TIMERANGE_KWH);
                } else {
                    sum = sum.add(entry.getValue());
                }
            }
            if (sum.compareTo(maxSum) > 0) {
                maxSum = sum;
            }
        }
        return maxSum;
    }

    private BigDecimal handleAllTimeRangesForOneDay(DecimalFormat decimalFormat, TimeRange actualRange, PowerConsumptionDay pcd,
            boolean isToday, BigDecimal chartValuePerPowerValue, ChartEntry chartEntry, BigDecimal daySum) {

        for (Map.Entry<TimeRange, BigDecimal> entry : pcd.getValues().entrySet()) {
            boolean comingTimeRange =
                entry.getValue().equals(BigDecimal.ZERO) && entry.getKey().ordinal() > actualRange.ordinal();
            ValueWithCaption vwc = new ValueWithCaption();
            BigDecimal kwh;
            if (isToday && comingTimeRange) {
                kwh = handleComingTimeRange(vwc);
            } else {
                kwh = handlePreviousOrActualTimeRange(decimalFormat, actualRange, isToday, entry, vwc);
                daySum = daySum.add(kwh);
            }
            vwc.setValue(chartValuePerPowerValue.multiply(kwh).toString());
            chartEntry.getValuesWithCaptions().add(vwc);
            addChartSpacer(chartEntry);
        }
        return daySum;
    }

    private String sumCaption(boolean historyView, DecimalFormat decimalFormat, boolean isToday, BigDecimal daySum) {

        String sumCaption = SUM_SIGN + decimalFormat.format(daySum);
        if (isToday && historyView) {
            sumCaption += " + ?";
        }
        return sumCaption;
    }

    private void lookupCollapsablePowerDay(List<PowerConsumptionDay> days, int index, ChartEntry entry) {
        if (index < days.size() - 3) {
            entry.setCollapse(" collapse multi-collapse chartTarget");
        }
    }

    private void addChartSpacer(ChartEntry chartEntry) {

        ValueWithCaption spacer = new ValueWithCaption();
        spacer.setCssClass(" bg-dark");
        spacer.setValue(SPACER_VALUE.toString());
        chartEntry.getValuesWithCaptions().add(spacer);
    }

    private BigDecimal handlePreviousOrActualTimeRange(DecimalFormat decimalFormat, TimeRange actualRange, boolean isToday,
            Map.Entry<TimeRange, BigDecimal> entry, ValueWithCaption vwc) {

        BigDecimal kwh;
        kwh = entry.getValue().divide(KWH_FACTOR_BD, new MathContext(3, RoundingMode.HALF_UP));
        cssClassActiveTimerange(actualRange, isToday, entry, vwc);
        vwc.setCaption(chartValueCaption(decimalFormat, kwh));
        return kwh;
    }

    private BigDecimal handleComingTimeRange(ValueWithCaption vwc) {

        BigDecimal kwh;
        kwh = PLACEHOLDER_TIMERANGE_KWH;
        vwc.setCssClass(" bg-secondary");
        vwc.setCaption("?");
        return kwh;
    }

    private void cssClassActiveTimerange(TimeRange actualRange, boolean isToday, Map.Entry<TimeRange, BigDecimal> entry,
            ValueWithCaption vwc) {
        if (isToday && entry.getKey().ordinal() == actualRange.ordinal()) {
            vwc.setCssClass(" bg-primary progress-bar-striped progress-bar-animated");
        } else {
            vwc.setCssClass(" bg-primary");
        }
    }

    private String chartValueCaption(DecimalFormat decimalFormat, BigDecimal kwh) {

        if (kwh.compareTo(BigDecimal.ONE) < 0) {
            return StringUtils.EMPTY;
        } else if (kwh.compareTo(BigDecimal.ONE) > 0 && kwh.compareTo(ONE_POINT_NINE) < 0) {
            return "1..";
        } else {
            return decimalFormat.format(kwh);
        }
    }

}
