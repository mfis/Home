package de.fimatas.home.client.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class TableRowView {

    private ValueWithCaption valueWithCaption;

    private String secondRowValue = null;

    private String historyKey;
}
