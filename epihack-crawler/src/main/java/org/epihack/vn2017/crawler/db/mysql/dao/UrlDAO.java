package org.epihack.vn2017.crawler.db.mysql.dao;

import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public abstract class UrlDAO extends MySqlEpihackBaseDAO {

	@SqlUpdate("CREATE TABLE IF NOT EXISTS `url` (`id` BINARY(16) NOT NULL, `url` VARCHAR(2048) NULL, `last_scan_time` BIGINT(13) NULL,  PRIMARY KEY (`id`)) ENGINE = InnoDB")
	public abstract void createTable();
}
