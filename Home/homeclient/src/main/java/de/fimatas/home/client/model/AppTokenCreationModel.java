package de.fimatas.home.client.model;

import java.io.Serializable;

public class AppTokenCreationModel implements Serializable {

    private static final long serialVersionUID = 1L;

    boolean success;

    String token;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
