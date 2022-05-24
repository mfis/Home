package de.fimatas.home.library.util;

import de.fimatas.home.library.domain.model.WeatherConditions;
import de.fimatas.home.library.domain.model.WeatherForecastConclusion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WeatherForecastConclusionTextFormatter {

    public static final int FORMAT_FROM_TO_ONLY = 1;
    public static final int FORMAT_CONDITIONS_1_MAX = 2;
    public static final int FORMAT_FROM_TO_PLUS_1_MAX = 3;
    public static final int FORMAT_FROM_TO_ALL_SIGNIFICANT_CONDITIONS = 4;
    public static final int FORMAT_LONGEST = 5;
    public static final int SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS = 6;
    public static final int SIGNIFICANT_CONDITION_NATIVE_ICON = 7;
    public static final int SIGNIFICANT_CONDITION_WEB_ICON = 8;

    public static Map<Integer, String> formatConclusionText(WeatherForecastConclusion conclusion){

        final var texts = new LinkedHashMap<Integer, String>();

        final var conditionsSortedBySignificance = conclusion.getConditions().stream()
                .filter(WeatherConditions::isSignificant).sorted(Comparator.comparingInt(WeatherConditions::ordinal)).collect(Collectors.toList());
        final var conditionsSortedByTime = conclusion.getConditions().stream()
                .filter(WeatherConditions::isSignificant).collect(Collectors.toList());
        final var usignificanceConditionWithHighestOrdinal = conclusion.getConditions().stream()
                .filter(c -> !c.isSignificant()).min(Comparator.comparingInt(WeatherConditions::ordinal));

        final var fromToString = formatTemperature(conclusion.getMinTemp())
                + ".." + formatTemperature(conclusion.getMaxTemp()) + "°C";

        final var fromUntilToString = formatTemperature(conclusion.getMinTemp())
                + " bis " + formatTemperature(conclusion.getMaxTemp()) + "°C";

        StringBuilder fromUntilToWithCaptionAndTime = new StringBuilder("Temperatur " + fromUntilToString);

        if(conditionsSortedByTime.isEmpty() && usignificanceConditionWithHighestOrdinal.isPresent()){
            conditionsSortedByTime.add(usignificanceConditionWithHighestOrdinal.get());
        }

        for(WeatherConditions c : conditionsSortedByTime){
            fromUntilToWithCaptionAndTime.append(", ").append(text(c, conclusion, true));
            if(conclusion.getFirstOccurences().containsKey(c)){
                fromUntilToWithCaptionAndTime.append(" ab ").append(conclusion.getFirstOccurences().get(c).getHour()).append(" Uhr");
            }
        }

        final var condOneMax = conditionsSortedBySignificance.isEmpty()? "" : text(conditionsSortedBySignificance.get(0), conclusion, false) + plusIfMoreThenOne(conditionsSortedBySignificance);
        final var allSignificantConditions = conditionsSortedByTime.stream().map(c -> text(c, conclusion, false)).collect(Collectors.joining(", "));

        texts.put(FORMAT_FROM_TO_ONLY, fromToString);
        texts.put(FORMAT_CONDITIONS_1_MAX, condOneMax);
        texts.put(FORMAT_FROM_TO_PLUS_1_MAX, fromToString + (condOneMax.length() > 0 ? ", " + condOneMax : ""));
        texts.put(FORMAT_FROM_TO_ALL_SIGNIFICANT_CONDITIONS, fromUntilToString + (allSignificantConditions.length() > 0 ? ", " + allSignificantConditions : ""));
        texts.put(FORMAT_LONGEST, fromUntilToWithCaptionAndTime.toString());
        texts.put(SIGNIFICANT_CONDITION_COLOR_CODE_UI_CLASS, conditionsSortedBySignificance.isEmpty() || conditionsSortedBySignificance.get(0).getColor() == null ? "" : conditionsSortedBySignificance.get(0).getColor().getUiClass());
        texts.put(SIGNIFICANT_CONDITION_NATIVE_ICON, conditionsSortedBySignificance.isEmpty() ? (usignificanceConditionWithHighestOrdinal.isPresent()?usignificanceConditionWithHighestOrdinal.get().getSfSymbolsID():"") : conditionsSortedBySignificance.get(0).getSfSymbolsID());
        texts.put(SIGNIFICANT_CONDITION_WEB_ICON, conditionsSortedBySignificance.isEmpty() ? (usignificanceConditionWithHighestOrdinal.isPresent()?usignificanceConditionWithHighestOrdinal.get().getFontAwesomeID():"") : conditionsSortedBySignificance.get(0).getFontAwesomeID());

        return texts;
    }

    private static String text(WeatherConditions cond, WeatherForecastConclusion conclusion, boolean longText){
        if(conclusion.getConditions().contains(WeatherConditions.WIND)){
            return "Wind " + (longText ? "bis " : "") + conclusion.getMaxWind() + " km/h";
        }else{
            return cond.getCaption();
        }
    }

    private static String plusIfMoreThenOne(List<WeatherConditions> cond){
        return cond.size() > 1 ? " +" : "";
    }

    private static int formatTemperature(BigDecimal temperature){
        return temperature.setScale(0, RoundingMode.HALF_UP).intValue();
    }

}
