package de.fimatas.home.controller.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fimatas.home.library.model.SettingsModel;

public class SettingsDAO {

    private static SettingsDAO instance;

    private Properties properties = null;

    private ObjectMapper objectMapper;

    private static final String PUSHTOKEN_PREFIX = "pushToken.";

    private SettingsDAO() {
        super();
        properties = getApplicationProperties();
        objectMapper = new ObjectMapper();
    }

    public static synchronized SettingsDAO getInstance() {
        if (instance == null) {
            instance = new SettingsDAO();
        }
        return instance;
    }

    public synchronized void write(SettingsModel settingsModel) {
        properties.setProperty(PUSHTOKEN_PREFIX + settingsModel.getToken(), mapToJson(settingsModel));
        try {
            FileOutputStream fos = new FileOutputStream(
                new File(System.getProperty("user.home") + "/documents/config/homecontrolleruser.properties"));
            properties.store(fos, "");
            fos.flush();
            fos.close();
        } catch (IOException ioe) {
            throw new IllegalStateException("error writing external properties:", ioe);
        }
    }

    public void delete(SettingsModel settingsModel) {
        properties.remove(PUSHTOKEN_PREFIX + settingsModel.getToken());
    }

    public List<SettingsModel> read() {

        List<SettingsModel> names = new LinkedList<>();
        properties.keySet().stream().filter(key -> ((String) key).startsWith(PUSHTOKEN_PREFIX))
            .forEach(key -> names.add(mapToObject(properties.getProperty((String) key))));
        return names;
    }

    private Properties getApplicationProperties() {
        properties = new Properties();
        File file = new File(System.getProperty("user.home") + "/documents/config/homecontrolleruser.properties");
        try (var stream = new FileInputStream(file)) {
            properties.load(stream);
        } catch (Exception e) {
            throw new IllegalStateException("Properties could not be loaded", e);
        }
        return properties;
    }

    private String mapToJson(SettingsModel settingsModel) {
        try {
            return objectMapper.writeValueAsString(settingsModel);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("error serializing settingsModel:", e);
        }
    }

    private SettingsModel mapToObject(String json) {
        try {
            return objectMapper.readValue(json, SettingsModel.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("error deserializing settingsModel:", e);
        }
    }

}
