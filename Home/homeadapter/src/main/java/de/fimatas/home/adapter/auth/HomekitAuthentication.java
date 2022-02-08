package de.fimatas.home.adapter.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.github.hapjava.server.HomekitAuthInfo;
import io.github.hapjava.server.impl.HomekitServer;
import io.github.hapjava.server.impl.crypto.HAPSetupCodeUtils;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

public class HomekitAuthentication implements HomekitAuthInfo  {

    private static final File PERSISTENCE_FILE = new File(
            System.getProperty("user.home") + File.separator + "documents" + File.separator + "config" + File.separator + "homekitAuth.json"
    );

    private AuthenticationFields fields;

    @SneakyThrows
    private HomekitAuthentication() {
        loadOrCreateAndPersist();
    }

    private static final class InstanceHolder {
        private static final HomekitAuthentication instance = new HomekitAuthentication();
    }

    public static HomekitAuthentication getInstance()  {
        return InstanceHolder.instance;
    }

    @Override
    public String getPin() {
        return fields.getPin();
    }

    @Override
    public String getMac() {
        return fields.getMac();
    }

    @Override
    public BigInteger getSalt() {
        return fields.getSalt();
    }

    @Override
    public byte[] getPrivateKey() {
        return fields.getPrivateKey();
    }

    @Override
    public String getSetupId() {
        return fields.getSetupId();
    }

    @Override
    public synchronized void createUser(String username, byte[] publicKey) {
        if(StringUtils.isBlank(fields.getPin())){
            throw new IllegalStateException("PIN is not set while creating user!");
        }
        if (!fields.getUserMap().containsKey(username)) {
            fields.getUserMap().put(username, publicKey);
            save();
        }
    }

    @Override
    public void removeUser(String username) {
        fields.getUserMap().remove(username);
        save();
    }

    @Override
    public byte[] getUserPublicKey(String username) {
        return fields.getUserMap().get(username);
    }

    @Override
    public boolean hasUser() {
        return !fields.getUserMap().isEmpty();
    }

    public void setPin(String pin){
        if(!StringUtils.equals(fields.getPin(), pin)){
            fields.setPin(pin);
            save();
        }
    }

    public String getSetupURI(){
        return HAPSetupCodeUtils.getSetupURI(fields.getPin().replace("-",""), fields.getSetupId(), 2);
    }

    @SneakyThrows
    private void loadOrCreateAndPersist() {

        if(PERSISTENCE_FILE.exists() && FileUtils.sizeOf(PERSISTENCE_FILE) > 0){
            fields = new ObjectMapper().readValue(PERSISTENCE_FILE, AuthenticationFields.class);
            save();
        }else{
            fields = new AuthenticationFields();
            fields.setPin(StringUtils.EMPTY);
            fields.setMac(HomekitServer.generateMac());
            fields.setSalt(HomekitServer.generateSalt());
            fields.setPrivateKey(HomekitServer.generateKey());
            fields.setSetupId(HAPSetupCodeUtils.generateSetupId());
            fields.setUserMap(new ConcurrentHashMap<>());
        }
    }

    @SneakyThrows
    private synchronized void save() {
        ObjectWriter writer = new ObjectMapper().writer();
        writer = writer.withDefaultPrettyPrinter();
        writer.writeValue(PERSISTENCE_FILE, fields);
    }

}