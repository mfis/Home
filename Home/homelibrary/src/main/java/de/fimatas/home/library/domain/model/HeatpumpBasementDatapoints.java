package de.fimatas.home.library.domain.model;

import de.fimatas.home.library.model.ConditionColor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

@Getter
public enum HeatpumpBasementDatapoints {

    PUMPE(
            "Heizkreis Pumpe",
            false,
            1,
            HeatpumpBasementDatapoints::formattedValueLong,
            HeatpumpBasementDatapoints::formattedValueShort,
            null,
            val -> isValueOff(val) ? ConditionColor.DEFAULT : ConditionColor.GREEN
    ),

    VORLAUF_TEMPERATUR(
            "Vorlauf Temperatur",
            false,
            1,
            val -> val,
            HeatpumpBasementDatapoints::formattedValueShort,
            new BigDecimal("0.1"),
            val -> valueAsBigDecimal(val).intValue() < getOrangeTemperatureValue() ? ConditionColor.DEFAULT : ConditionColor.ORANGE
    ),

    SPEICHER_TEMPERATUR(
            "Speicher Temperatur",
            false,
            1,
            val -> val,
            HeatpumpBasementDatapoints::formattedValueShort,
            new BigDecimal("0.1"),
            val -> valueAsBigDecimal(val).intValue() < getOrangeTemperatureValue() ? ConditionColor.DEFAULT : ConditionColor.ORANGE
    ),

    VERDICHTER_STATUS(
            "Außengerät Status",
            false,
            2,
            HeatpumpBasementDatapoints::formattedValueLong,
            HeatpumpBasementDatapoints::formattedValueShort,
            null,
            val -> isValueOff(val) ? ConditionColor.DEFAULT : ConditionColor.ORANGE
    ),

    LEISTUNG_SOLL(
            "Außengerät Leistung",
            false,
            2,
            val -> val,
            HeatpumpBasementDatapoints::formattedValueShort,
            new BigDecimal("1"),
            val -> valueAsBigDecimal(val).compareTo(BigDecimal.ZERO) == 0 ? ConditionColor.DEFAULT : ConditionColor.ORANGE
    ),

    LEISTUNGSAUFNAHME(
            "Stromaufnahme",
            false,
            3,
            val -> val,
            HeatpumpBasementDatapoints::formattedValueShort,
            new BigDecimal("20"),
            val -> valueAsBigDecimal(val).compareTo(BigDecimal.ZERO) == 0 ? ConditionColor.DEFAULT : ConditionColor.ORANGE
    ),

    WAERMELEISTUNG(
            "Wärmeleistung",
            false,
            3,
            val -> val,
            HeatpumpBasementDatapoints::formattedValueShort,
            new BigDecimal("20"),
            val -> valueAsBigDecimal(val).intValue() == 0 ? ConditionColor.DEFAULT : ConditionColor.ORANGE
    ),

    ELEKTRO_HEIZUNG(
            "E-Heizung Status",
            false,
            3,
            HeatpumpBasementDatapoints::formattedValueLong,
            HeatpumpBasementDatapoints::formattedValueShort,
            null,
            val -> isValueOff(val) ? ConditionColor.DEFAULT : ConditionColor.RED
    ),

    VERBRAUCH(
            "--",
            true,
            3,
            val -> val,
            HeatpumpBasementDatapoints::formattedValueShort,
            new BigDecimal("0.2"),
            val -> ConditionColor.DEFAULT
    ),

    WAERMEPRODUKTION(
            "--",
            true,
            3,
            val -> val,
            HeatpumpBasementDatapoints::formattedValueShort,
            new BigDecimal("0.2"),
            val -> ConditionColor.DEFAULT
    ),

    PROGRAMM(
            "Programm",
            false,
            4,
            HeatpumpBasementDatapoints::formattedValueLong,
            HeatpumpBasementDatapoints::formattedValueShort,
            null,
            val -> isValueOff(val) ? ConditionColor.DEFAULT : ConditionColor.GREEN
    ),

    ELEKTRO_STD(
            "E-Heizung Laufzeit",
            false,
            4,
            val -> val,
            HeatpumpBasementDatapoints::formattedValueShort,
            null,
            val -> ConditionColor.DEFAULT
    ),

    DRUCK(
            "Anlagendruck",
            false,
            4,
            val -> val,
            HeatpumpBasementDatapoints::formattedValueShort,
            new BigDecimal("0.02"),
            val -> valueAsBigDecimal(val).compareTo(new BigDecimal("1.30")) < 1 ? ConditionColor.RED : ConditionColor.DEFAULT
    ),

    ;

    private final String alternateLabel;
    private final boolean hidden;
    private final int group;
    private final Function<String, String> formattedValueLong;
    private final Function<String, String> formattedValueShort;
    private final BigDecimal tendencyThreshold;
    private final Function<String, ConditionColor> colorBasedByValue;

    private static final String AUS = "Aus";
    public final static String VAL_STANDBY = "Standby";
    private final static String VAL_OFF = AUS;
    private final static String VAL_AUTO_OFF = "Automatik aus";
    private final static String VAL_INACTIVE = "Deaktiviert";
    private final static int VAL_TEMPERATURE_ORANGE = 35;

    public final static String CONST_VAL_DEFROST = "Abtauen";
    public final static String CONST_VAL_LOWERING = "Absenken";

    private static final String ABTAUBETRIEB = "Abtaubetrieb";
    private static final String ABSENKBETRIEB = "Absenkbetrieb";
    private static final String AUTOMATIK = "Automatik";
    private static final String AUSGESCHALTET = "Ausgeschaltet";
    private static final String EINGESCHALTET = "Eingeschaltet";
    private static final String AUTO = "Auto";
    private static final String EIN = "Ein";
    private static final String SPERRZEIT = "Sperrzeit";
    private static final String BETRIEB = "Betrieb";

    private final static Map<String, String> VAL_ALIAS_MAP_LONG = Map.of(
            AUTO, AUTOMATIK,
            AUTOMATIK + " ein", AUTOMATIK,
            VAL_AUTO_OFF, AUSGESCHALTET,
            VAL_OFF, AUSGESCHALTET,
            VAL_INACTIVE, AUSGESCHALTET,
            SPERRZEIT, AUSGESCHALTET,
            EIN, EINGESCHALTET,
            BETRIEB, EINGESCHALTET
    );

    private final static Map<String, String> VAL_ALIAS_MAP_SHORT= Map.of(
            AUTO, AUTO,
            AUTOMATIK + " ein", AUTO,
            VAL_AUTO_OFF, AUS,
            VAL_OFF, AUS,
            VAL_INACTIVE, AUS,
            SPERRZEIT, AUS,
            EIN, EIN,
            BETRIEB, EIN
    );

    private final static Map<String, String> VAL_PROGRAMSTATES_ALIAS_MAP_LONG = Map.of(
            ABTAUBETRIEB, CONST_VAL_DEFROST,
            ABSENKBETRIEB, CONST_VAL_LOWERING
    );

    private final static Map<String, String> VAL_PROGRAMSTATES_ALIAS_MAP_SHORT= Map.of(
            ABTAUBETRIEB, CONST_VAL_DEFROST,
            ABSENKBETRIEB, CONST_VAL_LOWERING
    );

    HeatpumpBasementDatapoints(String alternateLabel, boolean hidden, int group, Function<String, String> formattedValueLong, Function<String, String> formattedValueShort, BigDecimal tendencyThreshold, Function<String, ConditionColor> colorBasedByValue){
        this.alternateLabel = alternateLabel;
        this.hidden = hidden;
        this.group = group;
        this.formattedValueLong = formattedValueLong;
        this.formattedValueShort = formattedValueShort;
        this.tendencyThreshold = tendencyThreshold;
        this.colorBasedByValue = colorBasedByValue;
    }

    @SneakyThrows
    public static BigDecimal valueAsBigDecimal(String valueWithUnit){
        var valueWithoutUnit = StringUtils.substringBefore(StringUtils.trimToEmpty(valueWithUnit), StringUtils.SPACE);
        DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(Locale.GERMAN);
        df.setParseBigDecimal(true);
        return (BigDecimal) df.parse(valueWithoutUnit);
    }

    private static int getOrangeTemperatureValue() {
        return VAL_TEMPERATURE_ORANGE;
    }

    public static boolean isValueOff(String value){
        return VAL_OFF.equalsIgnoreCase(value)
                || VAL_AUTO_OFF.equalsIgnoreCase(value)
                || VAL_STANDBY.equalsIgnoreCase(value)
                || VAL_INACTIVE.equalsIgnoreCase(value);
    }

    private static String formattedValueLong(String valueRaw) {
        var valueTrimmed = StringUtils.trimToEmpty(valueRaw);
        return VAL_ALIAS_MAP_LONG.getOrDefault(valueTrimmed, valueTrimmed);
    }

    private static String formattedValueShort(String valueRaw) {
        var valueTrimmed = StringUtils.trimToEmpty(valueRaw);
        var mapped = VAL_ALIAS_MAP_SHORT.getOrDefault(valueTrimmed, valueTrimmed);
        return StringUtils.remove(mapped, ' ');
    }

    private static String formattedProgramStatesValueLong(String valueRaw) {
        var valueTrimmed = StringUtils.trimToEmpty(valueRaw);
        return VAL_PROGRAMSTATES_ALIAS_MAP_LONG.getOrDefault(valueTrimmed, valueTrimmed);
    }

    private static String formattedProgramStatesValueShort(String valueRaw) {
        var valueTrimmed = StringUtils.trimToEmpty(valueRaw);
        var mapped = VAL_PROGRAMSTATES_ALIAS_MAP_SHORT.getOrDefault(valueTrimmed, valueTrimmed);
        return StringUtils.remove(mapped, ' ');
    }
}
