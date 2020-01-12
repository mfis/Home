package homecontroller.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import homecontroller.database.mapper.TimestampValuePair;
import homelibrary.homematic.model.HomematicCommand;

@Repository
public class MigrationDatabaseDAO {

	// @Autowired
	// @Qualifier(value = "jdbcTemplateMigrationDB")
	// private JdbcTemplate jdbcTemplateMigration;

	public List<TimestampValuePair> readValues(HomematicCommand command) {

		// return jdbcTemplateMigration.query(
		// "select * FROM " + command.accessKeyMigrationDB() + " order by ts
		// asc;", new Object[] {},
		// new TimestampValueMigrationRowMapper());
		return null; // FIXME: DELETEME
	}

}
