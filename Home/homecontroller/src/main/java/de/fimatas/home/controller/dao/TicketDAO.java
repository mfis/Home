package de.fimatas.home.controller.dao;

import de.fimatas.home.controller.database.mapper.TicketRowMapper;
import de.fimatas.home.controller.model.Ticket;
import de.fimatas.home.controller.service.UniqueTimestampService;
import jakarta.annotation.PostConstruct;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static de.fimatas.home.controller.dao.DaoUtils.cleanSqlValue;

@Component
@CommonsLog
public class TicketDAO {

    private final String TABLE_NAME = "TICKETS";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UniqueTimestampService uniqueTimestampService;

    @PostConstruct
    @Transactional(propagation = Propagation.REQUIRED)
    public void createTables() {

        jdbcTemplate.update("CREATE CACHED TABLE IF NOT EXISTS " + TABLE_NAME
                + " (TICKET VARCHAR(60) NOT NULL, EVENT VARCHAR(10) NOT NULL, VAL VARCHAR(10) NOT NULL, TS DATETIME NOT NULL, PRIMARY KEY (TICKET));");
        jdbcTemplate
                .update("CREATE UNIQUE INDEX IF NOT EXISTS " + "IDX1_" + TABLE_NAME + " ON " + TABLE_NAME + " (TICKET);");
        jdbcTemplate
                .update("CREATE INDEX IF NOT EXISTS " + "IDX2_" + TABLE_NAME + " ON " + TABLE_NAME + " (EVENT, VAL);");
    }

    @Transactional(readOnly = true)
    public boolean existsUsedTicket(String ticket){

        String query = "select * FROM " + TABLE_NAME + " where TICKET = ?;";
        var result = jdbcTemplate.query(query, new TicketRowMapper(), cleanSqlValue(ticket));
        return !result.isEmpty();
    }

    @Transactional(readOnly = true)
    public Ticket readLatestTicket(String event, String value){

        String query = "select * FROM " + TABLE_NAME + " where EVENT = ? AND VAL = ? ORDER BY TS DESC FETCH FIRST ROW ONLY;";
        var result = jdbcTemplate.query(query, new TicketRowMapper(), cleanSqlValue(event), cleanSqlValue(value));
        if(result.isEmpty()){
            return null;
        }else if(result.size()==1){
            return result.get(0);
        }else{
            throw new IllegalStateException("incorrect result size: " + result.size());
        }
    }

    @Transactional
    public void write(String ticket, String event, String value){

        jdbcTemplate
                .update("INSERT INTO " + TABLE_NAME + " (TICKET, EVENT, VAL, TS) VALUES (?, ?, ?, ?)",
                        cleanSqlValue(ticket), cleanSqlValue(event), cleanSqlValue(value), uniqueTimestampService.getAsStringWithMillis());
    }
}
