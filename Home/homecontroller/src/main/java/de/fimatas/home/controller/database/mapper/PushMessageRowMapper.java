package de.fimatas.home.controller.database.mapper;

import de.fimatas.home.controller.model.PushMessage;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class PushMessageRowMapper implements RowMapper<PushMessage> {

    @Override
    public PushMessage mapRow(ResultSet rs, int rowNum) throws SQLException {

        var pushMsg = new PushMessage();
        pushMsg.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(rs.getTimestamp("TS").getTime()), ZoneId.systemDefault()));
        pushMsg.setUsername(rs.getString("USERNAME"));
        pushMsg.setTitle(rs.getString("TITLE"));
        pushMsg.setTextMessage(rs.getString("TEXTMSG"));
        return pushMsg;
    }

}
