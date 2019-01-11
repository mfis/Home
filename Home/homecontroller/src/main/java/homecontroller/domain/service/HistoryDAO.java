package homecontroller.domain.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import homecontroller.database.mapper.BigDecimalRowMapper;
import homecontroller.database.mapper.TimestampRowMapper;
import homecontroller.database.mapper.TimestampValuePair;
import homecontroller.database.mapper.TimestampValueRowMapper;
import homecontroller.domain.model.Datapoint;
import homecontroller.domain.model.Device;
import homecontroller.domain.model.HeatingModel;
import homecontroller.domain.model.HomematicConstants;

@Component
public class HistoryDAO {

	private static final DateTimeFormatter SQL_TIMESTAMP_FORMATTER = DateTimeFormatter
			.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public long minutesSinceLastHeatingBoost(HeatingModel heating) {

		Timestamp timestamp = jdbcTemplate.queryForObject("select max(ts) as time FROM "
				+ heating.getDeviceHeating().accessKeyHistorian(Datapoint.CONTROL_MODE) + " where value = ?;",
				new Object[] { HomematicConstants.HEATING_CONTROL_MODE_BOOST.intValue() },
				new TimestampRowMapper("time"));

		Duration timeElapsed = Duration.between(Instant.ofEpochMilli(timestamp.getTime()), Instant.now());
		return timeElapsed.toMinutes();
	}

	public BigDecimal readMaxValue(Device device, Datapoint datapoint, LocalDateTime optionalFromDateTime) {

		String startTs = formatTimestamp(optionalFromDateTime);
		return jdbcTemplate
				.queryForObject(
						"select max(value) as value FROM " + device.accessKeyHistorian(datapoint)
								+ " where ts > '" + startTs + "';",
						new Object[] {}, new BigDecimalRowMapper("value"));
	}

	public List<TimestampValuePair> readValues(Device device, Datapoint datapoint,
			LocalDateTime optionalFromDateTime) {

		String startTs = formatTimestamp(optionalFromDateTime);
		return jdbcTemplate.query("select ts, value FROM " + device.accessKeyHistorian(datapoint)
				+ " where ts > '" + startTs + "' order by ts asc;", new Object[] {},
				new TimestampValueRowMapper());
	}

	private String formatTimestamp(LocalDateTime optionalFromDateTime) {
		String startTs;
		if (optionalFromDateTime == null) {
			startTs = SQL_TIMESTAMP_FORMATTER
					.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.systemDefault()));
		} else {
			startTs = SQL_TIMESTAMP_FORMATTER.format(optionalFromDateTime);
		}
		return startTs;
	}

}
