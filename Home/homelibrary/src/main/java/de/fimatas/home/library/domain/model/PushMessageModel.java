package de.fimatas.home.library.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Getter
public class PushMessageModel extends AbstractSystemModel{

    private final List<PushMessage> list = new LinkedList<>();

    @Setter
    private boolean additionalEntries;

    public PushMessageModel() {
        setTimestamp(System.currentTimeMillis());
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }


}
