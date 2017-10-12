package org.epihack.vn2017.crawler.db.mysql.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.epihack.vn2017.crawler.db.bean.Article;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public class ArticleMapper implements ResultSetMapper<Article> {

	@Override
	public Article map(int index, ResultSet r, StatementContext ctx) throws SQLException {
		Article result = new Article();
		result.setContent(r.getString("content"));
		result.setDisease(r.getString("disease"));
		result.setProvince(r.getString("province"));
		result.setGroup(r.getString("group"));
		result.setShortDescription(r.getString("short_description"));
		result.setTime(new Date(r.getLong("timestamp")));
		result.setStatus(r.getInt("status"));
		result.setTitle(r.getString("title"));
		result.setId(r.getBytes("id"));
		return result;
	}

}
