package homecontroller.database.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.springframework.jdbc.core.RowMapper;

public class TimestampRowMapper implements RowMapper<Timestamp> {

	private String colName;

	public TimestampRowMapper(String colName) {
		this.colName = colName;
	}

	@Override
	public Timestamp mapRow(ResultSet rs, int rowNum) throws SQLException {
		return rs.getTimestamp(colName);
	}

}
