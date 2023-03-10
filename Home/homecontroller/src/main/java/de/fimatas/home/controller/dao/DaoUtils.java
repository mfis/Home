package de.fimatas.home.controller.dao;

public class DaoUtils {

    public static String cleanSqlValue(String string){
        String REGEXP_CLEAN = "[^a-zA-Z\\d\\s-_:.,'äöüÄÖÜß°]";
        return string.replaceAll(REGEXP_CLEAN, "");
    }
}
