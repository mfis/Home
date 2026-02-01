package de.fimatas.home.controller.database.mapper;

import de.fimatas.home.library.model.Notice;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class NoticeRowMapper implements RowMapper<Notice> {

    @Override
    public Notice mapRow(ResultSet rs, int rowNum) throws SQLException {

        var notice = new Notice();
        notice.setId(rs.getString("ID"));
        notice.setVersion(rs.getInt("VERSION"));
        notice.setLastEdited(LocalDateTime.ofInstant(Instant.ofEpochMilli(rs.getTimestamp("EDITED").getTime()), ZoneId.systemDefault()));
        notice.setUser(rs.getString("USERNAME"));
        notice.setMultiUser(Boolean.parseBoolean(rs.getString("MULTIUSER")));
        notice.setText(rs.getString("TEXT"));
        return notice;
    }
}
