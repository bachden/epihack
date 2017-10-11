package org.epihack.vn2017.crawler.extractor;

import java.util.Map;

public interface HtmlExtractorConfigAware {

	void setConfig(Map<String, Object> config);
}
