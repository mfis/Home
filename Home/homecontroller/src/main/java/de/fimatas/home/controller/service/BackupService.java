package de.fimatas.home.controller.service;

import de.fimatas.home.controller.dao.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Stream;

@Component
@CommonsLog
/*
    Restore Database:
    - stop homecontroller
    - stop homeclient
    - rename last backup to 'import.7z'
    - delete 'homehistory.mv.db' and 'homehistory.trace.db'
    - deploy or start homecontroller
    - wait for finish restoring
    - start homeclient
 */
public class BackupService {

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

    private static final DateTimeFormatter BACKUP_ADHOC_TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd__HH-mm_ss_SSS");

    private final String BACKUP_FILENAME_PREFIX = "backup_";

    private final String BACKUP_FILENAME_SUFFIX = ".7z";

    private int restoreRunCounter = 0;

    @PostConstruct
    public void noRestore() {

        long completeCount = historyDatabaseDAO.getCountOnStartup();

        if(completeCount == -1) {
            log.warn("!! COULD NOT CHECK DATABASE ROW COUNT");
        } else //noinspection StatementWithEmptyBody
            if (completeCount > 0) {
            log.info("database row count: " + completeCount);
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
            log.warn("!! COULD NOT CHECK DATABASE ROW COUNT");
        } else if (completeCount == 0) {
            File importFile = new File(lookupLocalBackupPath() + "import.7z");
            if (importFile.exists()) {
                log.info("!! AUTO-IMPORT DATABASE FROM: " + importFile.getAbsolutePath());
                backupRestoreDAO.restoreDatabase(importFile.getAbsolutePath());
                //noinspection ResultOfMethodCallIgnored
                importFile.renameTo(new File(importFile.getAbsolutePath() + ".done"));
                log.info("!! AUTO-IMPORT FINISHED");
            }else{
                log.warn("!! DATABASE EMPTY - NO AUTO-IMPORT FILE FOUND");
            }
        } else {
            log.info("database row count: " + completeCount);
        }

        historyDatabaseDAO.completeInit();
        evChargingDAO.completeInit();
        pushMessageDAO.completeInit();
        stateHandlerDAO.completeInit();

        restoreRunCounter++;
    }

    @PreDestroy
    public void backupDatabaseOnShutdown() {
        backupDatabase();
    }

    @Scheduled(cron = "0 45 01 * * *")
    public void backupDatabaseCreateNew() {
        backupDatabase();
    }

    private void backupDatabase() {

        String fileName = lookupBackupFileName();
        File localTempFile = new File(lookupLocalTempPath() + "/" + fileName);
        backupRestoreDAO.backupDatabase(localTempFile);

        try {
            FileUtils.copyFile(localTempFile, new File(lookupLocalBackupPath() + fileName));
        } catch (Exception e) {
            log.error("Exception copy local backup file:", e);
        }

        try {
            uploadService.uploadBackupFile(localTempFile);
        } catch(Exception e){
            log.error("Exception upload backup file to client:", e);
        }

        try {
            FileUtils.delete(localTempFile);
        } catch (Exception e) {
            log.error("Exception deleting temp backup:", e);
        }

        try {
            deleteOldBackups(Path.of(lookupLocalBackupPath()));
        } catch (Exception e) {
            log.error("Exception deleting old backups:", e);
        }
    }

    private void deleteOldBackups(Path directory) throws IOException {

        LocalDateTime cutoff = LocalDateTime.now().minusMonths(6);
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(path -> !Files.isDirectory(path))
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        if (!name.startsWith(BACKUP_FILENAME_PREFIX) || !name.endsWith(BACKUP_FILENAME_SUFFIX)) return false;
                        try {
                            String datePart = StringUtils.substringBetween(name, BACKUP_FILENAME_PREFIX, BACKUP_FILENAME_SUFFIX);
                            LocalDateTime fileDate = LocalDateTime.parse(datePart, BACKUP_ADHOC_TIMESTAMP_FORMATTER);
                            return fileDate.isBefore(cutoff);
                        } catch (Exception e) {
                            log.warn("deleteOldBackups#1", e);
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("deleteOldBackups#2", e);
                        }
                    });
        }
    }

    private String lookupBackupFileName() {

        LocalDateTime dateTime = LocalDateTime.now();
        String timestamp = BACKUP_ADHOC_TIMESTAMP_FORMATTER.format(dateTime);
        return BACKUP_FILENAME_PREFIX + timestamp + BACKUP_FILENAME_SUFFIX;
    }

    private String lookupLocalBackupPath() {
        String path = env.getProperty("backup.database.path");
        if (!Objects.requireNonNull(path).endsWith("/")) {
            path = path + "/";
        }
        return path;
    }

    private String lookupLocalTempPath() {
        String rootpath = env.getProperty("backup.database.temp");
        if (Objects.requireNonNull(rootpath).endsWith("/")) {
            rootpath = Strings.CI.removeEnd(rootpath, "/");
        }
        File rootFile = new File(rootpath);
        File targetFile = new File(rootpath + "/homecontroller");
        if(!targetFile.exists() || !targetFile.isDirectory()) {
            if(new File(rootpath).isDirectory()) {
                boolean ok = targetFile.mkdirs();
                if(ok){
                    return targetFile.getAbsolutePath();
                }
            }
        }else {
            return targetFile.getAbsolutePath();
        }
        throw new IllegalStateException("backup path error");
    }

}
