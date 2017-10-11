package org.epihack.vn2017.crawler;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;

import com.nhb.common.Loggable;
import com.nhb.common.async.Callback;
import com.nhb.messaging.http.HttpAsyncFuture;
import com.nhb.messaging.http.HttpClientHelper;

public class HtmlFetcher implements Loggable {

	private final HttpClientHelper httpClient = new HttpClientHelper();

	public void fetch(final String url, final Callback<String> callback) {
		final HttpAsyncFuture future = httpClient.executeAsyncGet(url, null);
		future.setCallback(new Callback<HttpResponse>() {

			@Override
			public void apply(HttpResponse response) {
				if (response == null) {
					getLogger().error("Error while fetching html from url: {}", url, future.getFailedCause());
					if (callback != null) {
						callback.apply(null);
					}
				} else {
					try {
						String responseHtml = EntityUtils.toString(response.getEntity(), "utf-8");
						if (callback != null) {
							callback.apply(responseHtml);
						}
					} catch (ParseException | IOException e) {
						getLogger().error("Error while handling response data from url: {}", url);
					}
				}
			}
		});
		future.setTimeout(1, TimeUnit.MINUTES);
	}

	public void shutdown() {
		try {
			this.httpClient.close();
		} catch (IOException e) {
			getLogger().error("Cannot close http client", e);
		}
	}
}
