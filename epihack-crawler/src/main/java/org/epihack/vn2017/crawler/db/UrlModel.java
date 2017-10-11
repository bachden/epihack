package org.epihack.vn2017.crawler.db;

import java.util.Arrays;
import java.util.Collection;

import org.epihack.vn2017.crawler.db.bean.UrlBean;

public interface UrlModel extends EpihackCrawlerModel {

	static final String TABLE_NAME = "url";

	boolean checkTableExists();

	void createTable();

	default void insert(String... urls) {
		this.insert(Arrays.asList(urls));
	}

	void insert(Collection<String> urls);

	Collection<UrlBean> fetchAll();

	Collection<UrlBean> fetchAvailableUrls(long expiredTime);

	void updateLastScanTime(byte[] id, long lastScanTime);

	void updateLastScanTime(String url, long lastScanTime);
}
