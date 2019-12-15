package homecontroller.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import homecontroller.dao.HistoryDatabaseDAO;

@Component
public class BackupService {

	@Autowired
	private Environment env;

	@Autowired
	private BackblazeBackupAPI backupAPI;

	@Autowired
	private HistoryDatabaseDAO historyDatabaseDAO;

	private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMATTER = DateTimeFormatter
			.ofPattern("yyyy-MM-dd");

	private static final Log LOG = LogFactory.getLog(BackupService.class);

	@PostConstruct
	private void init() {
		backupDatabaseCreateNew();
		backupDatabaseUpload();
	}

	@Scheduled(cron = "0 45 01 * * *")
	private void backupDatabaseCreateNew() {

		historyDatabaseDAO.backupDatabase(backupFilename(false));
	}

	@Scheduled(cron = "0 50 01 * * *")
	private void backupDatabaseUpload() {

		List<Path> list = new LinkedList<>();
		Path path = Paths.get(backupFilename(false));
		if (path.toFile().exists()) {
			list.add(path);
		}
		backupAPI.backup(list.stream());

		Path yesterdaysFile = Paths.get(backupFilename(true));
		if (yesterdaysFile.toFile().exists()) {
			try {
				Files.delete(yesterdaysFile);
			} catch (IOException e) {
				LOG.error("Exception deleting database backup file:", e);
			}
		}
	}

	@Scheduled(cron = "0 45 02 * * *")
	private void backupHistorian() {

		try (Stream<Path> pathes = Files.walk(Paths.get(env.getProperty("backup.base")))) {
			Stream<Path> readablePathes = pathes.filter(path -> path.toFile().isFile());
			backupAPI.backup(readablePathes);
		} catch (IOException ioe) {
			LOG.error("Exception reading historian backup files:", ioe);
		}
	}

	private String backupFilename(boolean yesterday) {

		LocalDateTime dateTime = LocalDateTime.now();
		if (yesterday) {
			dateTime = dateTime.minusHours(24);
		}
		String path = env.getProperty("backup.database.path");
		String timestamp = BACKUP_TIMESTAMP_FORMATTER.format(dateTime);
		if (!path.endsWith("/")) {
			path = path + "/";
		}
		return path + "backup_" + timestamp + ".zip";
	}
}
