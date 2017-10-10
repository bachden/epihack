package org.epihack.vn2017.crawler.db.mysql;

import org.epihack.vn2017.crawler.db.UrlModel;
import org.epihack.vn2017.crawler.db.mysql.dao.UrlDAO;

public class MySqlUrlModel extends MySqlEpihackBaseModel implements UrlModel {

	@Override
	public void createTable() {
		try (UrlDAO dao = this.openDAO(UrlDAO.class)) {
			dao.createTable();
		}
	}

	@Override
	public boolean checkTableExists() {
		return this.checkTableExists(TABLE_NAME);
	}

}
