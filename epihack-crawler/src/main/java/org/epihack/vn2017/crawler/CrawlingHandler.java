package org.epihack.vn2017.crawler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.epihack.vn2017.crawler.db.ArticleModel;
import org.epihack.vn2017.crawler.db.UrlModel;
import org.epihack.vn2017.crawler.db.bean.Article;
import org.epihack.vn2017.crawler.db.bean.DiseaseNumCases;
import org.epihack.vn2017.crawler.db.bean.UrlBean;
import org.epihack.vn2017.crawler.extractor.ExtractorConfig;
import org.epihack.vn2017.crawler.extractor.HtmlExtractor;
import org.epihack.vn2017.crawler.extractor.HtmlExtractorFactory;

import com.mario.entity.impl.BaseMessageHandler;
import com.mario.entity.message.Message;
import com.mario.external.configuration.ExternalConfiguration;
import com.mario.schedule.ScheduledCallback;
import com.nhb.common.async.Callback;
import com.nhb.common.data.MapTuple;
import com.nhb.common.data.PuArray;
import com.nhb.common.data.PuArrayList;
import com.nhb.common.data.PuElement;
import com.nhb.common.data.PuObject;
import com.nhb.common.data.PuObjectRO;
import com.nhb.common.db.models.ModelFactory;
import com.nhb.common.utils.Converter;
import com.nhb.common.utils.FileSystemUtils;

import lombok.Getter;

public class CrawlingHandler extends BaseMessageHandler {

	private ModelFactory modelFactory;

	private Diseases diseases;

	@Getter
	private Collection<String> provinces;

	private UrlModel urlModel;

	private HtmlExtractorFactory extractorFactory;

	private HtmlFetcher fetcher;

	private long rescanDelaySeconds = 300;

	private AtomicInteger crawlingCount = new AtomicInteger(0);

	private final int numWorkers = 32;
	private final ExecutorService executor = Executors.newFixedThreadPool(numWorkers);
	private final List<String> waitingUrls = new CopyOnWriteArrayList<>();

	private ArticleModel articleModel;

	private Map<String, Integer> articleStatuses = new HashMap<>();
	{
		articleStatuses.put("accepted", 1);
		articleStatuses.put("rejected", 2);
		articleStatuses.put("ignored", 3);
	}

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

		String diseasesConfName = initParams.getString("diseasesConfig", null);
		initDiseases(diseasesConfName);

		String urlsConfName = initParams.getString("urlsConfig", null);
		initUrls(urlsConfName);

		String extractorConfigName = initParams.getString("htmlExtractorConfig", null);
		initExtractors(extractorConfigName);

		String provincesConfigName = initParams.getString("provincesConfig", null);
		initProvinces(provincesConfigName);

		this.fetcher = new HtmlFetcher();
	}

	private void initProvinces(String provincesConfigName) {
		if (provincesConfigName == null) {
			throw new NullPointerException("Provinces config name cannot be null");
		}
		this.provinces = new HashSet<>();
		ExternalConfiguration configuration = this.getApi().getExternalConfiguration(provincesConfigName);
		this.provinces.addAll(configuration.get());

		configuration.addUpdateListener(new Callback<Collection<String>>() {

			@Override
			public void apply(Collection<String> configs) {
				provinces.clear();
				provinces.addAll(configuration.get());
			}
		});
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

	private void initDiseases(String keywordsConfName) {
		if (keywordsConfName == null) {
			throw new NullPointerException("keywords config name cannot be null");
		}
		ExternalConfiguration config = getApi().getExternalConfiguration(keywordsConfName);

		this.diseases = config.get();
		config.addUpdateListener(new Callback<Diseases>() {

			@Override
			public void apply(Diseases diseases) {
				CrawlingHandler.this.diseases = diseases;
			}
		});

		getLogger().debug("diseases: " + this.diseases);
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

		articleModel = this.modelFactory.getModel(ArticleModel.class.getName());
		if (!articleModel.checkTableExists()) {
			articleModel.createTable();
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

	private String[] searchForDiseaseAndProvince(String... arr) {
		String group = null;
		String foundDisease = null;
		String foundProvince = null;

		for (String text : arr) {
			if (text != null) {
				text = text.toLowerCase();
				for (Disease disease : diseases.getGroupA()) {
					Collection<String> keywords = new HashSet<>();
					keywords.add(disease.getName());
					if (disease.getKeywords() != null) {
						keywords.addAll(disease.getKeywords());
					}
					for (String keyword : keywords) {
						if (text.contains(keyword)) {
							foundDisease = disease.getName();
							group = "a";
							break;
						}
					}
				}

				for (Disease disease : diseases.getGroupB()) {
					Collection<String> keywords = new HashSet<>();
					keywords.add(disease.getName());
					if (disease.getKeywords() != null) {
						keywords.addAll(disease.getKeywords());
					}
					for (String keyword : keywords) {
						if (text.contains(keyword)) {
							foundDisease = disease.getName();
							group = "b";
							break;
						}
					}
				}

				for (Disease disease : diseases.getGroupC()) {
					Collection<String> keywords = new HashSet<>();
					keywords.add(disease.getName());
					if (disease.getKeywords() != null) {
						keywords.addAll(disease.getKeywords());
					}
					for (String keyword : keywords) {
						if (text.contains(keyword)) {
							foundDisease = disease.getName();
							group = "c";
							break;
						}
					}
				}

				for (String province : provinces) {
					if (text.contains(province)) {
						foundProvince = province;
						break;
					}
				}

				if (foundDisease != null && foundProvince != null) {
					break;
				}
			}
		}

		return new String[] { foundDisease == null ? "none" : foundDisease, group,
				foundProvince == null ? "none" : foundProvince };
	}

	private void crawl(final String url) {
		if (url == null) {
			return;
		}

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
						Article article = null;
						try {
							article = extractor.extract(html);
						} catch (Exception e) {
							getLogger().error("Error white extract html from url: {}", trimedUrl, e);
						}
						if (article != null) {
							String[] arr = searchForDiseaseAndProvince(article.getContent(), article.getTitle(),
									article.getShortDescription());

							article.setDisease(arr[0]);
							article.setGroup(arr[1]);
							article.setProvince(arr[2]);
							article.setUrl(trimedUrl);

							if (!article.getDisease().equals("none")) {
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
							getLogger().debug("got data but cannot extract: " + trimedUrl);
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
		if (article == null) {
			getLogger().error("Cannot save null article",
					new NullPointerException("article to be saved cannot be null"));
		} else if (article.getUrl() == null) {
			getLogger().warn("Cannot save article with no url");
		} else {
			if (article.getId() == null) {
				article.autoId();
			}
			try {
				getLogger().debug("Saving article: {}", article);
				this.articleModel.saveArticle(article);
			} catch (Exception e) {
				getLogger().error("Cannot save article", e);
			}
		}
	}

	private String nextUrl() {
		synchronized (waitingUrls) {
			if (waitingUrls.size() == 0) {
				return null;
			}
			return waitingUrls.remove(0);
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

	private void startCrawlingWorker() {
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
				if (waitingUrls.size() == 0) {
					Collection<UrlBean> fetchAvailableUrl = urlModel.fetchAvailableUrls(rescanDelaySeconds * 1000);
					for (UrlBean bean : fetchAvailableUrl) {
						waitingUrls.add(bean.getUrl());
					}
				}
				if (crawlingCount.get() == 0) {
					startCrawlingWorker();
				}
			}
		});

		startCrawlingWorker();
	}

	@Override
	public PuElement handle(Message message) {
		if (message.getData() != null && message.getData() instanceof PuObject) {
			PuObject data = (PuObject) message.getData();
			String command = data.getString("command", data.getString("cmd", null));
			if (command != null) {
				try {
					String[] groupLabels = new String[] { "a", "b", "c" };
					switch (command) {
					case "countDiseaseNumCasesByGroup": {
						PuObject resultData = new PuObject();
						for (String groupLabel : groupLabels) {
							PuArray arr = new PuArrayList();
							Collection<DiseaseNumCases> list = this.articleModel
									.countDiseaseNumCasesByGroup(groupLabel);
							for (DiseaseNumCases dnc : list) {
								arr.addFrom(dnc.toPuObject());
							}
							resultData.set("group" + groupLabel.toUpperCase(), arr);
						}
						PuObject response = new PuObject();
						response.set("data", resultData);
						response.set("status", Status.OK.getCode());
						return response;
					}
					case "fetchAllArticle": {
						Collection<Article> articles = this.articleModel.fetchAll();
						Map<String, Map<String, Collection<Article>>> diseaseToProvinceToArticle = new HashMap<>();
						for (Article article : articles) {
							if (!diseaseToProvinceToArticle.containsKey(article.getDisease())) {
								diseaseToProvinceToArticle.put(article.getDisease(), new HashMap<>());
							}
							Map<String, Collection<Article>> provinceToArticle = diseaseToProvinceToArticle
									.get(article.getDisease());

							if (!provinceToArticle.containsKey(article.getProvince())) {
								provinceToArticle.put(article.getProvince(), new HashSet<>());
							}

							Collection<Article> articleByProvinceAndDiseaseCollection = provinceToArticle
									.get(article.getProvince());
							articleByProvinceAndDiseaseCollection.add(article);
						}
						PuObject diseases = new PuObject();
						for (Entry<String, Map<String, Collection<Article>>> diseaseToProvinceEntry : diseaseToProvinceToArticle
								.entrySet()) {

							PuArray provinces = new PuArrayList();
							Map<String, Collection<Article>> provinceToArticles = diseaseToProvinceEntry.getValue();
							for (Entry<String, Collection<Article>> provinceToArticlesEntry : provinceToArticles
									.entrySet()) {

								Collection<Article> articlesByProvince = provinceToArticlesEntry.getValue();
								PuArray articleList = new PuArrayList();
								for (Article article : articlesByProvince) {
									articleList.addFrom(article.toPuObject());
								}

								PuObject province = new PuObject();
								province.set("name", provinceToArticlesEntry.getKey());
								province.set("contactInfo", "Nguyễn Văn A: 0901234567");
								province.set("numberOfCases", articlesByProvince.size());
								province.set("articles", articleList);

								provinces.addFrom(province);
							}

							PuObject disease = new PuObject();
							disease.set("name", diseaseToProvinceEntry.getKey());
							disease.set("provinces", provinces);
							diseases.set(diseaseToProvinceEntry.getKey(), disease);
						}
						return PuObject.fromObject(new MapTuple<>("status", Status.OK.getCode(), "data", diseases));
					}
					case "changeArticleStatus": {
						String articleUUID = data.getString("articleId");
						if (articleUUID != null) {
							byte[] articleId = Converter.uuidToBytes(articleUUID);
							String nextStatus = data.getString("status");
							Integer statusValue = this.articleStatuses.get(nextStatus);
							if (statusValue != null) {
								if (this.articleModel.updateArticleStatus(articleId, statusValue.intValue())) {
									return PuObject.fromObject(new MapTuple<>("status", Status.OK.getCode()));
								} else {
									return PuObject.fromObject(new MapTuple<>("status", Status.ERROR.getCode(),
											"message", "Cannot update article status"));
								}
							} else {
								return PuObject.fromObject(new MapTuple<>("status", Status.INVALID_PARAMETER.getCode(),
										"message",
										"status name is invalid, valid values are: accepted, rejected, ignored"));
							}
						} else {
							return PuObject.fromObject(new MapTuple<>("status", Status.INVALID_PARAMETER.getCode(),
									"message", "article id must be specific"));
						}
					}
					}
				} catch (Exception e) {
					getLogger().error("Error while processing request: ", e);
					return PuObject.fromObject(new MapTuple<>("status", Status.INTERNAL_SERVER_ERROR.getCode(),
							"message", e.getMessage()));
				}
			} else {
				return PuObject.fromObject(new MapTuple<>("status", Status.MISSING_COMMAND.getCode(), "message",
						Status.MISSING_COMMAND.getMessage()));
			}
		}
		return PuObject.fromObject(
				new MapTuple<>("status", Status.UNKNOWN_ERROR.getCode(), "message", Status.UNKNOWN_ERROR.getMessage()));
	}
}
