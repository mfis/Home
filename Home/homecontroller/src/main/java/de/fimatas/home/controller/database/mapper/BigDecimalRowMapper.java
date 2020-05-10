package de.fimatas.home.controller.database.mapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class BigDecimalRowMapper implements RowMapper<BigDecimal> {

    private String colName;

    public BigDecimalRowMapper(String colName) {
        this.colName = colName;
    }

    @Override
    public BigDecimal mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getBigDecimal(colName);
    }

}
