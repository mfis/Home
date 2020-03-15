package de.fimatas.home.controller.database.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class LongRowMapper implements RowMapper<Long> {

	private String colName;

	public LongRowMapper(String colName) {
		this.colName = colName;
	}

	@Override
	public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
		return rs.getLong(colName);
	}

}
