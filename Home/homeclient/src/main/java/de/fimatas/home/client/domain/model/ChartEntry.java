package de.fimatas.home.client.domain.model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class ChartEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private String label = "";

    private String additionalLabel = "";

    private List<ValueWithCaption> valuesWithCaptions = new LinkedList<>();

    private String collapse = "";

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<ValueWithCaption> getValuesWithCaptions() {
        return valuesWithCaptions;
    }

    public void setValuesWithCaptions(List<ValueWithCaption> valuesWithCaptions) {
        this.valuesWithCaptions = valuesWithCaptions;
    }

    public String getAdditionalLabel() {
        return additionalLabel;
    }

    public void setAdditionalLabel(String additionalLabel) {
        this.additionalLabel = additionalLabel;
    }

    public String getCollapse() {
        return collapse;
    }

    public void setCollapse(String collapse) {
        this.collapse = collapse;
    }

}
