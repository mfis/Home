package de.fimatas.home.controller.database.mapper;

import de.fimatas.home.controller.model.Ticket;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TicketRowMapper implements RowMapper<Ticket> {

    @Override
    public Ticket mapRow(ResultSet rs, int rowNum) throws SQLException {

        var ticket = new Ticket();
        ticket.setTicket(rs.getString("TICKET"));
        ticket.setEvent(rs.getString("EVENT"));
        ticket.setValue(rs.getString("VAL"));
        ticket.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(rs.getTimestamp("TS").getTime()), ZoneId.systemDefault()));
        return ticket;
    }

}
