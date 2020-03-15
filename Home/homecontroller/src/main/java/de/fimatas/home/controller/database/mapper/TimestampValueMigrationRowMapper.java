package de.fimatas.home.controller.database.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.jdbc.core.RowMapper;

import de.fimatas.home.controller.model.HistoryValueType;

public class TimestampValueMigrationRowMapper implements RowMapper<TimestampValuePair> {

	@Override
	public TimestampValuePair mapRow(ResultSet rs, int rowNum) throws SQLException {
		LocalDateTime dateTime = LocalDateTime
				.ofInstant(Instant.ofEpochMilli(rs.getTimestamp("TS").getTime()), ZoneId.systemDefault());
		return new TimestampValuePair(dateTime, rs.getBigDecimal("VALUE"), HistoryValueType.SINGLE);
	}

}
