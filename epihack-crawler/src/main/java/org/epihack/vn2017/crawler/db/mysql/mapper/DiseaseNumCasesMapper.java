package org.epihack.vn2017.crawler.db.mysql.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.epihack.vn2017.crawler.db.bean.DiseaseNumCases;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public class DiseaseNumCasesMapper implements ResultSetMapper<DiseaseNumCases> {

	@Override
	public DiseaseNumCases map(int index, ResultSet r, StatementContext ctx) throws SQLException {
		DiseaseNumCases result = new DiseaseNumCases();
		result.setName(r.getString("name"));
		result.setNumberOfCases(r.getInt("numberOfCases"));
		return result;
	}

}
