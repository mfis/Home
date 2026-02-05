package de.fimatas.home.controller.dao;

import lombok.SneakyThrows;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringTokenizer;

@Repository
public class BackupRestoreDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${backup.zip.password}")
    private String password;

    private static final Log LOG = LogFactory.getLog(BackupRestoreDAO.class);

    @Transactional
    @SneakyThrows
    public void backupDatabase(File outputFile)  {

        List<String> scriptQueries = jdbcTemplate.query("SCRIPT;", new SingleColumnRowMapper<>(String.class));

        try (SevenZOutputFile sevenZOutput = new SevenZOutputFile(outputFile, password.toCharArray())) {

            SevenZArchiveEntry entry = sevenZOutput.createArchiveEntry(new File("backup.sql"), "backup.sql");
            sevenZOutput.putArchiveEntry(entry);

            for (String line : scriptQueries) {
                String trimmedLine = StringUtils.trimToEmpty(line);
                if (isNotCreateOrAlterStatement(trimmedLine)) {
                    byte[] data = trimmedLine.concat("\n").getBytes(StandardCharsets.UTF_8);
                    sevenZOutput.write(data);
                }
            }
            sevenZOutput.closeArchiveEntry();
        } catch (IOException e) {
            LOG.error("error processing backup file:", e);
            throw new RuntimeException("Backup failed", e);
        }
    }

    @Transactional
    public void restoreDatabase(String filename) throws IOException {
        final StringTokenizer tokenizer = getStringTokenizer(filename);
        int statementsExecuted = 0;

        while (tokenizer.hasMoreTokens()) {
            var actualStatement = tokenizer.nextToken().trim();
            if (StringUtils.isBlank(actualStatement)) continue;

            var tableExisting = true;
            if (actualStatement.startsWith("INSERT")) {
                var tableName = StringUtils.substringBetween(actualStatement, "INTO", "VALUES").trim();
                try {
                    jdbcTemplate.queryForMap("select count(*) FROM " + tableName + " LIMIT 1;");
                } catch (BadSqlGrammarException e) {
                    tableExisting = false;
                    LOG.warn("DB-Import - Table not existing: " + tableName);
                }
            }
            if (tableExisting) {
                jdbcTemplate.update(actualStatement);
                statementsExecuted++;
            }
        }
        LOG.info("DB-Import - Statements executed: " + statementsExecuted);
    }

    private StringTokenizer getStringTokenizer(String filename) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (SevenZFile sevenZFile = SevenZFile.builder()
                .setFile(new File(filename))
                .setPassword(password.toCharArray())
                .get()) {

            SevenZArchiveEntry entry = sevenZFile.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    byte[] content = new byte[8192];
                    int len;
                    while ((len = sevenZFile.read(content)) > 0) {
                        String s = new String(content, 0, len, StandardCharsets.UTF_8);
                        sb.append(s);
                    }
                }
                entry = sevenZFile.getNextEntry();
            }
        }

        return new StringTokenizer(sb.toString(), ";");
    }

    private boolean isNotCreateOrAlterStatement(String line) {
        return StringUtils.isNotBlank(line) && !Strings.CI.startsWith(line, "CREATE ")
                && !Strings.CI.startsWith(line, "ALTER ") && !Strings.CI.startsWith(line, "-- ");
    }
}