package homecontroller.dao;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import homecontroller.database.mapper.BigDecimalRowMapper;
import homecontroller.database.mapper.TimestampValuePair;
import homecontroller.database.mapper.TimestampValueRowMapper;
import homecontroller.model.HistoryValueType;
import homecontroller.model.TimeRange;
import homelibrary.homematic.model.History;
import homelibrary.homematic.model.HomematicCommand;

@Repository
public class HistoryDatabaseDAO {

	private static final String VALUE = "VAL";

	private static final DateTimeFormatter SQL_TIMESTAMP_FORMATTER = DateTimeFormatter
			.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@PostConstruct
	@Transactional
	public void setupTables() {

		for (History history : History.values()) {
			jdbcTemplate.update("CREATE CACHED TABLE IF NOT EXISTS " + history.getCommand().buildVarName()
					+ " (TS DATETIME NOT NULL, TYP CHAR(1) NOT NULL, VAL DOUBLE NOT NULL, PRIMARY KEY (TS));");
			jdbcTemplate.update(
					"CREATE UNIQUE INDEX IF NOT EXISTS " + "IDX1_" + history.getCommand().buildVarName()
							+ " ON " + history.getCommand().buildVarName() + " (TS, TYP);");
		}
	}

	@Transactional
	public void persistEntries(Map<HomematicCommand, List<TimestampValuePair>> toInsert) {

		for (HomematicCommand command : toInsert.keySet()) {
			String table = command.buildVarName();
			for (TimestampValuePair pair : toInsert.get(command)) {
				if (pair != null) {
					String ts = formatTimestamp(pair.getTimestamp());
					String val = pair.getValue().toString();
					String sql = "insert into " + table + " (TS, TYP, VAL) values ('" + ts + "', '"
							+ pair.getType().getDatabaseKey() + "', " + val + ");";
					jdbcTemplate.update(sql);
				}
			}
		}
	}

	@Transactional(readOnly = true)
	public TimestampValuePair readExtremValueBetween(HomematicCommand command,
			HistoryValueType historyValueType, LocalDateTime fromDateTime, LocalDateTime untilDateTime) {

		String where = fromDateTime != null || untilDateTime != null ? " where " : "";
		String and = fromDateTime != null && untilDateTime != null ? " and " : "";

		String query = "select " + (historyValueType == HistoryValueType.MIN ? "min" : "max")
				+ "(val) as val FROM " + command.buildVarName() + where
				+ (fromDateTime != null ? ("ts >= '" + formatTimestamp(fromDateTime) + "'") : "") + and
				+ (untilDateTime != null ? ("ts < '" + formatTimestamp(untilDateTime) + "'") : "") + ";";

		BigDecimal result = jdbcTemplate.queryForObject(query, new Object[] {},
				new BigDecimalRowMapper(VALUE));
		if (result == null) {
			return null;
		} else {
			return new TimestampValuePair(null, result, historyValueType);
		}
	}

	@Transactional(readOnly = true)
	public TimestampValuePair readExtremValueInTimeRange(HomematicCommand command,
			HistoryValueType historyValueType, TimeRange timerange, LocalDateTime fromDateTime,
			LocalDateTime untilDateTime) {

		String query = "select " + (historyValueType == HistoryValueType.MIN ? "min" : "max")
				+ "(val) as val FROM " + command.buildVarName() + " where ts >= '"
				+ formatTimestamp(fromDateTime) + "' and ts < '" + formatTimestamp(untilDateTime) + "'"
				+ " and hour(ts) " + timerange.getHoursSqlQueryString() + ";";

		BigDecimal result = jdbcTemplate.queryForObject(query, new Object[] {},
				new BigDecimalRowMapper(VALUE));
		if (result == null) {
			return null;
		} else {
			return new TimestampValuePair(null, result, historyValueType);
		}
	}

	@Transactional(readOnly = true)
	public TimestampValuePair readFirstValueBefore(HomematicCommand command, LocalDateTime localDateTime,
			int maxHoursReverse) {

		String query = "select val FROM " + command.buildVarName() + " where ts <= '"
				+ formatTimestamp(localDateTime) + "' and ts > '"
				+ formatTimestamp(localDateTime.minusHours(maxHoursReverse))
				+ "' order by ts desc fetch first row only;";

		List<BigDecimal> result = jdbcTemplate.query(query, new Object[] {}, new BigDecimalRowMapper(VALUE));
		if (result.isEmpty()) {
			return null;
		} else {
			return new TimestampValuePair(null, result.get(0), HistoryValueType.SINGLE);
		}
	}

	@Transactional(readOnly = true)
	public TimestampValuePair readLatestValue(HomematicCommand command) {

		String query = "SELECT * FROM " + command.buildVarName() + " where ts = (select max(ts) from "
				+ command.buildVarName() + ");";

		return jdbcTemplate.queryForObject(query, new Object[] {}, new TimestampValueRowMapper());
	}

	@Transactional(readOnly = true)
	public List<TimestampValuePair> readValues(HomematicCommand command, LocalDateTime optionalFromDateTime) {

		String whereClause = StringUtils.EMPTY;
		if (optionalFromDateTime != null) {
			String startTs = formatTimestamp(optionalFromDateTime);
			whereClause = " where ts > '" + startTs + "'";
		}
		return jdbcTemplate.query(
				"select ts, val FROM " + command.buildVarName() + whereClause + " order by ts asc;",
				new Object[] {}, new TimestampValueRowMapper());
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
