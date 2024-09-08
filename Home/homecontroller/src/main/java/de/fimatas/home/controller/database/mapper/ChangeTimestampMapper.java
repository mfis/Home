package de.fimatas.home.controller.database.mapper;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ChangeTimestampMapper implements RowMapper<LocalDateTime> {

    @Override
    public LocalDateTime mapRow(ResultSet rs, int rowNum) throws SQLException {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(rs.getTimestamp("CHANGETS").getTime()), ZoneId.systemDefault());
    }

}
