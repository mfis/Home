package de.fimatas.home.controller.dao;

import de.fimatas.home.controller.database.mapper.ChangeTimestampMapper;
import de.fimatas.home.controller.database.mapper.EvChargingMapper;
import de.fimatas.home.controller.model.EvChargeDatabaseEntry;
import de.fimatas.home.controller.service.UniqueTimestampService;
import de.fimatas.home.library.domain.model.ElectricVehicle;
import de.fimatas.home.library.domain.model.EvChargePoint;
import lombok.Getter;
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
                + " (STARTTS DATETIME NOT NULL, ENDTS DATETIME, CHANGETS DATETIME NOT NULL, CHARGEPOINT INTEGER NOT NULL, EVNAME VARCHAR(8) NOT NULL, " +
                "STARTVAL DOUBLE NOT NULL, ENDVAL DOUBLE NOT NULL, MAXVAL DOUBLE NOT NULL, PRIMARY KEY (STARTTS, EVNAME));");
        jdbcTemplate
                .update("CREATE UNIQUE INDEX IF NOT EXISTS " + "IDX1_" + TABLE_NAME + " ON " + TABLE_NAME + " (STARTTS, EVNAME);");
        jdbcTemplate
                .update("CREATE UNIQUE INDEX IF NOT EXISTS " + "IDX2_" + TABLE_NAME + " ON " + TABLE_NAME + " (ENDTS);");
        jdbcTemplate
                .update("CREATE UNIQUE INDEX IF NOT EXISTS " + "IDX3_" + TABLE_NAME + " ON " + TABLE_NAME + " (CHANGETS);");
    }

    @Transactional(readOnly = true)
    public List<EvChargeDatabaseEntry> read(ElectricVehicle ev, LocalDateTime startTS){

        return jdbcTemplate.query(
                "select * FROM " + TABLE_NAME + " where EVNAME = ? and STARTTS >= ?;",
                new EvChargingMapper(), ev.name(), SQL_TIMESTAMP_FORMATTER.format(startTS));
    }

    @Transactional(readOnly = true)
    public boolean activeChargingOnDB(){

        return !jdbcTemplate.query(
                "select * FROM " + TABLE_NAME + " where ENDTS is null;", new EvChargingMapper()).isEmpty();
    }


   @Transactional(readOnly = true)
    public LocalDateTime maxChangeTimestamp(){

       final List<LocalDateTime> entries = jdbcTemplate.query(
               "select CHANGETS FROM " + TABLE_NAME + " ORDER BY CHANGETS DESC LIMIT 1;", new ChangeTimestampMapper());
       return entries.isEmpty()?null:entries.get(0);
   }

    @Transactional
    public synchronized void finishAll(){

        jdbcTemplate
                .update("UPDATE " + TABLE_NAME + " SET ENDTS = ? WHERE ENDTS is null",
                        uniqueTimestampService.getAsStringWithMillis());
    }

    @Transactional
    public synchronized void write(ElectricVehicle ev, BigDecimal counter, EvChargePoint chargePoint){

        if (setupIsRunning) {
            throw new IllegalStateException("setup is still running");
        }

        final List<EvChargeDatabaseEntry> entryList = jdbcTemplate.query(
                "select * FROM " + TABLE_NAME + " where EVNAME = ? and ENDTS is null;",
                new EvChargingMapper(), ev.name());

        if(entryList.size()>1){
            log.error("write() -> Unexpected row count: " + entryList.size());
        }else if(entryList.size()==1){
            if(counter.compareTo(entryList.get(0).getEndVal()) != 0){
                log.debug("write() -> update new value=" + counter);
                jdbcTemplate
                        .update("UPDATE " + TABLE_NAME + " SET CHANGETS = ?, ENDVAL = ?, MAXVAL = ? WHERE EVNAME = ? AND ENDTS is null",
                                uniqueTimestampService.getAsStringWithMillis(),
                                counter, entryList.get(0).getMaxVal().compareTo(counter) > 0 ? entryList.get(0).getMaxVal() : counter, ev.name());
            }else{
                log.debug("write() -> ignoring same value=" + counter);
            }
        }else{
            String ts = uniqueTimestampService.getAsStringWithMillis();
            log.debug("write() -> NEW INSERT! start=" + ts);
            jdbcTemplate
                    .update("INSERT INTO " + TABLE_NAME + " (STARTTS, ENDTS, CHANGETS, CHARGEPOINT, EVNAME, STARTVAL, ENDVAL, MAXVAL) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                            ts, null, ts,
                            chargePoint.getNumber(), ev.name(), counter, counter, counter);
        }
    }

}
