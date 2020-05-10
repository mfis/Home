package de.fimatas.home.library.domain.model;

import java.io.Serializable;

public class BackupFile implements Serializable {

    private static final long serialVersionUID = 1L;

    private byte[] bytes;

    private String filename;

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

}