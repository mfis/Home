package de.fimatas.home.library.model;

import de.fimatas.home.library.domain.model.AbstractSystemModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

@EqualsAndHashCode(callSuper = true)
@Data
public class ControllerStateModel extends AbstractSystemModel {

    private String uptime;

    @SuppressWarnings("StringBufferReplaceableByString")
    public String print(){
        StringBuilder sb = new StringBuilder();
        int maxLength = 10;
        sb.append(StringUtils.rightPad("Uptime", maxLength, '.')).append(": ");
        sb.append(uptime);
        sb.append("\n");
        return sb.toString();
    }
}
