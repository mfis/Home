package de.fimatas.home.client.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class ViewCorrelationView implements Serializable {

    private static final long serialVersionUID = 1L;

    private String from = "";

    private String to = "";
}
