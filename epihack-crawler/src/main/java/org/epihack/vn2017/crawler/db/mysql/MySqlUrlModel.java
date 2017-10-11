package org.epihack.vn2017.crawler.db.mysql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.epihack.vn2017.crawler.db.UrlModel;
import org.epihack.vn2017.crawler.db.bean.UrlBean;
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

	@Override
	public void insert(Collection<String> urls) {
		List<UrlBean> beans = new ArrayList<>();
		for (String url : urls) {
			UrlBean bean = new UrlBean();
			bean.autoId();
			bean.setUrl(url);
			bean.setLastScanTime(-1l);
			beans.add(bean);
		}
		try (UrlDAO dao = this.openDAO(UrlDAO.class)) {
			dao.insert(beans);
		}
	}

	@Override
	public Collection<UrlBean> fetchAvailableUrls(long expiredTime) {
		try (UrlDAO dao = this.openDAO(UrlDAO.class)) {
			return dao.fetchAvailableUrl(System.currentTimeMillis(), expiredTime);
		}
	}

	@Override
	public void updateLastScanTime(byte[] id, long lastScanTime) {
		try (UrlDAO dao = this.openDAO(UrlDAO.class)) {
			dao.updateLastScanTime(id, lastScanTime);
		}
	}

	@Override
	public void updateLastScanTime(String url, long lastScanTime) {
		try (UrlDAO dao = this.openDAO(UrlDAO.class)) {
			dao.updateLastScanTime(url, lastScanTime);
		}
	}

	@Override
	public Collection<UrlBean> fetchAll() {
		try (UrlDAO dao = this.openDAO(UrlDAO.class)) {
			return dao.fetchAll();
		}
	}
}
