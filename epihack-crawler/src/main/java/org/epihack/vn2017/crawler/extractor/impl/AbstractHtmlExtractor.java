package org.epihack.vn2017.crawler.extractor.impl;

import java.util.Map;

import org.epihack.vn2017.crawler.extractor.HtmlExtractor;
import org.epihack.vn2017.crawler.extractor.HtmlExtractorConfigAware;

import com.nhb.common.Loggable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public abstract class AbstractHtmlExtractor implements HtmlExtractor, HtmlExtractorConfigAware, Loggable {

	@Setter
	@Getter(AccessLevel.PROTECTED)
	private Map<String, Object> config;
}
