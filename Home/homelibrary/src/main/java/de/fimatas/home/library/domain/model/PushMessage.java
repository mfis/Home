package de.fimatas.home.library.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PushMessage {

    private long timestamp;

    private String username;

    private String title;

    private String textMessage;
}
