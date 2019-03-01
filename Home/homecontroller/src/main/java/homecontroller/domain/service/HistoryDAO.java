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
import homecontroller.domain.model.Heating;
import homecontroller.domain.model.HomematicConstants;

@Component
public class HistoryDAO {

	private static final DateTimeFormatter SQL_TIMESTAMP_FORMATTER = DateTimeFormatter
			.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public long minutesSinceLastHeatingBoost(Heating heating) {

		Timestamp timestamp = jdbcTemplate.queryForObject("select max(ts) as time FROM "
				+ heating.getDevice().accessKeyHistorian(Datapoint.CONTROL_MODE) + " where value = ?;",
				new Object[] { HomematicConstants.HEATING_CONTROL_MODE_BOOST.intValue() },
				new TimestampRowMapper("time"));

		Duration timeElapsed = Duration.between(Instant.ofEpochMilli(timestamp.getTime()), Instant.now());
		return timeElapsed.toMinutes();
	}
	
	public BigDecimal readExtremValueBetween(Device device, Datapoint datapoint, ExtremValueType extremValueType, LocalDateTime fromDateTime, LocalDateTime untilDateTime) {

		String WHERE = fromDateTime!=null || untilDateTime!=null?" where ":"";
		String AND = fromDateTime!=null && untilDateTime!=null?" and ":"";
		
		String query = "select " + (extremValueType==ExtremValueType.MIN?"min":"max") + "(value) as value FROM " + device.accessKeyHistorian(datapoint)
		+ WHERE + (fromDateTime!=null?("ts >= '" + formatTimestamp(fromDateTime) + "'"):"") + 
		AND + (untilDateTime!=null?("ts < '" + formatTimestamp(untilDateTime) + "'"):"") + ";";
		
		return jdbcTemplate
				.queryForObject(
						query,
						new Object[] {}, new BigDecimalRowMapper("value"));
	}
	
	public BigDecimal readExtremValueInTimeRange(Device device, Datapoint datapoint, ExtremValueType extremValueType, TimeRange timerange ,LocalDateTime fromDateTime, LocalDateTime untilDateTime) {

		String query = "select " + (extremValueType==ExtremValueType.MIN?"min":"max") + "(value) as value FROM " + device.accessKeyHistorian(datapoint)
		+ " where ts >= '" + formatTimestamp(fromDateTime) + "' and ts < '" + formatTimestamp(untilDateTime) + "'" + 
		" and hour(ts) " + timerange.hoursQueryString + ";";
		
		return jdbcTemplate
				.queryForObject(
						query,
						new Object[] {}, new BigDecimalRowMapper("value"));
	}
	
	public BigDecimal readFirstValueBefore(Device device, Datapoint datapoint, LocalDateTime localDateTime, int maxHoursReverse) {

		String query = "select value FROM " + device.accessKeyHistorian(datapoint)
		+ " where ts <= '" + formatTimestamp(localDateTime) + "' and ts > '" + formatTimestamp(localDateTime.minusHours(maxHoursReverse)) + "' order by ts desc fetch first row only;";
		
		List<BigDecimal> result = jdbcTemplate
		.query(
				query,
				new Object[] {}, new BigDecimalRowMapper("value"));
		if(result==null || result.size()==0) {
			return null;
		}else {
		return result.get(0);
		}
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
	
	public enum ExtremValueType{
		MIN, MAX;
	}
	
	public enum TimeRange{
		DAY("not in (0,1,2,3,4,5)"), NIGHT("in (0,1,2,3,4,5)");
		private final String hoursQueryString;
		private TimeRange(String hoursQueryString) {
			this.hoursQueryString = hoursQueryString;
		}
	}

}
