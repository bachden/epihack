package org.epihack.vn2017.crawler.db.mysql.dao;

import java.util.Collection;

import org.epihack.vn2017.crawler.db.bean.UrlBean;
import org.epihack.vn2017.crawler.db.mysql.mapper.UrlMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

@RegisterMapper(UrlMapper.class)
public abstract class UrlDAO extends MySqlEpihackBaseDAO {

	@SqlUpdate("CREATE TABLE `url` (" //
			+ "`id` binary(16) NOT NULL, " //
			+ "`url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL, " //
			+ "`last_scan_time` bigint(13) NOT NULL, " //
			+ "PRIMARY KEY (`id`), " //
			+ "UNIQUE KEY `url_UNIQUE` (`url`)) " //
			+ "ENGINE=InnoDB " //
			+ "DEFAULT CHARSET=utf8mb4 " //
			+ "COLLATE=utf8mb4_unicode_ci;")
	public abstract void createTable();

	@BatchChunkSize(1000)
	@SqlBatch("INSERT INTO `url` (`id`, `url`, `last_scan_time`) VALUES (:id, :url, :lastScanTime)")
	public abstract void insert(@BindBean Collection<UrlBean> urls);

	@SqlQuery("SELECT * FROM `url` WHERE last_scan_time = -1 OR (:currentTime - last_scan_time) >= :expiredTime")
	public abstract Collection<UrlBean> fetchAvailableUrl(@Bind("currentTime") long currentTime,
			@Bind("expiredTime") long expiredTime);

	@SqlUpdate("UPDATE `url` SET `last_scan_time` = :lastScanTime WHERE `id` = :id")
	public abstract void updateLastScanTime(@Bind("id") byte[] id, @Bind("lastScanTime") long lastScanTime);

	@SqlUpdate("UPDATE `url` SET `last_scan_time` = :lastScanTime WHERE `url` = :url")
	public abstract void updateLastScanTime(@Bind("url") String url, @Bind("lastScanTime") long lastScanTime);

	@SqlQuery("SELECT * FROM `url`")
	public abstract Collection<UrlBean> fetchAll();
}
