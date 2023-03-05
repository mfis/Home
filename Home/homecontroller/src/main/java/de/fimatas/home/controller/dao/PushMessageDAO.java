package de.fimatas.home.controller.dao;

import de.fimatas.home.controller.database.mapper.PushMessageRowMapper;
import de.fimatas.home.controller.model.PushMessage;
import de.fimatas.home.controller.service.UniqueTimestampService;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.PostConstruct;
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

    @PostConstruct
    @Transactional(propagation = Propagation.REQUIRED)
    public void createTables() {

        jdbcTemplate.update("CREATE CACHED TABLE IF NOT EXISTS " + TABLE_NAME
                + " (TS DATETIME NOT NULL, USERNAME VARCHAR(16) NOT NULL, TITLE VARCHAR(64) NOT NULL, TEXTMSG VARCHAR(350) NOT NULL, PRIMARY KEY (TS));");
        jdbcTemplate
                .update("CREATE UNIQUE INDEX IF NOT EXISTS " + "IDX1_" + TABLE_NAME + " ON " + TABLE_NAME + " (TS, USERNAME);");
    }

    @Transactional(readOnly = true)
    public List<PushMessage> readMessages(String user){

        String query =
                "select * FROM " + TABLE_NAME + " where USERNAME = ? ORDER BY TS DESC;";

        return jdbcTemplate.query(query, new String[]{user}, new PushMessageRowMapper());
    }

    @Transactional
    public void writeMessage(String user, String title, String textMessage){
        jdbcTemplate
                .update("INSERT INTO " + TABLE_NAME + " (TS, USERNAME, TITLE, TEXTMSG) VALUES (?, ?, ?, ?)",
                        uniqueTimestampService.getAsStringWithMillis(), cleanSqlValue(user), cleanSqlValue(title), cleanSqlValue(textMessage));
    }

    @Transactional
    public void deleteMessagesOlderAsNDays(int days){
        final String millis = UniqueTimestampService.getAsStringWithMillis(uniqueTimestampService.get().minusDays(days));
        jdbcTemplate
                .update("DELETE FROM " + TABLE_NAME + " WHERE TS < ?", millis);
    }
}
