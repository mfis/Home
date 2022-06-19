package de.fimatas.home.controller.database.mapper;

import de.fimatas.home.controller.model.State;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class StateRowMapper implements RowMapper<State> {

    @Override
    public State mapRow(ResultSet rs, int rowNum) throws SQLException {

        var state = new State();
        state.setGroupname(rs.getString("GROUPNAME"));
        state.setStatename(rs.getString("STATENAME"));
        state.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(rs.getTimestamp("TS").getTime()), ZoneId.systemDefault()));
        state.setValue(rs.getString("VAL"));
        return state;
    }

}
