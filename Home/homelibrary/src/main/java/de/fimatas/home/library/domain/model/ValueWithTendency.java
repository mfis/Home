package de.fimatas.home.library.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Setter
@Getter
public class ValueWithTendency<T extends Serializable> implements Serializable {

    @Serial
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

}
