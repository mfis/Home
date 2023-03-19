package de.fimatas.home.client.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class PushMessagesView implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<PushMessageView> list;
}
