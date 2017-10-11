package org.epihack.vn2017.crawler.extractor;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import com.nhb.common.annotations.Transparent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Article {

	private Date time;
	private String title;
	private String shortDescription;
	private String content;

	@Transparent
	private final Collection<String> relatedUrls = new HashSet<>();
}
