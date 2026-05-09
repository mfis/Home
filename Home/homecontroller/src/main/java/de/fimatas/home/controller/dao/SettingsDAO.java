package de.fimatas.home.controller.dao;

import de.fimatas.home.library.model.SettingsModel;
import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.annotation.Async;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class SettingsDAO {

    public static final String PATH = DaoUtils.getConfigRoot() + "homecontrolleruser.properties";
    private static SettingsDAO instance;

    private final Properties properties;

    private final ObjectMapper objectMapper;

    private static final String PUSHTOKEN_PREFIX = "pushToken.";

    private SettingsDAO() {
        super();
        properties = DaoUtils.getApplicationProperties(PATH);
        objectMapper = JsonMapper.builder().build();
    }

    public static synchronized SettingsDAO getInstance() {
        if (instance == null) {
            instance = new SettingsDAO();
        }
        return instance;
    }

    public synchronized void write(SettingsModel settingsModel) {
        properties.setProperty(PUSHTOKEN_PREFIX + settingsModel.getToken(), mapToJson(settingsModel));
    }

    @PreDestroy
    @Async
    public synchronized void persist() {
        try {
            FileOutputStream fos = new FileOutputStream(PATH);
            properties.store(fos, "");
            fos.flush();
            fos.close();
        } catch (IOException ioe) {
            throw new IllegalStateException("error writing external properties:", ioe);
        }
    }

    @SuppressWarnings("unused")
    public void delete(SettingsModel settingsModel) {
        properties.remove(PUSHTOKEN_PREFIX + settingsModel.getToken());
    }

    public List<SettingsModel> read() {

        List<SettingsModel> names = new LinkedList<>();
        properties.keySet().stream().filter(key -> ((String) key).startsWith(PUSHTOKEN_PREFIX))
            .forEach(key -> names.add(mapToObject(properties.getProperty((String) key))));
        return names;
    }

    private String mapToJson(SettingsModel settingsModel) {
        try {
            return objectMapper.writeValueAsString(settingsModel);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("error serializing settingsModel:", e);
        }
    }

    private SettingsModel mapToObject(String json) {
        try {
            return objectMapper.readValue(json, SettingsModel.class);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("error deserializing settingsModel:", e);
        }
    }

}
