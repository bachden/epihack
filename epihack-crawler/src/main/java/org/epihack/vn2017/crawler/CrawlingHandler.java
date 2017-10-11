package org.epihack.vn2017.crawler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.epihack.vn2017.crawler.db.UrlModel;
import org.epihack.vn2017.crawler.db.bean.UrlBean;
import org.epihack.vn2017.crawler.extractor.Article;
import org.epihack.vn2017.crawler.extractor.ExtractorConfig;
import org.epihack.vn2017.crawler.extractor.HtmlExtractor;
import org.epihack.vn2017.crawler.extractor.HtmlExtractorFactory;

import com.mario.entity.impl.BaseMessageHandler;
import com.mario.external.configuration.ExternalConfiguration;
import com.mario.schedule.ScheduledCallback;
import com.nhb.common.async.Callback;
import com.nhb.common.data.PuObjectRO;
import com.nhb.common.db.models.ModelFactory;
import com.nhb.common.utils.FileSystemUtils;
import com.nhb.common.utils.StringUtils;

import lombok.Getter;

public class CrawlingHandler extends BaseMessageHandler {

	private ModelFactory modelFactory;

	@Getter
	private Collection<String> keywords;

	private UrlModel urlModel;

	private HtmlExtractorFactory extractorFactory;

	private HtmlFetcher fetcher;

	private long rescanDelaySeconds = 300;

	private AtomicInteger crawlingCount = new AtomicInteger(0);

	private final int numWorkers = 32;
	private final ExecutorService executor = Executors.newFixedThreadPool(numWorkers);
	private final List<String> waitingForCrawlingUrls = new CopyOnWriteArrayList<>();

	@Override
	public void init(PuObjectRO initParams) {

		String modelMappingFile = initParams.getString("modelMappingFile", null);
		String mysqlDatasourceName = initParams.getString("mysqlDatasourceName", null);
		if (modelMappingFile == null) {
			throw new IllegalArgumentException("modelMappingFile and mysqlDatasourceName must be set");
		}
		try {
			this.initModelFactory(modelMappingFile, mysqlDatasourceName);
		} catch (Exception e) {
			throw new RuntimeException("Error while init model factory", e);
		}

		String keywordsConfName = initParams.getString("keywordsConfig", null);
		initKeywords(keywordsConfName);

		String urlsConfName = initParams.getString("urlsConfig", null);
		initUrls(urlsConfName);

		String extractorConfigName = initParams.getString("htmlExtractorConfig", null);
		initExtractors(extractorConfigName);

		this.fetcher = new HtmlFetcher();
	}

	@Override
	public void destroy() throws Exception {
		if (this.fetcher != null) {
			this.fetcher.shutdown();
		}
		executor.shutdown();
		if (executor.awaitTermination(2, TimeUnit.SECONDS)) {
			executor.shutdownNow();
		}
	}

	private void initExtractors(String extractorConfigName) {
		if (extractorConfigName == null) {
			throw new NullPointerException("Extractor config name cannot be null");
		}
		this.extractorFactory = new HtmlExtractorFactory();
		ExternalConfiguration configuration = this.getApi().getExternalConfiguration(extractorConfigName);
		Collection<ExtractorConfig> externalConfiguration = configuration.get();
		this.extractorFactory.config(externalConfiguration);

		configuration.addUpdateListener(new Callback<Collection<ExtractorConfig>>() {

			@Override
			public void apply(Collection<ExtractorConfig> configs) {
				extractorFactory.config(configs);
			}
		});
	}

	private void initUrls(String urlsConfName) {
		if (urlsConfName == null) {
			throw new NullPointerException("urls conf name cannot be null");
		}
		final ExternalConfiguration config = getApi().getExternalConfiguration(urlsConfName);
		final Collection<String> urls = new CopyOnWriteArraySet<>();

		Runnable urlUpdater = () -> {
			try {
				Collection<UrlBean> beans = urlModel.fetchAll();

				Collection<String> existingUrls = new HashSet<>();
				for (UrlBean bean : beans) {
					existingUrls.add(bean.getUrl());
				}

				Collection<String> tobeInsertedBeans = new HashSet<>();
				for (String newUrl : urls) {
					if (!existingUrls.contains(newUrl)) {
						tobeInsertedBeans.add(newUrl);
					}
				}

				if (tobeInsertedBeans.size() > 0) {
					getLogger().debug("adding new url: {}", tobeInsertedBeans);
					urlModel.insert(tobeInsertedBeans);
				}
			} catch (Exception ex) {
				getLogger().error("Error while updating urls into db", ex);
			}
		};

		urls.addAll(config.get());
		config.addUpdateListener(new Callback<Collection<String>>() {

			@Override
			public void apply(Collection<String> updatedUrls) {
				urls.clear();
				urls.addAll(updatedUrls);
				urlUpdater.run();
			}
		});

		urlUpdater.run();
	}

	private void initKeywords(String keywordsConfName) {
		if (keywordsConfName == null) {
			throw new NullPointerException("keywords config name cannot be null");
		}
		ExternalConfiguration config = getApi().getExternalConfiguration(keywordsConfName);
		this.keywords = config.get();
		config.addUpdateListener(new Callback<Collection<String>>() {

			@Override
			public void apply(Collection<String> keywords) {
				CrawlingHandler.this.keywords = keywords;
			}
		});
	}

	private void initModelFactory(String modelMappingFile, String mysqlDatasource)
			throws FileNotFoundException, IOException {
		this.modelFactory = new ModelFactory(getApi().getDatabaseAdapter(mysqlDatasource));
		this.modelFactory.setClassLoader(this.getClass().getClassLoader());
		String filePath = FileSystemUtils.createAbsolutePathFrom("extensions", this.getExtensionName(),
				modelMappingFile);
		try (InputStream is = new FileInputStream(filePath)) {
			Properties props = new Properties();
			props.load(is);
			this.modelFactory.addClassImplMapping(props);
		}
		this.urlModel = this.modelFactory.getModel(UrlModel.class.getName());
		if (!urlModel.checkTableExists()) {
			urlModel.createTable();
		}
	}

	private void addUrls(Collection<String> urls) {
		for (String url : urls) {
			try {
				this.urlModel.insert(url);
			} catch (Exception e) {
				// getLogger().error("Add url error", e);
			}
		}
	}

	private void updateLastScanTime(String url) {
		this.urlModel.updateLastScanTime(url, System.currentTimeMillis());
	}

	private void crawl(final String url) {
		if (url == null) {
			return;
		}

		getLogger().debug("continue crawl url: {}", url);

		String trimedUrl = url.trim();
		crawlingCount.incrementAndGet();

		this.fetcher.fetch(trimedUrl, new Callback<String>() {

			@Override
			public void apply(String html) {
				updateLastScanTime(url);
				crawlingCount.decrementAndGet();
				if (html != null) {
					HtmlExtractor extractor = extractorFactory.getExtractor(trimedUrl);
					if (extractor != null) {
						Article article = extractor.extract(html);
						if (article != null) {
							getLogger().debug("article: {}", article);
							StringBuilder pattern = new StringBuilder();
							for (String keyword : keywords) {
								if (pattern.length() > 0) {
									pattern.append("|");
								}
								pattern.append("(").append(keyword).append(")");
							}
							String p = pattern.toString();

							String content = article.getContent();
							String title = article.getTitle();
							String shortDescription = article.getShortDescription();
							if ((content != null && StringUtils.match(content, p)) //
									|| (title != null && StringUtils.match(title, p)) //
									|| (shortDescription != null && StringUtils.match(shortDescription, p))) {
								saveArticle(article);
							}

							Collection<String> urls = new HashSet<>();
							for (String newUrl : article.getRelatedUrls()) {
								if (!newUrl.startsWith(trimedUrl)) {
									if (!newUrl.startsWith("http")) {
										if (!trimedUrl.endsWith("/") && !newUrl.startsWith("/")) {
											newUrl = trimedUrl + "/" + newUrl;
										} else {
											newUrl = trimedUrl + newUrl;
										}
									}
									urls.add(newUrl);
								}
							}
							addUrls(urls);
						} else {
							getLogger().debug("got data but cannot extract: " + html);
						}
					} else {
						getLogger().warn("Extractor not found for url {}", trimedUrl);
					}
				}
				continueCrawling();
			}
		});
	}

	private void saveArticle(Article article) {
		getLogger().debug("Saving article: {}", article.getTitle());
	}

	private String nextUrl() {
		synchronized (waitingForCrawlingUrls) {
			if (waitingForCrawlingUrls.size() == 0) {
				return null;
			}
			return waitingForCrawlingUrls.remove(0);
		}
	}

	private void continueCrawling() {
		String nextUrl = this.nextUrl();
		if (nextUrl != null) {
			this.executor.submit(() -> {
				crawl(nextUrl);
			});
		}
	}

	private void startWorkerForCrawling() {
		for (int i = 0; i < numWorkers; i++) {
			this.continueCrawling();
		}
	}

	@Override
	public void onServerReady() {
		long period = 3000;
		this.getApi().getScheduler().scheduleAtFixedRate(period, period, new ScheduledCallback() {

			@Override
			public void call() {
				if (waitingForCrawlingUrls.size() == 0) {
					Collection<UrlBean> fetchAvailableUrl = urlModel.fetchAvailableUrls(rescanDelaySeconds * 1000);
					for (UrlBean bean : fetchAvailableUrl) {
						waitingForCrawlingUrls.add(bean.getUrl());
					}
				}
				if (crawlingCount.get() == 0) {
					startWorkerForCrawling();
				}
			}
		});

		startWorkerForCrawling();
	}
}
