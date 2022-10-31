package de.fimatas.home.controller.dao;

import de.fimatas.home.controller.database.mapper.EvChargingMapper;
import de.fimatas.home.controller.model.EvChargeDatabaseEntry;
import de.fimatas.home.library.domain.model.ElectricVehicle;
import de.fimatas.home.library.domain.model.EvChargePoint;
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
                new EvChargingMapper(), ev.name(), SQL_TIMESTAMP_FORMATTER.format(startTS));
    }

    @Transactional(readOnly = true)
    public boolean unfinishedChargingOnDB(){

        return !jdbcTemplate.query(
                "select * FROM " + TABLE_NAME + " where ENDTS is null;", new EvChargingMapper()).isEmpty();
    }

    @Transactional
    public synchronized void finishAll(){

        jdbcTemplate
                .update("UPDATE " + TABLE_NAME + " SET ENDTS = ? WHERE ENDTS is null",
                        SQL_TIMESTAMP_FORMATTER.format(LocalDateTime.now()));
    }

    @Transactional
    public synchronized void write(ElectricVehicle ev, BigDecimal counter, EvChargePoint chargePoint){

        final List<EvChargeDatabaseEntry> entryList = jdbcTemplate.query(
                "select * FROM " + TABLE_NAME + " where EVNAME = ? and ENDTS is null;",
                new EvChargingMapper(), ev.name());

        if(entryList.size()>1){
            log.error("Unexpected row count: " + entryList.size());
        }else if(entryList.size()==1){
            jdbcTemplate
                    .update("UPDATE " + TABLE_NAME + " SET ENDTS = ?, ENDVAL = ?, MAXVAL = ? WHERE EVNAME = ? AND ENDTS is null",
                            null, counter, entryList.get(0).getEndVal().compareTo(counter) > 0 ? entryList.get(0).getEndVal() : counter, ev.name());
        }else{
            jdbcTemplate
                    .update("INSERT INTO " + TABLE_NAME + " (STARTTS, ENDTS, CHARGEPOINT, EVNAME, STARTVAL, ENDVAL, MAXVAL) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            SQL_TIMESTAMP_FORMATTER.format(LocalDateTime.now()), null,
                            chargePoint.getNumber(), ev.name(), counter, counter, counter);
        }
    }

}
