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
        sb.append(StringUtils.leftPad(systemUptime, 15));
        sb.append("\n");
        sb.append(StringUtils.rightPad("Application-Uptime", maxLength, '.')).append(": ");
        sb.append(StringUtils.leftPad(appUptime, 15));
        sb.append("\n");
        return sb.toString();
    }
}
