package org.epihack.vn2017.crawler.db;

public interface UrlModel extends EpihackCrawlerModel {

	static final String TABLE_NAME = "url";

	boolean checkTableExists();
	
	void createTable();
}
