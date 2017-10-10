package org.epihack.vn2017.crawler.db.mysql;

import java.util.List;

import org.epihack.vn2017.crawler.db.mysql.dao.UrlDAO;

import com.nhb.common.db.models.AbstractModel;

public class MySqlEpihackBaseModel extends AbstractModel {

	public boolean checkTableExists(String tableName) {
		try (UrlDAO dao = this.openDAO(UrlDAO.class)) {
			List<String> existingTable = dao.showTable(tableName);
			return existingTable.size() > 0;
		}
	}
}
