package de.fimatas.home.controller.dao;

import lombok.extern.apachecommons.CommonsLog;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

@CommonsLog
public class PersistentCacheDAO {

    public static final String PATH = DaoUtils.getConfigRoot() + "homecontrollerpersistentcache.properties";
    private static PersistentCacheDAO instance;

    private final Properties properties;

    private final ObjectMapper objectMapper;

    private PersistentCacheDAO() {
        super();
        properties = DaoUtils.getApplicationProperties(PATH);
        objectMapper = JsonMapper.builder().build();
    }

    public static synchronized PersistentCacheDAO getInstance() {
        if (instance == null) {
            instance = new PersistentCacheDAO();
        }
        return instance;
    }

    public synchronized void write(String key, Object instantToWrite) {
        properties.setProperty(key, mapToJson(instantToWrite));
        persist();
    }

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

    public <T> T read(String key, Class<T> clazz) {
        var json = (String) properties.get(key);
        if(json == null) return null;
        return mapToObject(json, clazz);
    }

    private String mapToJson(Object instantToWrite) {
        try {
            return objectMapper.writeValueAsString(instantToWrite);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("error serializing instantToWrite:", e);
        }
    }

    private <T> T mapToObject(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("error deserializing instantToWrite:", e);
            return null;
        }
    }

}
