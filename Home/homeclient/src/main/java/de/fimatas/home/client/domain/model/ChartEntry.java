package de.fimatas.home.client.domain.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

@Data
public class ChartEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private String label = "";

    private BigDecimal numericValue;

    private String additionalLabel = "";

    private List<ValueWithCaption> valuesWithCaptions = new LinkedList<>();

    private String collapse = "";

}
