package de.fimatas.home.controller.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.fimatas.home.controller.command.HomematicCommand;
import de.fimatas.home.controller.database.mapper.BigDecimalRowMapper;
import de.fimatas.home.controller.database.mapper.LongRowMapper;
import de.fimatas.home.controller.database.mapper.StringRowMapper;
import de.fimatas.home.controller.database.mapper.TimestampValuePair;
import de.fimatas.home.controller.database.mapper.TimestampValueRowMapper;
import de.fimatas.home.controller.model.History;
import de.fimatas.home.controller.model.HistoryElement;
import de.fimatas.home.controller.model.HistoryValueType;
import de.fimatas.home.library.domain.model.TimeRange;

@Repository
public class HistoryDatabaseDAO {

    private static final String VALUE = "VAL";

    private static final DateTimeFormatter SQL_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private History history;

    private static final Log LOG = LogFactory.getLog(HistoryDatabaseDAO.class);

    private long countOnStartup = -1;

    private boolean setupIsRunning = true;

    public void completeInit(){
        setupIsRunning = false;
    }

    public long getCountOnStartup() {
        return countOnStartup;
    }

    @Transactional
    @PostConstruct
    public void createTables() {

        long completeCount = 0;
        for (HistoryElement history : history.list()) {
            var varName = history.getCommand().getCashedVarName();
            jdbcTemplate.update("CREATE CACHED TABLE IF NOT EXISTS " + varName
                + " (TS DATETIME NOT NULL, TYP CHAR(1) NOT NULL, VAL DOUBLE NOT NULL, PRIMARY KEY (TS));");
            jdbcTemplate
                .update("CREATE UNIQUE INDEX IF NOT EXISTS " + "IDX1_" + varName + " ON " + varName + " (TS, TYP);");

            String countQuery = "SELECT COUNT(TS) AS CNT FROM " + varName + ";";
            long result = jdbcTemplate.queryForObject(countQuery, new Object[] {}, new LongRowMapper("CNT"));
            completeCount += result;
        }
        countOnStartup = completeCount;
    }

    @Transactional
    public void persistEntries(Map<HomematicCommand, List<TimestampValuePair>> toInsert) {

        if (setupIsRunning) {
            throw new IllegalStateException("connat persist entries - setup is still running");
        }

        for (Entry<HomematicCommand, List<TimestampValuePair>> entry : toInsert.entrySet()) {
            String table = entry.getKey().getCashedVarName();
            for (TimestampValuePair pair : entry.getValue()) {
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
    public TimestampValuePair readExtremValueBetween(HomematicCommand command, HistoryValueType historyValueType,
            LocalDateTime fromDateTime, LocalDateTime untilDateTime) {

        String where = fromDateTime != null || untilDateTime != null ? " where " : "";
        String and = fromDateTime != null && untilDateTime != null ? " and " : "";

        String query = "select " + (historyValueType == HistoryValueType.MIN ? "min" : "max") + "(val) as val FROM "
            + command.getCashedVarName() + where
            + (fromDateTime != null ? ("ts >= '" + formatTimestamp(fromDateTime) + "'") : "") + and
            + (untilDateTime != null ? ("ts < '" + formatTimestamp(untilDateTime) + "'") : "") + ";";

        BigDecimal result = jdbcTemplate.queryForObject(query, new Object[] {}, new BigDecimalRowMapper(VALUE));
        if (result == null) {
            return null;
        } else {
            return new TimestampValuePair(null, result, historyValueType);
        }
    }

    @Transactional(readOnly = true)
    public TimestampValuePair readExtremValueInTimeRange(HomematicCommand command, HistoryValueType historyValueType,
            LocalDateTime fromDateTime, LocalDateTime untilDateTime, List<TimeRange> timeranges) {

        String query = "select " + (historyValueType == HistoryValueType.MIN ? "min" : "max") + "(val) as val FROM "
            + command.getCashedVarName() + " where ts >= '" + formatTimestamp(fromDateTime) + "' and ts < '"
            + formatTimestamp(untilDateTime) + "'" + " and hour(ts) " + TimeRange.hoursSqlQueryString(timeranges) + ";";

        BigDecimal result = jdbcTemplate.queryForObject(query, new Object[] {}, new BigDecimalRowMapper(VALUE));
        if (result == null) {
            return null;
        } else {
            return new TimestampValuePair(null, result, historyValueType);
        }
    }

    @Transactional(readOnly = true)
    public TimestampValuePair readFirstValueBefore(HomematicCommand command, LocalDateTime localDateTime, int maxHoursReverse) {

        String query =
            "select val FROM " + command.getCashedVarName() + " where ts <= '" + formatTimestamp(localDateTime) + "' and ts > '"
                + formatTimestamp(localDateTime.minusHours(maxHoursReverse)) + "' order by ts desc fetch first row only;";

        List<BigDecimal> result = jdbcTemplate.query(query, new Object[] {}, new BigDecimalRowMapper(VALUE));
        if (result.isEmpty()) {
            return null;
        } else {
            return new TimestampValuePair(null, result.get(0), HistoryValueType.SINGLE);
        }
    }

    @Transactional(readOnly = true)
    public TimestampValuePair readLatestValue(HomematicCommand command) {

        var varName = command.getCashedVarName();
        String query = "SELECT * FROM " + varName + " where ts = (select max(ts) from " + varName + ");";

        List<TimestampValuePair> result = jdbcTemplate.query(query, new Object[] {}, new TimestampValueRowMapper());
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
        return jdbcTemplate.query("select * FROM " + command.getCashedVarName() + whereClause + " order by ts asc;",
            new Object[] {}, new TimestampValueRowMapper());
    }

    private String formatTimestamp(LocalDateTime optionalFromDateTime) {
        String startTs;
        if (optionalFromDateTime == null) {
            startTs = SQL_TIMESTAMP_FORMATTER.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.systemDefault()));
        } else {
            startTs = SQL_TIMESTAMP_FORMATTER.format(optionalFromDateTime);
        }
        return startTs;
    }

}
