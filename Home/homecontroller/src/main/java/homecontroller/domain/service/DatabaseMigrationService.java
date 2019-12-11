package homecontroller.domain.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import homecontroller.dao.HistoryDatabaseDAO;
import homecontroller.dao.MigrationDatabaseDAO;
import homecontroller.database.mapper.TimestampValuePair;
import homelibrary.homematic.model.History;
import homelibrary.homematic.model.HomematicCommand;

@Component
public class DatabaseMigrationService {

	@Autowired
	private MigrationDatabaseDAO migrationDatabaseDAO;

	@Autowired
	HistoryDatabaseDAO historyDatabaseDAO;

	private static final Log LOG = LogFactory.getLog(DatabaseMigrationService.class);

	public void startMigration() {

		for (History history : History.values()) {
			List<TimestampValuePair> sourceList = read(history.getCommand());
			List<List<TimestampValuePair>> partitions = ListUtils.partition(sourceList, 1000);
			int i = 0;
			for (List<TimestampValuePair> partition : partitions) {
				i++;
				Map<HomematicCommand, List<TimestampValuePair>> toInsert = new HashMap<>();
				toInsert.put(history.getCommand(), partition);
				LOG.info("Migration of " + history.getCommand().buildVarName() + " Part " + i + " of "
						+ partitions.size());
				historyDatabaseDAO.persistEntries(toInsert);
			}
		}

	}

	private List<TimestampValuePair> read(HomematicCommand command) {
		return migrationDatabaseDAO.readValues(command);
	}

}
