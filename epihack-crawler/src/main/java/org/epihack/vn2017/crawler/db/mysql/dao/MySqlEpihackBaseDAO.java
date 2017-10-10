package org.epihack.vn2017.crawler.db.mysql.dao;

import java.util.List;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;

import com.nhb.common.db.sql.daos.BaseMySqlDAO;

public abstract class MySqlEpihackBaseDAO extends BaseMySqlDAO {

	@SqlQuery("SHOW TABLES LIKE :tableName")
	public abstract List<String> showTable(@Bind("tableName") String tableName);

}
