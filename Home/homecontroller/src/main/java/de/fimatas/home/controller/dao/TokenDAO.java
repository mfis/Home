package de.fimatas.home.controller.dao;

import org.springframework.scheduling.annotation.Async;

import jakarta.annotation.PreDestroy;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class TokenDAO {

    public static final String PATH = DaoUtils.getConfigRoot() + "homecontrollertoken.properties";
    private static TokenDAO instance;

    private final Properties properties;

    private TokenDAO() {
        super();
        properties = DaoUtils.getApplicationProperties(PATH);
    }

    public static synchronized TokenDAO getInstance() {
        if (instance == null) {
            instance = new TokenDAO();
        }
        return instance;
    }

    public String read(String category, String key) {
        return properties.getProperty(category + "." + key, null);
    }

    public synchronized void write(String category, String key, String value) {
        properties.setProperty(category + "." + key, value);
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

}
