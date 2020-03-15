package de.fimatas.home.controller.database.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class StringRowMapper implements RowMapper<String> {

	private String colName;

	public StringRowMapper(String colName) {
		this.colName = colName;
	}

	@Override
	public String mapRow(ResultSet rs, int rowNum) throws SQLException {
		return rs.getString(colName);
	}

}
