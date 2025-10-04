package de.fimatas.home.client.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValueWithCaption implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String value = "";

    private String caption = "";

    private String cssClass = "";

    private String tendencyIcon = "";

    public ValueWithCaption(String value, String caption, String cssClass){
        this.value = value;
        this.caption = caption;
        this.cssClass = cssClass;
    }

}
