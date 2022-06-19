package de.fimatas.home.controller.dao;

import de.fimatas.home.controller.database.mapper.StateRowMapper;
import de.fimatas.home.controller.model.State;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@CommonsLog
public class StateHandlerDAO {

    private final String TABLE_NAME = "STATES";

    private static final DateTimeFormatter SQL_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    @Transactional(propagation = Propagation.REQUIRED)
    public void createTables() {

        jdbcTemplate.update("CREATE CACHED TABLE IF NOT EXISTS " + TABLE_NAME
                + " (GROUPNAME VARCHAR(16) NOT NULL, STATENAME VARCHAR(16) NOT NULL, TS DATETIME NOT NULL, VAL VARCHAR(16) NOT NULL, PRIMARY KEY (GROUPNAME, STATENAME));");
        jdbcTemplate
                .update("CREATE UNIQUE INDEX IF NOT EXISTS " + "IDX1_" + TABLE_NAME + " ON " + TABLE_NAME + " (GROUPNAME, STATENAME);");
    }

    @Transactional(readOnly = true)
    public List<State> readStates(String groupname){

        String query =
                "select * FROM " + TABLE_NAME + " where GROUPNAME = ?;";

        return jdbcTemplate.query(query, new String[]{groupname}, new StateRowMapper());
    }

    @Transactional(readOnly = true)
    public State readState(String groupname, String statename){

        String query =
                "select * FROM " + TABLE_NAME + " where GROUPNAME = ? and statename = ?;";

        return jdbcTemplate.queryForObject(query, new String[]{groupname, cleanString(statename)}, new StateRowMapper());
    }

    @Transactional
    public void writeState(String groupname, String statename, String value){
        jdbcTemplate
                .update("MERGE INTO " + TABLE_NAME + " KEY (GROUPNAME, STATENAME) VALUES (?, ?, ?, ?)",
                        groupname, cleanString(statename), SQL_TIMESTAMP_FORMATTER.format(LocalDateTime.now()), cleanString(value));
    }

    private String cleanString(String string){
        String REGEXP_CLEAN = "[^a-zA-Z\\d-]";
        return string.replaceAll(REGEXP_CLEAN, string);
    }
}
