package org.epihack.vn2017.crawler.extractor.impl;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.epihack.vn2017.crawler.extractor.Article;
import org.epihack.vn2017.crawler.extractor.Article.ArticleBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DefaultHtmlExtractor extends AbstractHtmlExtractor {

	protected String getTimeSelector() {
		return (String) this.getConfig().get("timeSelector");
	}

	protected String getTitleSelector() {
		return (String) this.getConfig().get("titleSelector");
	}

	protected String getShortDescriptionSelector() {
		return (String) this.getConfig().get("shortDescriptionSelector");
	}

	protected String getContentSelector() {
		return (String) this.getConfig().get("contentSelector");
	}

	protected Element querySelector(Document document, String selector) {
		Elements elements = document.select(selector);
		if (elements.size() > 0) {
			return elements.first();
		}
		return null;
	}

	protected String getText(Document document, String selector) {
		Element element = this.querySelector(document, selector);
		if (element != null) {
			return element.text();
		}
		return null;
	}

	protected Elements querySelectorAll(Document document, String selector) {
		return document.select(selector);
	}

	protected Date extractTime(Document document) {
		String timeStr = this.getText(document, this.getTimeSelector());
		getLogger().debug("Time string: " + timeStr);
		return null;
	}

	protected String extractShortDescription(Document document) {
		return this.getText(document, this.getShortDescriptionSelector());
	}

	protected String extractTitle(Document document) {
		return this.getText(document, this.getTitleSelector());
	}

	protected String extractContent(Document document) {
		return this.getText(document, this.getContentSelector());
	}

	protected Collection<String> extractLinks(Document document) {
		Elements elements = this.querySelectorAll(document, "a");
		Collection<String> results = new HashSet<>();
		for (Element ele : elements) {
			String href = ele.attr("href");
			if (href != null) {
				href = href.trim();
				if (href.length() > 0) {
					results.add(href);
				}
			}
		}
		return results;
	}

	@Override
	public Article extract(String html) {
		Document document = Jsoup.parse(html);

		ArticleBuilder builder = Article.builder();
		builder.time(this.extractTime(document));
		builder.title(extractTitle(document));
		builder.shortDescription(this.extractShortDescription(document));
		builder.content(this.extractContent(document));

		Article result = builder.build();
		result.getRelatedUrls().addAll(extractLinks(document));
		return result;
	}
}
