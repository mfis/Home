package de.fimatas.home.library.domain.model;

import java.util.LinkedList;
import java.util.List;

public class PushMessageModel {

    private List<PushMessage> list = new LinkedList<>();

    private long timestamp;

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

    public List<PushMessage> getList() {
        return list;
    }


    public boolean isAdditionalEntries() {
        return additionalEntries;
    }

    public void setAdditionalEntries(boolean additionalEntries) {
        this.additionalEntries = additionalEntries;
    }

}
