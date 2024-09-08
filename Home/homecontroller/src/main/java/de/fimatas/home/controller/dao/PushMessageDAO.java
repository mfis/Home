package de.fimatas.home.controller.dao;

import de.fimatas.home.controller.database.mapper.PushMessageRowMapper;
import de.fimatas.home.controller.service.UniqueTimestampService;
import de.fimatas.home.library.domain.model.PushMessage;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static de.fimatas.home.controller.dao.DaoUtils.cleanSqlValue;

@Component
@CommonsLog
public class PushMessageDAO {

    private final String TABLE_NAME = "PUSHMESSAGE";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UniqueTimestampService uniqueTimestampService;

    private boolean setupIsRunning = true;

    public void completeInit(){
        setupIsRunning = false;
    }

    @PostConstruct
    @Transactional(propagation = Propagation.REQUIRED)
    public void createTables() {

        jdbcTemplate.update("CREATE CACHED TABLE IF NOT EXISTS " + TABLE_NAME
                + " (TS DATETIME NOT NULL, USERNAME VARCHAR(16) NOT NULL, TITLE VARCHAR(64) NOT NULL, TEXTMSG VARCHAR(350) NOT NULL, PRIMARY KEY (TS));");
        jdbcTemplate
                .update("CREATE UNIQUE INDEX IF NOT EXISTS " + "IDX1_" + TABLE_NAME + " ON " + TABLE_NAME + " (TS, USERNAME);");
    }

    @Transactional(readOnly = true)
    public List<PushMessage> readMessages(){

        String query =
                "select * FROM " + TABLE_NAME + " ORDER BY TS DESC;";

        return jdbcTemplate.query(query, new PushMessageRowMapper());
    }

    @Transactional(readOnly = true)
    public List<PushMessage> readMessagesFromLastThreeSeconds(){

        String query =
                "select * FROM " + TABLE_NAME + " WHERE TS > ? ORDER BY TS DESC;";

        return jdbcTemplate.query(query, new PushMessageRowMapper(), UniqueTimestampService.getAsStringWithMillis(LocalDateTime.now().minusSeconds(3)));
    }

    @Transactional
    public PushMessage writeMessage(LocalDateTime ts, String user, String title, String textMessage){

        if (setupIsRunning) {
            throw new IllegalStateException("setup is still running");
        }

        final PushMessage message = new PushMessage(ts.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), cleanSqlValue(user), cleanSqlValue(title), cleanSqlValue(textMessage));
        jdbcTemplate
                .update("INSERT INTO " + TABLE_NAME + " (TS, USERNAME, TITLE, TEXTMSG) VALUES (?, ?, ?, ?)",
                        UniqueTimestampService.getAsStringWithMillis(ts), message.getUsername(), message.getTitle(), message.getTextMessage());
        return message;
    }

    @Transactional
    public void deleteMessagesOlderAsNDays(int days){

        if (setupIsRunning) {
            throw new IllegalStateException("setup is still running");
        }

        final String ts = UniqueTimestampService.getAsStringWithMillis(uniqueTimestampService.get().minusDays(days));
        jdbcTemplate
                .update("DELETE FROM " + TABLE_NAME + " WHERE TS < ?", ts);
    }
}
