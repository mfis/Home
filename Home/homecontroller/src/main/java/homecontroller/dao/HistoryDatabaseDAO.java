package homecontroller.dao;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import homecontroller.database.mapper.BigDecimalRowMapper;
import homecontroller.database.mapper.TimestampValuePair;
import homecontroller.database.mapper.TimestampValueRowMapper;
import homelibrary.homematic.model.Datapoint;
import homelibrary.homematic.model.Device;
import homelibrary.homematic.model.History;
import homelibrary.homematic.model.HomematicCommand;

@Repository
public class HistoryDatabaseDAO {

	private static final String VALUE = "value";

	private static final DateTimeFormatter SQL_TIMESTAMP_FORMATTER = DateTimeFormatter
			.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	private Map<HomematicCommand, List<TimestampValuePair>> map = new HashMap<HomematicCommand, List<TimestampValuePair>>();

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@PostConstruct
	@Transactional
	public void setupTables() {

		for (History history : History.values()) {
			jdbcTemplate.update("CREATE CACHED TABLE IF NOT EXISTS " + history.getCommand().buildVarName()
					+ " (TS DATETIME NOT NULL, VALUE DOUBLE NOT NULL, PRIMARY KEY (TS));");
		}
		// jdbcTemplate.update("insert into TEST_HM_2 (TS, VALUE) values
		// (CURRENT_TIMESTAMP, 22);");
	}

	public void addEntry(HomematicCommand command, TimestampValuePair pair) {
		if (!map.containsKey(command)) {
			map.put(command, new LinkedList<TimestampValuePair>());
		}
		map.get(command).add(pair);
	}

	@Transactional
	public void persistEntry(HomematicCommand command) {
		List<TimestampValuePair> toPersist = map.get(command);
		map.put(command, new LinkedList<TimestampValuePair>());

	}

	public BigDecimal readExtremValueBetween(Device device, Datapoint datapoint,
			ExtremValueType extremValueType, LocalDateTime fromDateTime, LocalDateTime untilDateTime) {

		String where = fromDateTime != null || untilDateTime != null ? " where " : "";
		String and = fromDateTime != null && untilDateTime != null ? " and " : "";

		String query = "select " + (extremValueType == ExtremValueType.MIN ? "min" : "max")
				+ "(value) as value FROM " + device.accessKeyHistorian(datapoint) + where
				+ (fromDateTime != null ? ("ts >= '" + formatTimestamp(fromDateTime) + "'") : "") + and
				+ (untilDateTime != null ? ("ts < '" + formatTimestamp(untilDateTime) + "'") : "") + ";";

		return jdbcTemplate.queryForObject(query, new Object[] {}, new BigDecimalRowMapper(VALUE));
	}

	public BigDecimal readExtremValueInTimeRange(Device device, Datapoint datapoint,
			ExtremValueType extremValueType, TimeRange timerange, LocalDateTime fromDateTime,
			LocalDateTime untilDateTime) {

		String query = "select " + (extremValueType == ExtremValueType.MIN ? "min" : "max")
				+ "(value) as value FROM " + device.accessKeyHistorian(datapoint) + " where ts >= '"
				+ formatTimestamp(fromDateTime) + "' and ts < '" + formatTimestamp(untilDateTime) + "'"
				+ " and hour(ts) " + timerange.hoursQueryString + ";";

		return jdbcTemplate.queryForObject(query, new Object[] {}, new BigDecimalRowMapper(VALUE));
	}

	public BigDecimal readFirstValueBefore(Device device, Datapoint datapoint, LocalDateTime localDateTime,
			int maxHoursReverse) {

		String query = "select value FROM " + device.accessKeyHistorian(datapoint) + " where ts <= '"
				+ formatTimestamp(localDateTime) + "' and ts > '"
				+ formatTimestamp(localDateTime.minusHours(maxHoursReverse))
				+ "' order by ts desc fetch first row only;";

		List<BigDecimal> result = jdbcTemplate.query(query, new Object[] {}, new BigDecimalRowMapper(VALUE));
		if (result.isEmpty()) {
			return null;
		} else {
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

	protected TimestampValuePair min(List<TimestampValuePair> list) {
		TimestampValuePair cmp = null;
		for (TimestampValuePair pair : list) {
			if (cmp == null || cmp.getValue().compareTo(pair.getValue()) > 0) {
				cmp = pair;
			}
		}
		return cmp;
	}

	protected TimestampValuePair max(List<TimestampValuePair> list) {
		TimestampValuePair cmp = null;
		for (TimestampValuePair pair : list) {
			if (cmp == null || cmp.getValue().compareTo(pair.getValue()) < 0) {
				cmp = pair;
			}
		}
		return cmp;
	}

	protected TimestampValuePair avg(List<TimestampValuePair> list) {
		if (list == null || list.isEmpty()) {
			return null;
		}
		BigDecimal sum = BigDecimal.ZERO;
		for (TimestampValuePair pair : list) {
			sum = sum.add(pair.getValue());
		}
		return new TimestampValuePair(timestamp, sum.divide(new BigDecimal(list.size())));
	}

	public enum ExtremValueType {
		MIN, MAX;
	}

	public enum TimeRange {
		DAY("in (11,12,13,14,15,16,17,18,19)"), NIGHT("in (0,1,2,3,4,5,6,7,8)");
		private final String hoursQueryString;

		private TimeRange(String hoursQueryString) {
			this.hoursQueryString = hoursQueryString;
		}
	}

}
