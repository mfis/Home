package de.fimatas.home.library.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class Doorlock extends AbstractDeviceModel {

    public Doorlock() {
        super();
    }

    private boolean open;

    private boolean lockState;

    private boolean lockStateUncertain;

    private Boolean lockAutomation;

    private Boolean lockAutomationEvent;

    private String lockAutomationInfoText;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime lastOpened;

}
