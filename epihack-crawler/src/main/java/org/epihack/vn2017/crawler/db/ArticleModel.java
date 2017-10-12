package org.epihack.vn2017.crawler.db;

import java.util.Collection;

import org.epihack.vn2017.crawler.db.bean.Article;
import org.epihack.vn2017.crawler.db.bean.DiseaseNumCases;

public interface ArticleModel {

	static final String TABLE_NAME = "article";

	boolean checkTableExists();

	void createTable();

	void saveArticle(Article article);

	Collection<DiseaseNumCases> countDiseaseNumCasesByGroup(String group);

	boolean updateArticleStatus(byte[] articleId, int intValue);
	
	Collection<Article> fetchAll();
}
