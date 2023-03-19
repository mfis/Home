package de.fimatas.home.client.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class PushMessageView implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id = "";

    private String timestamp = "";

    private String title = "";

    private String message = "";

}
