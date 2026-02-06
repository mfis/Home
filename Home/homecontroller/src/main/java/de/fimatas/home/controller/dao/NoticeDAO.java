package de.fimatas.home.controller.dao;
import de.fimatas.home.controller.database.mapper.NoticeRowMapper;
import de.fimatas.home.controller.service.UniqueTimestampService;
import de.fimatas.home.library.model.Notice;
import jakarta.annotation.PostConstruct;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.List;

import static de.fimatas.home.controller.dao.DaoUtils.cleanSqlValue;

@Component
@CommonsLog
public class NoticeDAO {

    private final String TABLE_NAME = "NOTICE";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UniqueTimestampService uniqueTimestampService;

    @PostConstruct
    @Transactional(propagation = Propagation.REQUIRED)
    public void createTables() {

        jdbcTemplate.update("CREATE CACHED TABLE IF NOT EXISTS " + TABLE_NAME
                + " (ID CHAR(36) NOT NULL, VERSION INT NOT NULL, EDITED TIMESTAMP DEFAULT CURRENT_TIMESTAMP, USERNAME VARCHAR(64), MULTIUSER BOOLEAN NOT NULL, LOGICALDELETED BOOLEAN NOT NULL, TEXT VARCHAR(5000000), PRIMARY KEY (ID, VERSION));");
    }

    @Transactional(readOnly = true)
    public List<Notice> getLatestNotices() {

        String query = "SELECT n.* FROM " + TABLE_NAME + " n " +
                "WHERE LOGICALDELETED = FALSE AND n.VERSION = (SELECT MAX(sub.VERSION) FROM " + TABLE_NAME + " sub WHERE sub.ID = n.ID)";

        return jdbcTemplate.query(query, new NoticeRowMapper());
    }

    @Transactional
    public long createNew(String id, String username, boolean multiUser, String text) {

        long version = 0;
        jdbcTemplate.update(
                "INSERT INTO " + TABLE_NAME + " (ID, VERSION, EDITED, USERNAME, MULTIUSER, LOGICALDELETED, TEXT) VALUES (?, ?, ?, ?, ?, ?, ?)",
                cleanSqlValue(id),
                version,
                uniqueTimestampService.getAsStringWithMillis(),
                cleanSqlValue(username),
                multiUser,
                false,
                cleanSqlValue(text)
        );
        return version;
    }

    @Transactional
    public long modify(String id, String username, boolean multiUser, String text) {

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO " + TABLE_NAME + " (ID, VERSION, EDITED, USERNAME, MULTIUSER, LOGICALDELETED, TEXT) " +
                            "SELECT ID, MAX(VERSION) + 1, ?, ?, ?, ?, ? " +
                            "FROM " + TABLE_NAME + " WHERE ID = ? GROUP BY ID",
                    new String[] { "VERSION" }
            );

            ps.setString(1, uniqueTimestampService.getAsStringWithMillis());
            ps.setString(2, cleanSqlValue(username));
            ps.setBoolean(3, multiUser);
            ps.setBoolean(4, false);
            ps.setString(5, text);
            ps.setString(6, id);

            return ps;
        }, keyHolder);

        Number newVersion = keyHolder.getKey();
        if(newVersion == null) {
            throw new DataAccessResourceFailureException("New version number not found");
        }
        physicalDeleteOldVersions(id);
        return newVersion.longValue();
    }

    private void physicalDeleteOldVersions(String id) {

        String deleteSql = "DELETE FROM " + TABLE_NAME + " WHERE ID = ? " +
                "AND VERSION NOT IN (" +
                "SELECT VERSION FROM " + TABLE_NAME + " WHERE ID = ? ORDER BY VERSION DESC LIMIT 4)";

        jdbcTemplate.update(deleteSql, cleanSqlValue(id), cleanSqlValue(id));
    }
}