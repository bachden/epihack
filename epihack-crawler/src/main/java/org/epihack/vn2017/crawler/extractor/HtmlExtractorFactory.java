package org.epihack.vn2017.crawler.extractor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.nhb.common.Loggable;

public class HtmlExtractorFactory implements Loggable {

	private Collection<ExtractorConfig> configs;
	private Map<String, HtmlExtractor> cache = new ConcurrentHashMap<>();

	public void config(Collection<ExtractorConfig> configs) {
		synchronized (this) {
			this.configs = new HashSet<>();
			this.cache.clear();
			this.configs.addAll(configs);
		}

	}

	public HtmlExtractor getExtractor(String url) {
		if (this.configs == null) {
			return null;
		}
		for (ExtractorConfig config : this.configs) {
			if (url.startsWith(config.getUrlPrefix())) {
				synchronized (this) {
					if (this.cache.containsKey(config.getUrlPrefix())) {
						return this.cache.get(config.getUrlPrefix());
					}
					try {
						Class<?> clazz = Class.forName(config.getHandler());
						if (HtmlExtractor.class.isAssignableFrom(clazz)) {
							HtmlExtractor extractor = (HtmlExtractor) clazz.newInstance();
							if (extractor instanceof HtmlExtractorConfigAware) {
								((HtmlExtractorConfigAware) extractor).setConfig(config.getConfig());
							}
							this.cache.put(config.getUrlPrefix(), extractor);
							return extractor;
						}
					} catch (Exception e) {
						getLogger().error("Error while create new extractor", e);
					}
				}
			}
		}
		return null;
	}
}
