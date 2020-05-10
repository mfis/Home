package de.fimatas.home.controller.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

public class ExternalPropertiesDAO {

    private static ExternalPropertiesDAO instance;

    Properties properties = null;

    private ExternalPropertiesDAO() {
        super();
        properties = getApplicationProperties();
    }

    public static synchronized ExternalPropertiesDAO getInstance() {
        if (instance == null) {
            instance = new ExternalPropertiesDAO();
        }
        return instance;
    }

    public synchronized void write(String key, String value) {
        properties.setProperty(key, value);
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

    public void delete(String key) {
        properties.remove(key);
    }

    public String read(String key) {
        if (key == null) {
            return null;
        }
        return properties.getProperty(key);
    }

    public List<String> lookupNamesContainingString(String string) {

        List<String> names = new LinkedList<>();
        for (Object key : properties.keySet()) {
            if (StringUtils.contains(key.toString(), string)) {
                names.add(key.toString());
            }
        }
        return names;
    }

    private Properties getApplicationProperties() {
        properties = new Properties();
        try {
            File file = new File(System.getProperty("user.home") + "/documents/config/homecontrolleruser.properties");
            properties.load(new FileInputStream(file));
            return properties;
        } catch (Exception e) {
            throw new IllegalStateException("Properties could not be loaded", e);
        }
    }

}
