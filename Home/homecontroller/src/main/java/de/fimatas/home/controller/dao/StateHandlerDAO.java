package de.fimatas.home.controller.dao;

import de.fimatas.home.controller.database.mapper.StateRowMapper;
import de.fimatas.home.controller.model.State;
import de.fimatas.home.controller.service.UniqueTimestampService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import static de.fimatas.home.controller.dao.DaoUtils.cleanSqlValue;

@Component
@CommonsLog
public class StateHandlerDAO {

    private final String TABLE_NAME = "STATES";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UniqueTimestampService uniqueTimestampService;

    @Getter
    private boolean setupIsRunning = true;

    public void completeInit(){
        setupIsRunning = false;
    }

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

        return jdbcTemplate.query(query, new StateRowMapper(), groupname);
    }

    @Transactional(readOnly = true)
    public State readState(String groupname, String statename){

        String query =
                "select * FROM " + TABLE_NAME + " where GROUPNAME = ? and statename = ?;";

        var result = jdbcTemplate.query(query, new StateRowMapper(), cleanSqlValue(groupname), cleanSqlValue(statename));
        if(result.isEmpty()){
            return null;
        }else if(result.size()==1){
            return result.get(0);
        }else{
            throw new IllegalStateException("incorrect result size: " + result.size());
        }
    }

    @Transactional
    public void writeState(String groupname, String statename, String value){

        if (setupIsRunning) {
            throw new IllegalStateException("setup is still running");
        }

        jdbcTemplate
                .update("MERGE INTO " + TABLE_NAME + " KEY (GROUPNAME, STATENAME) VALUES (?, ?, ?, ?)",
                        cleanSqlValue(groupname), cleanSqlValue(statename), uniqueTimestampService.getAsStringWithMillis(), cleanSqlValue(value));
    }
}
