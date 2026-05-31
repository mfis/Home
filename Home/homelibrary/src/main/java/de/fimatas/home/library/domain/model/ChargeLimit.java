package de.fimatas.home.library.domain.model;

import de.fimatas.home.library.model.ConditionColor;
import lombok.Getter;

@Getter
public enum ChargeLimit {

    _15((short)15, "15%", ConditionColor.RED, false), //
    _20((short)20, "20%", ConditionColor.ORANGE, false), //
    _25((short)25, "25%", ConditionColor.GREEN, true), //
    _40((short)40, "40%", ConditionColor.GREEN, true), //
    _50((short)50, "50%", ConditionColor.GREEN, true), //
    _80((short)80, "80%", ConditionColor.GREEN, true), //
    _85((short)85, "85%", ConditionColor.ORANGE, true), //
    _90((short)90, "90%", ConditionColor.ORANGE, true), //
    MAX((short)100, "Max", ConditionColor.RED, true), //
    ;

    private final short percentage;

    private final String caption;

    private final ConditionColor color;

    private final boolean show;

    ChargeLimit(short percentage, String caption,  ConditionColor color,  boolean show) {
        this.percentage = percentage;
        this.caption = caption;
        this.color = color;
        this.show = show;
        if(this.name().length()>8) throw new IllegalArgumentException("name too long: " + this.name());
    }

}
