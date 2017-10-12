package org.epihack.vn2017.crawler.extractor.impl;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.epihack.vn2017.crawler.db.bean.Article;
import org.epihack.vn2017.crawler.db.bean.Article.ArticleBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DefaultHtmlExtractor extends AbstractHtmlExtractor {

	protected String getTimeSelector() {
		return (String) this.getConfig().get("timeSelector");
	}

	@SuppressWarnings("unchecked")
	protected Map<String, String> getTimeFormat() {
		return (Map<String, String>) this.getConfig().get("timeFormat");
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

		Map<String, String> timeFormat = this.getTimeFormat();

		String patternStr = timeFormat.get("pattern");

		int dateGroupId = Integer.valueOf(timeFormat.get("dateGroupId"));
		int monthGroupId = Integer.valueOf(timeFormat.get("monthGroupId"));
		int yearGroupId = Integer.valueOf(timeFormat.get("yearGroupId"));
		int hourGroupId = Integer.valueOf(timeFormat.get("hourGroupId"));
		int minuteGroupId = Integer.valueOf(timeFormat.get("minuteGroupId"));

		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(timeStr);
		if (matcher.find()) {
			String date = matcher.group(dateGroupId);
			String month = matcher.group(monthGroupId);
			String year = matcher.group(yearGroupId);
			String hour = matcher.group(hourGroupId);
			String minute = matcher.group(minuteGroupId);

			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.DATE, Integer.valueOf(date));
			cal.set(Calendar.MONTH, Integer.valueOf(month) - 1);
			cal.set(Calendar.YEAR, Integer.valueOf(year));
			cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(hour));
			cal.set(Calendar.MINUTE, Integer.valueOf(minute));

			return cal.getTime();
		}

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
