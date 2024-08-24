package de.fimatas.home.library.model;

import de.fimatas.home.library.domain.model.AbstractSystemModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

@EqualsAndHashCode(callSuper = true)
@Data
public class ControllerStateModel extends AbstractSystemModel {

    private String systemUptime;

    private String appUptime;

    @SuppressWarnings("StringBufferReplaceableByString")
    public String print(){
        StringBuilder sb = new StringBuilder();
        int maxLength = 21;
        sb.append(StringUtils.rightPad("System-Uptime", maxLength, '.')).append(": ");
        sb.append(systemUptime);
        sb.append("\n");
        sb.append(StringUtils.rightPad("Application-Uptime", maxLength, '.')).append(": ");
        sb.append(appUptime);
        sb.append("\n");
        return sb.toString();
    }
}
