package de.fimatas.home.controller.dao;

import de.fimatas.home.controller.database.mapper.*;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Repository
@CommonsLog
public class BackupRestoreDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Log LOG = LogFactory.getLog(BackupRestoreDAO.class);

    @Transactional
    public void backupDatabase(String filename) {

        List<String> scriptQueries = jdbcTemplate.query("SCRIPT;", new StringRowMapper("SCRIPT"));

        try (FileOutputStream fos = new FileOutputStream(filename); ZipOutputStream zipOut = new ZipOutputStream(fos)) {

            ZipEntry zipEntry = new ZipEntry(new File(filename).getName().replace(".zip", ""));
            zipOut.putNextEntry(zipEntry);

            scriptQueries.forEach(e -> {
                String line = StringUtils.trimToEmpty(e);
                if (isNotCreateOrAlterStatement(line)) {
                    byte[] cmdBytes = line.concat("\n").getBytes(StandardCharsets.UTF_8);
                    try {
                        zipOut.write(cmdBytes, 0, cmdBytes.length);
                    } catch (IOException ioe) {
                        throw new IllegalArgumentException("error writing zio out", ioe);
                    }
                }
            });
        } catch (Exception e) {
            LOG.error("error processing backup file:", e);
        }
    }

    @Transactional
    public void restoreDatabase(String filename) throws IOException {

        final StringTokenizer tokenizer = getStringTokenizer(filename);
        int statemantsExecuted = 0;
        while (tokenizer.hasMoreTokens()) {
            var actualStatement = tokenizer.nextToken().trim();
            var tableExisting = true;
            if(actualStatement.startsWith("INSERT")){
                var tableName = StringUtils.substringBetween(actualStatement, "INTO", "VALUES").trim();
                try {
                    jdbcTemplate.queryForMap("select count(*) FROM " + tableName + ";");
                }catch (BadSqlGrammarException e){
                    tableExisting = false;
                    log.warn("DB-Import - Table not existing: " + tableName);
                }
            }
            if(tableExisting){
                jdbcTemplate.update(actualStatement + ";");
                statemantsExecuted++;
            }
        }
        log.info("DB-Import - Statemens executed: " + statemantsExecuted);
    }

    private static StringTokenizer getStringTokenizer(String filename) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(filename))) {
            byte[] buffer = new byte[1024];
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    String s = new String(buffer, 0, len, StandardCharsets.UTF_8);
                    sb.append(s);
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }

        return new StringTokenizer(sb.toString(), ";");
    }

    private boolean isNotCreateOrAlterStatement(String line) {
        return StringUtils.isNotBlank(line) && !StringUtils.startsWithIgnoreCase(line, "CREATE ")
                && !StringUtils.startsWithIgnoreCase(line, "ALTER ") && !StringUtils.startsWithIgnoreCase(line, "-- ");
    }
}
