package de.fimatas.home.controller.dao;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class DaoUtils {

    public static String cleanSqlValue(String string){
        String REGEXP_CLEAN = "[^a-zA-Z\\d\\s-_:.,'äöüÄÖÜß°]";
        return string.replaceAll(REGEXP_CLEAN, "");
    }

    public static Properties getApplicationProperties(String path) {
        Properties properties = new Properties();
        try (var stream = new FileInputStream(path)) {
            properties.load(stream);
        } catch (Exception e) {
            throw new IllegalStateException("Properties could not be loaded", e);
        }
        return properties;
    }

    public static String getConfigRoot(){
        if(new File("/opt/homecontroller").exists()){
            return "/opt/homecontroller/";
        }else{
            return System.getProperty("user.home") + "/documents/config/homecontroller/";
        }
    }
}
