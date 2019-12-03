package homecontroller.database.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.jdbc.core.RowMapper;

public class TimestampValueRowMapper implements RowMapper<TimestampValuePair> {

	@Override
	public TimestampValuePair mapRow(ResultSet rs, int rowNum) throws SQLException {
		LocalDateTime dateTime = LocalDateTime
				.ofInstant(Instant.ofEpochMilli(rs.getTimestamp("ts").getTime()), ZoneId.systemDefault());
		return new TimestampValuePair(dateTime, rs.getBigDecimal("value"));
	}

}
