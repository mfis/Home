package de.fimatas.home.controller.database.mapper;

import de.fimatas.home.controller.model.EvChargeDatabaseEntry;
import de.fimatas.home.library.domain.model.ElectricVehicle;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class EvChargingMapper implements RowMapper<EvChargeDatabaseEntry> {

    @Override
    public EvChargeDatabaseEntry mapRow(ResultSet rs, int rowNum) throws SQLException {

        var entry = new EvChargeDatabaseEntry();
        entry.setStartTS(LocalDateTime.ofInstant(Instant.ofEpochMilli(rs.getTimestamp("STARTTS").getTime()), ZoneId.systemDefault()));
        entry.setEndTS(rs.getTimestamp("ENDTS")==null?null:
                LocalDateTime.ofInstant(Instant.ofEpochMilli(rs.getTimestamp("ENDTS").getTime()), ZoneId.systemDefault()));
        entry.setChargepoint(rs.getInt("CHARGEPOINT"));
        entry.setElectricVehicle(ElectricVehicle.valueOf(rs.getString("EVNAME")));
        entry.setStartVal(rs.getBigDecimal("STARTVAL"));
        entry.setEndVal(rs.getBigDecimal("ENDVAL"));
        entry.setMaxVal(rs.getBigDecimal("MAXVAL"));
        return entry;
    }

}
