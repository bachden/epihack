package org.epihack.vn2017.crawler.db.mysql.dao;

import java.util.Collection;

import org.epihack.vn2017.crawler.db.bean.Article;
import org.epihack.vn2017.crawler.db.bean.DiseaseNumCases;
import org.epihack.vn2017.crawler.db.mysql.mapper.ArticleMapper;
import org.epihack.vn2017.crawler.db.mysql.mapper.DiseaseNumCasesMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

public abstract class ArticleDAO extends MySqlEpihackBaseDAO {

	@SqlUpdate("CREATE TABLE `article` (\n" + "  `id` binary(16) NOT NULL,\n"
			+ "  `url` varchar(512) COLLATE utf8_unicode_ci NOT NULL,\n" + "  `timestamp` bigint(13) DEFAULT NULL,\n"
			+ "  `title` varchar(256) COLLATE utf8_unicode_ci DEFAULT NULL,\n"
			+ "  `short_description` varchar(2048) COLLATE utf8_unicode_ci DEFAULT NULL,\n"
			+ "  `content` text COLLATE utf8_unicode_ci,\n"
			+ "  `disease` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,\n"
			+ "  `group` char(1) COLLATE utf8_unicode_ci DEFAULT NULL,\n"
			+ "  `province` varchar(45) COLLATE utf8_unicode_ci DEFAULT NULL,\n" + "  PRIMARY KEY (`id`),\n"
			+ "  UNIQUE KEY `url_UNIQUE` (`url`),\n" + "  KEY `group_index` (`group`),\n"
			+ "  KEY `province_index` (`province`),\n" + "  KEY `disease_index` (`disease`)\n"
			+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci")
	public abstract void createTable();

	@SqlUpdate("INSERT INTO `article` (`id`, `url`, `timestamp`, `title`, `short_description`, `content`, `disease`, `group`, `province`) VALUES (:id, :url, :timestamp, :title, :shortDescription, :content, :disease, :group, :province);")
	public abstract void insert(@BindBean Article article);

	@SqlQuery("select count(*) as numberOfCases, `disease` as `name` from article where `group` = :groupLabel group by `disease`")
	@RegisterMapper(DiseaseNumCasesMapper.class)
	public abstract Collection<DiseaseNumCases> countDiseaseNumCasesByGroup(@Bind("groupLabel") String groupLabel);

	@SqlQuery("UPDATE `article` SET `status` = :status WHERE `id` = :id")
	public abstract int updateArticleStatus(byte[] articleId, int status);

	// @SqlQuery("SELECT count(*), province FROM article where `group` = :group
	// group by `province`")
	// public abstract void countCasesGroupByProvince(@Bind("group") String group);
	//
	// @SqlQuery("SELECT * FROM article where `group` = :group and `province` =
	// :provinceName")
	// public abstract void fetchArticles(@Bind("group") String group,
	// @Bind("provinceName") String provinceName);

	@SqlQuery("Select * from article")
	@RegisterMapper(ArticleMapper.class)
	public abstract Collection<Article> fetchAll();
}
