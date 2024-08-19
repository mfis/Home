package de.fimatas.home.library.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Setter
@Getter
public abstract class AbstractSystemModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    protected long timestamp;
}
