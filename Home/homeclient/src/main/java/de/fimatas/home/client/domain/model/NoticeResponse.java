package de.fimatas.home.client.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeResponse extends View {

    private String text;

    private String version;
}