package de.fimatas.home.client.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class AppAttributeModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String value;
}
