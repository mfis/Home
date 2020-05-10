package de.fimatas.home.library.domain.model;

import java.io.Serializable;

public class ValueWithTendency<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;

    private T value;

    private T referenceValue;

    private long referenceDateTime;

    private Tendency tendency;

    public ValueWithTendency(T value) {
        super();
        this.value = value;
    }

    public ValueWithTendency() {
        super();
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getReferenceValue() {
        return referenceValue;
    }

    public void setReferenceValue(T referenceValue) {
        this.referenceValue = referenceValue;
    }

    public long getReferenceDateTime() {
        return referenceDateTime;
    }

    public void setReferenceDateTime(long referenceDateTime) {
        this.referenceDateTime = referenceDateTime;
    }

    public Tendency getTendency() {
        return tendency;
    }

    public void setTendency(Tendency tendency) {
        this.tendency = tendency;
    }
}
