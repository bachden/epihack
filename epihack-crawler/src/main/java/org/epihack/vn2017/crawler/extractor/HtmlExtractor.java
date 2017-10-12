package org.epihack.vn2017.crawler.extractor;

import org.epihack.vn2017.crawler.db.bean.Article;

public interface HtmlExtractor {
	
	Article extract(String html);
}
