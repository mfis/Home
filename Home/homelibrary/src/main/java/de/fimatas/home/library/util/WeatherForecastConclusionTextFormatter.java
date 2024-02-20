package de.fimatas.home.library.util;

import de.fimatas.home.library.domain.model.WeatherConditions;
import de.fimatas.home.library.domain.model.WeatherForecastConclusion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static de.fimatas.home.library.util.HomeUtils.buildDecimalFormat;

public class WeatherForecastConclusionTextFormatter {

    public static final int FORMAT_CONDITIONS_SHORT_1_MAX_INCL_UNSIGNIFICANT = 0;
    public static final int FORMAT_FROM_TO_ONLY = 1;
    public static final int FORMAT_CONDITIONS_SHORT_1_MAX = 2;
    public static final int FORMAT_FROM_TO_PLUS_1_MAX = 3;
    public static final int FORMAT_FROM_TO_ALL_SIGNIFICANT_CONDITIONS = 4;
    public static final int FORMAT_LONGEST = 5;
    public static final int SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS = 6;
    public static final int SIGNIFICANT_CONDITION_NATIVE_ICON = 7;
    public static final int SIGNIFICANT_CONDITION_WEB_ICON = 8;
    public static final int WIND_GUST_TEXT = 9;
    public static final int PRECIPATION_TEXT = 10;
    public static final int SUNDURATION_TEXT = 11;

    private static final BigDecimal BD_60 = new BigDecimal("60");

    public static Map<Integer, String> formatConclusionText(WeatherForecastConclusion conclusion){
        return formatInternal(conclusion, false);
    }

    public static String formatConditionColor(WeatherForecastConclusion conclusion){
        return formatInternal(conclusion, true).get(SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS);
    }

    private static LinkedHashMap<Integer, String> formatInternal(WeatherForecastConclusion conclusion, boolean conditionColorOnly) {

        var conditions = conclusion.getConditions().stream()
                .sorted(Comparator.comparing(WeatherConditions::ordinal))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        final var texts = new LinkedHashMap<Integer, String>();

        final var conditionsSortedBySignificance = conditions.stream()
                .filter(WeatherConditions::isSignificant).sorted(Comparator.comparingInt(WeatherConditions::ordinal)).collect(Collectors.toCollection(LinkedHashSet::new));

        texts.put(SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS, conditionsSortedBySignificance.isEmpty() || firstElementOf(conditionsSortedBySignificance).getColor() == null ? "" : firstElementOf(conditionsSortedBySignificance).getColor().getUiClass());

        if(conditionColorOnly){
            return texts;
        }

        final var usignificanceConditionWithHighestOrdinal = conditions.stream()
                .filter(c -> !c.isSignificant()).min(Comparator.comparingInt(WeatherConditions::ordinal));

        final var formattedTempMin = formatTemperature(conclusion.getMinTemp());
        final var formattedTempMax = formatTemperature(conclusion.getMaxTemp());
        final var isMinMaxSame = formattedTempMin == formattedTempMax;
        final var fromToString = formattedTempMin + (isMinMaxSame ? "" : ".." + formattedTempMax) + "°C";
        final var fromUntilToString = formattedTempMin + (isMinMaxSame ? "" : " bis " + formattedTempMax) + "°C";

        final var conditionsForFormatLongest = conditions.stream()
                .filter(WeatherConditions::isSignificant).collect(Collectors.toCollection(LinkedHashSet::new));
        if(conditionsForFormatLongest.isEmpty() && usignificanceConditionWithHighestOrdinal.isPresent()){
            conditionsForFormatLongest.add(usignificanceConditionWithHighestOrdinal.get());
        }

        final var condOneMax = conditionsSortedBySignificance.isEmpty()? "" : text(firstElementOf(conditionsSortedBySignificance), conclusion, false) + plusIfMoreThenOne(conditionsSortedBySignificance);
        final var condOneMaxShort = conditionsSortedBySignificance.isEmpty()? "" : text(firstElementOf(conditionsSortedBySignificance), conclusion, true) + plusIfMoreThenOne(conditionsSortedBySignificance);
        final var condOneMaxInclUnsignificantShort = conditionsSortedBySignificance.isEmpty()? (usignificanceConditionWithHighestOrdinal.isPresent()?text(usignificanceConditionWithHighestOrdinal.get(), conclusion, true) : "") : text(firstElementOf(conditionsSortedBySignificance), conclusion, true) + plusIfMoreThenOne(conditionsSortedBySignificance);
        final var allSignificantConditions = conditionsForFormatLongest.stream().map(c -> text(c, conclusion, false)).collect(Collectors.joining(", "));

        StringBuilder fromUntilToWithCaptionAndTime = new StringBuilder("Temperatur " + fromUntilToString);
        conclusion.getFirstOccurences().entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(e -> {
            if(conditionsForFormatLongest.contains(e.getKey())){
                fromUntilToWithCaptionAndTime.append(", ").append(text(e.getKey(), conclusion, false));
                fromUntilToWithCaptionAndTime.append(" ab ").append(conclusion.getFirstOccurences().get(e.getKey()).getHour()).append(" Uhr");
                conditionsForFormatLongest.remove(e.getKey());
            }
        });
        conditionsForFormatLongest.forEach(c -> fromUntilToWithCaptionAndTime.append(", ").append(text(c, conclusion, false)));

        texts.put(FORMAT_FROM_TO_ONLY, fromToString);
        texts.put(FORMAT_CONDITIONS_SHORT_1_MAX, condOneMaxShort);
        texts.put(FORMAT_CONDITIONS_SHORT_1_MAX_INCL_UNSIGNIFICANT, condOneMaxInclUnsignificantShort);
        texts.put(FORMAT_FROM_TO_PLUS_1_MAX, fromToString + (!condOneMax.isEmpty() ? ", " + condOneMax : ""));
        texts.put(FORMAT_FROM_TO_ALL_SIGNIFICANT_CONDITIONS, fromUntilToString + (!allSignificantConditions.isEmpty() ? ", " + allSignificantConditions : ""));
        texts.put(FORMAT_LONGEST, fromUntilToWithCaptionAndTime.toString());
        texts.put(SIGNIFICANT_CONDITION_NATIVE_ICON, conditionsSortedBySignificance.isEmpty() ? (usignificanceConditionWithHighestOrdinal.isPresent()?usignificanceConditionWithHighestOrdinal.get().getSfSymbolsID():"") : firstElementOf(conditionsSortedBySignificance).getSfSymbolsID());
        texts.put(SIGNIFICANT_CONDITION_WEB_ICON, conditionsSortedBySignificance.isEmpty() ? (usignificanceConditionWithHighestOrdinal.isPresent()?usignificanceConditionWithHighestOrdinal.get().getFontAwesomeID():"") : firstElementOf(conditionsSortedBySignificance).getFontAwesomeID());
        texts.put(WIND_GUST_TEXT, windGustText(null, conclusion));
        texts.put(PRECIPATION_TEXT, precipationText(conclusion));
        texts.put(SUNDURATION_TEXT, sunDurationText(conclusion));
        return texts;
    }

    private static WeatherConditions firstElementOf(LinkedHashSet<WeatherConditions> set){
        return set.iterator().next();
    }

    private static String text(WeatherConditions cond, WeatherForecastConclusion conclusion, boolean shortText){
        if(!shortText && (cond == WeatherConditions.WIND || cond == WeatherConditions.GUST)){
            return windGustText(cond, conclusion);
        }else{
            return cond.getCaption();
        }
    }

    private static String windGustText(WeatherConditions cond, WeatherForecastConclusion conclusion) {
        String conditionPrefix = cond == null ? "" : cond.getCaption();
        if(conclusion.getMaxGust() == null || (conclusion.getMaxWind().intValue() == conclusion.getMaxGust().intValue())){
            if (conclusion.getMaxWind() != null){
                return (conditionPrefix + " bis " + conclusion.getMaxWind() + " km/h").trim();
            }else{
                return "";
            }
        }else{
            return (conditionPrefix + " " + conclusion.getMaxWind() + ".." + conclusion.getMaxGust() + " km/h").trim();
        }
    }

    private static String sunDurationText(WeatherForecastConclusion conclusion) {
        if(conclusion.getSunshineInMin()==null){
            return "";
        }else if(conclusion.getSunshineInMin().intValue() > BD_60.intValue()){
            return buildDecimalFormat("0").format(conclusion.getSunshineInMin().divide(BD_60, 1, RoundingMode.HALF_UP))  + " Std";
        }else{
            return conclusion.getSunshineInMin().intValue() + " Min";
        }
    }

    private static String precipationText(WeatherForecastConclusion conclusion) {
        if(conclusion.getPrecipitationInMM()==null){
            return "";
        }
        String pattern = conclusion.getPrecipitationInMM().compareTo(BigDecimal.TEN) < 0 && conclusion.getPrecipitationInMM().compareTo(BigDecimal.ZERO) > 0 ? "0.0" : "0";
        return buildDecimalFormat(pattern).format(conclusion.getPrecipitationInMM())  + " mm";
    }

    private static String plusIfMoreThenOne(Set<WeatherConditions> cond){
        return cond.size() > 1 ? " +" : "";
    }

    private static int formatTemperature(BigDecimal temperature){
        return temperature.setScale(0, RoundingMode.HALF_UP).intValue();
    }

}
