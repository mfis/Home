package de.fimatas.home.library.domain.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
