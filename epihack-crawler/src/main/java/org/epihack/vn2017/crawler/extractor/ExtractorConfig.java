package org.epihack.vn2017.crawler.extractor;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractorConfig {

	private String handler;

	private String urlPrefix;
	private Map<String, Object> config;
}
