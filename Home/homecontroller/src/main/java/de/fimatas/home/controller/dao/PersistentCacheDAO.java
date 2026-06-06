package de.fimatas.home.controller.dao;

import de.fimatas.home.controller.command.PersistentCacheCommand;
import de.fimatas.home.library.model.PersistentCacheEntry;
import de.fimatas.home.library.model.PersistentCacheKey;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Properties;

@CommonsLog
@Component
public class PersistentCacheDAO {

    public static final String PATH = DaoUtils.getConfigRoot() + "homecontrollerpersistentcache.properties";

    private Properties properties;

    private ObjectMapper objectMapper;

    @PostConstruct
    public void postConstruct() {
        properties = DaoUtils.getApplicationProperties(PATH);
        objectMapper = JsonMapper.builder().build();
    }

    @PreDestroy
    @Scheduled(cron = "45 7 0/6 * * *")
    public void preDestroyAndScheduled() {
        persist();
    }

    public synchronized void write(PersistentCacheCommand persistentCacheCommand) {
        properties.setProperty(persistentCacheCommand.getPersistentCacheKey().name(), mapToJson(new PersistentCacheEntry<>(LocalDateTime.now(), persistentCacheCommand.getInstantToWrite())));
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

    public <T> PersistentCacheEntry<T> read(PersistentCacheKey key, Class<T> clazz) {
        var json = (String) properties.get(key.name());
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

    private <T> PersistentCacheEntry<T> mapToObject(String json, Class<T> clazz) {
        try {
            JavaType type = objectMapper.getTypeFactory().constructParametricType(PersistentCacheEntry.class, clazz);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("error deserializing instantToWrite:", e);
            return null;
        }
    }

}
