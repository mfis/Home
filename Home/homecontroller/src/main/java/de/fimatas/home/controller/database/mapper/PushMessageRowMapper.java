package de.fimatas.home.controller.database.mapper;

import de.fimatas.home.library.domain.model.PushMessage;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PushMessageRowMapper implements RowMapper<PushMessage> {

    @Override
    public PushMessage mapRow(ResultSet rs, int rowNum) throws SQLException {

        var pushMsg = new PushMessage();
        pushMsg.setTimestamp(rs.getTimestamp("TS").getTime());
        pushMsg.setUsername(rs.getString("USERNAME"));
        pushMsg.setTitle(rs.getString("TITLE"));
        pushMsg.setTextMessage(rs.getString("TEXTMSG"));
        return pushMsg;
    }

}
