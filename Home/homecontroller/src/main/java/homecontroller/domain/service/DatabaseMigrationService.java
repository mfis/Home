package homecontroller.domain.service;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import homecontroller.database.mapper.TimestampValuePair;
import homelibrary.homematic.model.History;

@Component
public class DatabaseMigrationService {

	@Autowired
	private HistoryService historyService;

	private static final Log LOG = LogFactory.getLog(DatabaseMigrationService.class);

	private List<TimestampValuePair> filterEntries(History history, List<TimestampValuePair> source) {

		List<TimestampValuePair> dest = new LinkedList<>();
		for (TimestampValuePair sourceEntry : source) {
			historyService.diffValueCheckedAdd(history, sourceEntry, dest);
		}
		LOG.info("filter history migration entries: " + history.name() + ": " + source.size() + " / "
				+ dest.size());
		return dest;
	}

}
