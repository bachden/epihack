package org.epihack.vn2017.crawler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;

import org.epihack.vn2017.crawler.db.UrlModel;

import com.mario.entity.impl.BaseMessageHandler;
import com.mario.external.configuration.ExternalConfiguration;
import com.nhb.common.async.Callback;
import com.nhb.common.data.PuObjectRO;
import com.nhb.common.db.models.ModelFactory;
import com.nhb.common.utils.FileSystemUtils;

public class CrawlingHandler extends BaseMessageHandler {

	private ModelFactory modelFactory;

	@Override
	public void init(PuObjectRO initParams) {
		ExternalConfiguration config = getApi().getExternalConfiguration(initParams.getString("keywordsConfig", null));
		getLogger().debug("Keywords: " + config.get());
		config.addUpdateListener(new Callback<Collection<String>>() {

			@Override
			public void apply(Collection<String> keywords) {
				getLogger().debug("Keywords updated: {}", keywords);
			}
		});

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
	}

	private void initModelFactory(String modelMappingFile, String mysqlDatasource)
			throws FileNotFoundException, IOException {
		this.modelFactory = new ModelFactory(getApi().getDatabaseAdapter(mysqlDatasource));
		String filePath = FileSystemUtils.createAbsolutePathFrom("extensions", this.getExtensionName(),
				modelMappingFile);
		try (InputStream is = new FileInputStream(filePath)) {
			Properties props = new Properties();
			props.load(is);
			this.modelFactory.addClassImplMapping(props);
		}
		UrlModel urlModel = this.modelFactory.getModel(UrlModel.class.getName());
		if (!urlModel.checkTableExists()) {
			urlModel.createTable();
		}
	}

	@Override
	public void onServerReady() {

	}
}
