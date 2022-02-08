package de.fimatas.home.adapter.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigInteger;
import java.util.Map;

@Data
class AuthenticationFields {

    @JsonProperty
    private String pin;

    @JsonProperty
    private String mac;

    @JsonProperty
    private BigInteger salt;

    @JsonProperty
    private byte[] privateKey;

    @JsonProperty
    private String setupId;

    @JsonProperty
    private Map<String, byte[]> userMap;
}
