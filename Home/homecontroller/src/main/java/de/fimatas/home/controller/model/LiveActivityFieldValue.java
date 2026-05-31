package de.fimatas.home.controller.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class LiveActivityFieldValue {
    private BigDecimal value;
    private LiveActivityField field;
    private boolean unknown;
}
