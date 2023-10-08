package de.fimatas.home.controller.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import de.fimatas.home.controller.dao.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import de.fimatas.home.library.domain.model.BackupFile;

@Component
/*
    Restore Database:
    - stop homecontroller
    - stop homeclient
    - rename last backup to 'import.sql.zip'
    - delete 'homehistory.mv.db' and 'homehistory.trace.db'
    - deploy or start homecontroller
    - wait for finish restoring
    - start homeclient
 */
public class BackupService {

    @Autowired
    private BackblazeBackupAPI backupAPI;

    @Autowired
    private HistoryDatabaseDAO historyDatabaseDAO;

    @Autowired
    private EvChargingDAO evChargingDAO;

    @Autowired
    private PushMessageDAO pushMessageDAO;

    @Autowired
    private StateHandlerDAO stateHandlerDAO;

    @Autowired
    private BackupRestoreDAO backupRestoreDAO;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private Environment env;

    private static final DateTimeFormatter BACKUP_DAILY_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter BACKUP_ADHOC_TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd__HH-mm_ss_SSS");

    private static final Log LOG = LogFactory.getLog(BackupService.class);

    private int restoreRunCounter = 0;

    @PostConstruct
    public void noRestore() {

        long completeCount = historyDatabaseDAO.getCountOnStartup();

        if(completeCount == -1) {
            LOG.warn("!! COULD NOT CHECK DATABASE ROW COUNT");
        } else if (completeCount > 0) {
            LOG.info("database row count: " + completeCount);
            historyDatabaseDAO.completeInit();
            evChargingDAO.completeInit();
            pushMessageDAO.completeInit();
            stateHandlerDAO.completeInit();
            restoreRunCounter++;
        } else {
            // waiting for restoreTables() ...
        }
    }

    @Scheduled(initialDelay = 30_000L, fixedDelay=Long.MAX_VALUE)
    public void restoreTables() throws IOException {

        if(restoreRunCounter > 0){
            return;
        }

        long completeCount = historyDatabaseDAO.getCountOnStartup();

        if(completeCount == -1) {
            LOG.warn("!! COULD NOT CHECK DATABASE ROW COUNT");
        } else if (completeCount == 0) {
            File importFile = new File(lookupPath() + "import.sql.zip");
            if (importFile.exists()) {
                LOG.info("!! AUTO-IMPORT DATABASE FROM: " + importFile.getAbsolutePath());
                backupRestoreDAO.restoreDatabase(importFile.getAbsolutePath());
                //noinspection ResultOfMethodCallIgnored
                importFile.renameTo(new File(importFile.getAbsolutePath() + ".done"));
                LOG.info("!! AUTO-IMPORT FINISHED");
            }else{
                LOG.warn("!! DATABASE EMPTY - NO AUTO-IMPORT FILE FOUND");
            }
        } else {
            LOG.info("database row count: " + completeCount);
        }

        historyDatabaseDAO.completeInit();
        evChargingDAO.completeInit();
        pushMessageDAO.completeInit();
        stateHandlerDAO.completeInit();

        restoreRunCounter++;
    }

    @PreDestroy
    private void backupDatabaseOnShutdown() {
        backupRestoreDAO.backupDatabase(backupFilename(BACKUP_ADHOC_TIMESTAMP_FORMATTER, false));
    }

    @Scheduled(cron = "0 45 01 * * *")
    private void backupDatabaseCreateNew() {
        backupRestoreDAO.backupDatabase(backupFilename(BACKUP_DAILY_TIMESTAMP_FORMATTER, false));
    }

    @Scheduled(cron = "0 50 01 * * *")
    private void backupDatabaseUpload() {

        Path path = Paths.get(backupFilename(BACKUP_DAILY_TIMESTAMP_FORMATTER, false));

        try {
            List<Path> list = new LinkedList<>();
            if (path.toFile().exists()) {
                list.add(path);
            }
            backupAPI.backup(list.stream());
        } catch (Exception e) {
            LOG.error("Exception upload backup file to backblaze:", e);
        }

        try {
            if (path.toFile().exists()) {
                BackupFile backupFile = new BackupFile();
                backupFile.setFilename(path.toFile().getName());
                backupFile.setBytes(FileUtils.readFileToByteArray(path.toFile()));
                uploadService.uploadToClient(backupFile);
            }
        } catch (Exception e) {
            LOG.error("Exception upload backup file to client:", e);
        }

        Path yesterdaysFile = Paths.get(backupFilename(BACKUP_DAILY_TIMESTAMP_FORMATTER, true));
        if (yesterdaysFile.toFile().exists()) {
            try {
                Files.delete(yesterdaysFile);
            } catch (IOException e) {
                LOG.error("Exception deleting database backup file:", e);
            }
        }
    }

    private String backupFilename(DateTimeFormatter formatter, boolean yesterday) {

        LocalDateTime dateTime = LocalDateTime.now();
        if (yesterday) {
            dateTime = dateTime.minusHours(24);
        }
        String path = lookupPath();
        String timestamp = formatter.format(dateTime);
        return path + "backup_" + timestamp + ".sql.zip";
    }

    public String lookupPath() {

        String path = env.getProperty("backup.database.path");
        if (!Objects.requireNonNull(path).endsWith("/")) {
            path = path + "/";
        }
        return path;
    }
}
