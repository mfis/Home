package de.fimatas.home.client.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class LockView extends View {

    private String caption = "";

    private String lastOpened = "";

    private String linkAuto = "#";

    private String linkAutoEvent = "#";

    private String linkManual = "#";

    private String autoInfoText = "";

    private String linkLock = "#";

    private String linkUnlock = "#";

    private String linkOpen = "#";

}
