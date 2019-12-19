package homecontroller.dao;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import homecontroller.database.mapper.BigDecimalRowMapper;
import homecontroller.database.mapper.LongRowMapper;
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
	private JdbcTemplate jdbcTemplateHistory;

	@Autowired
	private Environment env;

	private boolean setupIsRunning = true;

	private static final Log LOG = LogFactory.getLog(HistoryDatabaseDAO.class);

	@PostConstruct
	@Transactional
	public void setupTables() {

		long completeCount = 0;

		for (History history : History.values()) {
			jdbcTemplateHistory.update("CREATE CACHED TABLE IF NOT EXISTS "
					+ history.getCommand().buildVarName()
					+ " (TS DATETIME NOT NULL, TYP CHAR(1) NOT NULL, VAL DOUBLE NOT NULL, PRIMARY KEY (TS));");
			jdbcTemplateHistory.update(
					"CREATE UNIQUE INDEX IF NOT EXISTS " + "IDX1_" + history.getCommand().buildVarName()
							+ " ON " + history.getCommand().buildVarName() + " (TS, TYP);");

			String countQuery = "SELECT COUNT(TS) AS CNT FROM " + history.getCommand().buildVarName() + ";";
			long result = jdbcTemplateHistory.queryForObject(countQuery, new Object[] {},
					new LongRowMapper("CNT"));
			completeCount += result;
		}

		if (completeCount == 0) {
			File importFile = new File(lookupPath() + "import.sql");
			if (importFile.exists()) {
				LOG.info("auto-import database from: " + importFile.getAbsolutePath());
				restoreDatabase(importFile.getAbsolutePath());
				importFile.renameTo(new File(importFile.getAbsolutePath() + ".done")); // NOSONAR
			}
		}

		setupIsRunning = false;
	}

	@Transactional
	public void backupDatabase(String filename) {

		String sql = "SCRIPT TO '" + filename + "';";
		jdbcTemplateHistory.execute(sql);

		try {
			String sqlFile = FileUtils.readFileToString(new File(filename), StandardCharsets.UTF_8);
			String[] statements = StringUtils.split(sqlFile, ';');
			StringBuilder sb = new StringBuilder();
			Arrays.asList(statements).stream().forEach(e -> {
				String line = StringUtils.trimToEmpty(e);
				if (StringUtils.isNotBlank(line) && !StringUtils.startsWithIgnoreCase(line, "CREATE ")
						&& !StringUtils.startsWithIgnoreCase(line, "ALTER ")
						&& !StringUtils.startsWithIgnoreCase(line, "-- ")) {
					sb.append(line + ";\n");
				}
			});
			FileUtils.write(new File(filename), sb.toString(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			LOG.error("error processing backup file:", e);
		}
	}

	@Transactional
	public void restoreDatabase(String filename) {

		String sql = "RUNSCRIPT FROM '" + filename + "';";
		jdbcTemplateHistory.execute(sql);
	}

	@Transactional
	public void persistEntries(Map<HomematicCommand, List<TimestampValuePair>> toInsert) {

		if (setupIsRunning) {
			throw new IllegalStateException("connat persist entries - setup is still running");
		}

		for (Entry<HomematicCommand, List<TimestampValuePair>> entry : toInsert.entrySet()) {
			String table = entry.getKey().buildVarName();
			for (TimestampValuePair pair : entry.getValue()) {
				if (pair != null) {
					String ts = formatTimestamp(pair.getTimestamp());
					String val = pair.getValue().toString();
					String sql = "insert into " + table + " (TS, TYP, VAL) values ('" + ts + "', '"
							+ pair.getType().getDatabaseKey() + "', " + val + ");";
					jdbcTemplateHistory.update(sql);
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

		BigDecimal result = jdbcTemplateHistory.queryForObject(query, new Object[] {},
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

		BigDecimal result = jdbcTemplateHistory.queryForObject(query, new Object[] {},
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

		List<BigDecimal> result = jdbcTemplateHistory.query(query, new Object[] {},
				new BigDecimalRowMapper(VALUE));
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

		List<TimestampValuePair> result = jdbcTemplateHistory.query(query, new Object[] {},
				new TimestampValueRowMapper());
		if (result.isEmpty()) {
			return null;
		} else {
			return result.get(0);
		}
	}

	@Transactional(readOnly = true)
	public List<TimestampValuePair> readValues(HomematicCommand command, LocalDateTime optionalFromDateTime) {

		String whereClause = StringUtils.EMPTY;
		if (optionalFromDateTime != null) {
			String startTs = formatTimestamp(optionalFromDateTime);
			whereClause = " where ts > '" + startTs + "'";
		}
		return jdbcTemplateHistory.query(
				"select * FROM " + command.buildVarName() + whereClause + " order by ts asc;",
				new Object[] {}, new TimestampValueRowMapper());
	}

	public String lookupPath() {

		String path = env.getProperty("backup.database.path");
		if (!path.endsWith("/")) {
			path = path + "/";
		}
		return path;
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
