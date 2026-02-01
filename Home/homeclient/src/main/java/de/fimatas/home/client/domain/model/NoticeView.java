package de.fimatas.home.client.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeView extends View {

    private String id;

    private String title;

    private String userName;

    private String userIcon;

    private String lastEditedText;

    private long lastEditedMillis;
}
