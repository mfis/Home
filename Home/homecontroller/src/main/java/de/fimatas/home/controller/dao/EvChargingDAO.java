package de.fimatas.home.controller.dao;

import de.fimatas.home.controller.database.mapper.EvChargingMapper;
import de.fimatas.home.controller.database.mapper.StateRowMapper;
import de.fimatas.home.controller.model.EvChargeDatabaseEntry;
import de.fimatas.home.controller.model.State;
import de.fimatas.home.library.domain.model.ElectricVehicle;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@CommonsLog
public class EvChargingDAO {

    private final String TABLE_NAME = "EVCHARGING";

    private final int CHARGEPOINT_FIX = 1;

    private static final DateTimeFormatter SQL_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    @Transactional(propagation = Propagation.REQUIRED)
    public void createTables() {

        jdbcTemplate.update("CREATE CACHED TABLE IF NOT EXISTS " + TABLE_NAME
                + " (STARTTS DATETIME NOT NULL, ENDTS DATETIME, CHARGEPOINT INTEGER, EVNAME VARCHAR(8) NOT NULL, " +
                "STARTVAL DOUBLE NOT NULL, ENDVAL DOUBLE NOT NULL, MAXVAL DOUBLE NOT NULL, PRIMARY KEY (STARTTS, EVNAME));");
        jdbcTemplate
                .update("CREATE UNIQUE INDEX IF NOT EXISTS " + "IDX1_" + TABLE_NAME + " ON " + TABLE_NAME + " (STARTTS, EVNAME);");
        jdbcTemplate
                .update("CREATE UNIQUE INDEX IF NOT EXISTS " + "IDX2_" + TABLE_NAME + " ON " + TABLE_NAME + " (EVNAME, ENDTS);");
    }

    @Transactional(readOnly = true)
    public List<EvChargeDatabaseEntry> read(ElectricVehicle ev, LocalDateTime startTS){

        return jdbcTemplate.query(
                "select * FROM " + TABLE_NAME + " where EVNAME = ? and STARTTS >= ?;",
                new String[]{ev.name(), SQL_TIMESTAMP_FORMATTER.format(startTS)}, new EvChargingMapper());
    }

    @Transactional
    public synchronized void write(ElectricVehicle ev, BigDecimal counter, boolean finish){

        final List<EvChargeDatabaseEntry> entryList = jdbcTemplate.query(
                "select * FROM " + TABLE_NAME + " where EVNAME = ? and ENDTS = null;",
                new String[]{ev.name()}, new EvChargingMapper());

        if(entryList.size()>1){
            log.error("Unexpected row count: " + entryList.size());
        }else if(entryList.size()==1){
            jdbcTemplate
                    .update("UPDATE " + TABLE_NAME + " SET ENDTS = ?, ENDVAL = ?, MALVAL = ?",
                            finish?SQL_TIMESTAMP_FORMATTER.format(LocalDateTime.now()):null,
                            counter, entryList.get(0).getEndVal().compareTo(counter) > 1 ? entryList.get(0).getEndVal() : counter);
        }else{
            jdbcTemplate
                    .update("INSERT INTO " + TABLE_NAME + " (STARTTS, ENDTS, CHARGEPOINT, EVNAME, STARTVAL, ENDVAL, MAXVAL) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            SQL_TIMESTAMP_FORMATTER.format(LocalDateTime.now()), finish?SQL_TIMESTAMP_FORMATTER.format(LocalDateTime.now()):null,
                            CHARGEPOINT_FIX, ev.name(), counter, counter, counter);
        }
    }

}
