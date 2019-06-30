package homecontroller.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BackupService {

	@Autowired
	private Environment env;

	@Autowired
	private BackblazeBackupAPI backupAPI;

	private static final Log LOG = LogFactory.getLog(BackupService.class);

	@Scheduled(cron = "0 45 1 * * *")
	private void scheduledBackup() {

		try (Stream<Path> pathes = Files.walk(Paths.get(env.getProperty("backup.base")))) {
			Stream<Path> readablePathes = pathes.filter(path -> path.toFile().isFile());
			readablePathes.forEach(path -> backupAPI.backup(path));
		} catch (IOException ioe) {
			LOG.error("Exception reading backup files:", ioe);
		}

	}
}
