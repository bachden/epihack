package org.epihack.vn2017.crawler.db.mysql.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.epihack.vn2017.crawler.db.bean.UrlBean;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public class UrlMapper implements ResultSetMapper<UrlBean> {

	@Override
	public UrlBean map(int index, ResultSet r, StatementContext ctx) throws SQLException {
		UrlBean bean = new UrlBean();
		bean.setId(r.getBytes("id"));
		bean.setLastScanTime(r.getLong("last_scan_time"));
		bean.setUrl(r.getString("url"));
		return bean;
	}

}
