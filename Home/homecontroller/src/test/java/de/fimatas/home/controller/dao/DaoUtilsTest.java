package de.fimatas.home.controller.dao;

import org.junit.jupiter.api.Test;

import static de.fimatas.home.controller.dao.DaoUtils.cleanSqlValue;
import static org.junit.jupiter.api.Assertions.*;

class DaoUtilsTest {

    @Test
    void testCleanSqlValueIsEqual() {
        assertEquals("ABC", cleanSqlValue("ABC"));
        assertEquals("ABCabc123", cleanSqlValue("ABCabc123"));
        assertEquals("- : . , ä ö ü Ä Ö Ü ß", cleanSqlValue("- : . , ä ö ü Ä Ö Ü ß"));
    }


    @Test
    void testCleanSqlValueIsNotEqual() {
        assertEquals("ABCE", cleanSqlValue("ABC@E"));
        assertEquals("xy", cleanSqlValue("(xy);"));
    }
}