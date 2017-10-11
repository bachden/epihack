package org.epihack.vn2017.crawler.db.bean;

import com.nhb.common.db.beans.UUIDBean;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UrlBean extends UUIDBean {

	private static final long serialVersionUID = 1L;

	private String url;
	private long lastScanTime;
}
