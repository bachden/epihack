package org.epihack.vn2017.crawler.db.mysql;

import java.util.Collection;

import org.epihack.vn2017.crawler.db.ArticleModel;
import org.epihack.vn2017.crawler.db.bean.Article;
import org.epihack.vn2017.crawler.db.bean.DiseaseNumCases;
import org.epihack.vn2017.crawler.db.mysql.dao.ArticleDAO;

public class MySqlArticleModel extends MySqlEpihackBaseModel implements ArticleModel {

	@Override
	public void saveArticle(Article article) {
		try (ArticleDAO dao = this.openDAO(ArticleDAO.class)) {
			dao.insert(article);
		}
	}

	@Override
	public boolean checkTableExists() {
		try (ArticleDAO dao = this.openDAO(ArticleDAO.class)) {
			return dao.showTable(TABLE_NAME).size() > 0;
		}
	}

	@Override
	public void createTable() {
		try (ArticleDAO dao = this.openDAO(ArticleDAO.class)) {
			dao.createTable();
		}
	}

	@Override
	public Collection<DiseaseNumCases> countDiseaseNumCasesByGroup(String group) {
		try (ArticleDAO dao = this.openDAO(ArticleDAO.class)) {
			return dao.countDiseaseNumCasesByGroup(group);
		}
	}

	@Override
	public boolean updateArticleStatus(byte[] articleId, int status) {
		try (ArticleDAO dao = this.openDAO(ArticleDAO.class)) {
			return dao.updateArticleStatus(articleId, status) == 1;
		}
	}

	@Override
	public Collection<Article> fetchAll() {
		try (ArticleDAO dao = this.openDAO(ArticleDAO.class)) {
			return dao.fetchAll();
		}
	}
}
