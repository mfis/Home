package de.fimatas.home.controller.dao;

import de.fimatas.home.controller.command.AbstractCommand;
import de.fimatas.home.controller.command.HomematicCommand;
import de.fimatas.home.controller.database.mapper.BigDecimalRowMapper;
import de.fimatas.home.controller.database.mapper.LongRowMapper;
import de.fimatas.home.controller.database.mapper.TimestampValuePair;
import de.fimatas.home.controller.database.mapper.TimestampValueRowMapper;
import de.fimatas.home.controller.model.History;
import de.fimatas.home.controller.model.HistoryElement;
import de.fimatas.home.controller.model.HistoryValueType;
import de.fimatas.home.library.domain.model.TimeRange;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@SuppressWarnings("SqlSourceToSinkFlow")
@Repository(ApplicationDatabaseDAO.APPLICATION_DATABASE_DAO)
@CommonsLog
public class ApplicationDatabaseDAO {

    public static final String APPLICATION_DATABASE_DAO = "applicationDatabaseDAO";

    private static final String VALUE = "VAL";

    private static final DateTimeFormatter SQL_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private History history;

    @Value("${spring.datasource.url}")
    private String dbUrl;
    @Value("${spring.datasource.username}")
    private String dbUser;
    @Value("${spring.datasource.password:}")
    private String password;

    @Getter
    private boolean databaseIsEmpty = false;

    @Transactional
    @PostConstruct
    public void postConstruct() {
        postConstructCheckDatabasePassword();
        postConstructCheckDatabaseTables();
    }

    @SneakyThrows
    private void postConstructCheckDatabasePassword() {

        if(StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("database password is empty!");
        }

        try {
            jdbcTemplate.execute((Connection con) -> con.getMetaData().getURL());
        } catch (Exception e) {
            Throwable cause = e;
            while (cause != null) {
                if (cause instanceof org.h2.jdbc.JdbcSQLInvalidAuthorizationSpecException) {
                    try (Connection fallbackCon = DriverManager.getConnection(dbUrl, dbUser, "");
                        Statement stmt = fallbackCon.createStatement()) {
                        stmt.execute("ALTER USER " + dbUser + " SET PASSWORD '" + password + "'");
                        log.info("database password is now set!");
                    }
                    return;
                }
                cause = cause.getCause();
            }
            throw e;
        }
    }

    private void postConstructCheckDatabaseTables() {

        long completeCount = 0;
        // check for existence of necessary tables
        for (HistoryElement history : history.list()) {
            var varName = history.getCommand().id();
            boolean tableExists = Boolean.TRUE.equals(jdbcTemplate.execute((Connection con) -> {
                DatabaseMetaData metaData = con.getMetaData();
                try (ResultSet rs = metaData.getTables(null, null, varName.toUpperCase(), new String[] {"TABLE"})) {
                    return rs.next();
                }
            }));
            if (tableExists) {
                String countQuery = "SELECT COUNT(TS) AS CNT FROM " + varName + ";";
                Long result = jdbcTemplate.queryForObject(countQuery, new LongRowMapper("CNT"), new Object[] {});
                completeCount += (result == null ? 0 : result);
            } else {
                jdbcTemplate.update("CREATE CACHED TABLE " + varName
                        + " (TS TIMESTAMP NOT NULL, TYP CHAR(1) NOT NULL, VAL DOUBLE NOT NULL, PRIMARY KEY (TS));");
                log.warn("database table was NOT existent: " + varName);
            }
        }
        log.info("database row count: " + completeCount);
        databaseIsEmpty = completeCount == 0;
    }

    @Transactional
    public void persistEntries(Map<AbstractCommand, List<TimestampValuePair>> toInsert) {

        for (Entry<AbstractCommand, List<TimestampValuePair>> entry : toInsert.entrySet()) {
            String table = entry.getKey().id();
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
            + command.id() + where
            + (fromDateTime != null ? ("ts >= '" + formatTimestamp(fromDateTime) + "'") : "") + and
            + (untilDateTime != null ? ("ts < '" + formatTimestamp(untilDateTime) + "'") : "") + ";";

        BigDecimal result = jdbcTemplate.queryForObject(query, new BigDecimalRowMapper(VALUE), new Object[] {});
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
            + command.id() + " where ts >= '" + formatTimestamp(fromDateTime) + "' and ts < '"
            + formatTimestamp(untilDateTime) + "'" + " and hour(ts) " + TimeRange.hoursSqlQueryString(timeranges) + ";";

        BigDecimal result = jdbcTemplate.queryForObject(query, new BigDecimalRowMapper(VALUE), new Object[] {});
        if (result == null) {
            return null;
        } else {
            return new TimestampValuePair(null, result, historyValueType);
        }
    }

    @Transactional(readOnly = true)
    public TimestampValuePair readFirstValueBefore(HomematicCommand command, LocalDateTime localDateTime, int maxHoursReverse) {

        String query =
            "select val FROM " + command.id() + " where ts <= '" + formatTimestamp(localDateTime) + "' and ts > '"
                + formatTimestamp(localDateTime.minusHours(maxHoursReverse)) + "' order by ts desc fetch first row only;";

        List<BigDecimal> result = jdbcTemplate.query(query, new BigDecimalRowMapper(VALUE), new Object[] {});
        if (result.isEmpty()) {
            return null;
        } else {
            return new TimestampValuePair(null, result.get(0), HistoryValueType.SINGLE);
        }
    }

    @Transactional(readOnly = true)
    public TimestampValuePair readLatestValue(AbstractCommand command) {

        var varName = command.id();
        String query = "SELECT * FROM " + varName + " where ts = (select max(ts) from " + varName + ");";

        List<TimestampValuePair> result = jdbcTemplate.query(query, new TimestampValueRowMapper(), new Object[] {});
        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0);
        }
    }

    @Transactional(readOnly = true)
    public List<TimestampValuePair> readValues(AbstractCommand command, LocalDateTime optionalFromDateTime) {

        String whereClause = StringUtils.EMPTY;
        if (optionalFromDateTime != null) {
            String startTs = formatTimestamp(optionalFromDateTime);
            whereClause = " where ts > '" + startTs + "'";
        }
        return jdbcTemplate.query("select * FROM " + command.id() + whereClause + " order by ts;",
                new TimestampValueRowMapper(), new Object[] {});
    }

    private String formatTimestamp(LocalDateTime optionalFromDateTime) {
        String startTs;
        //noinspection ReplaceNullCheck
        if (optionalFromDateTime == null) {
            startTs = SQL_TIMESTAMP_FORMATTER.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.systemDefault()));
        } else {
            startTs = SQL_TIMESTAMP_FORMATTER.format(optionalFromDateTime);
        }
        return startTs;
    }

}
