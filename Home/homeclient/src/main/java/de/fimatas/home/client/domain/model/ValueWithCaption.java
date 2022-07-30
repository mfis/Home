package de.fimatas.home.client.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValueWithCaption implements Serializable {

    private static final long serialVersionUID = 1L;

    private String value = "";

    private String caption = "";

    private String cssClass = "";

}
