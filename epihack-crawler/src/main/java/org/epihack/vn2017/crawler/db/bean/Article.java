package org.epihack.vn2017.crawler.db.bean;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import com.nhb.common.annotations.Transparent;
import com.nhb.common.data.PuArray;
import com.nhb.common.data.PuArrayList;
import com.nhb.common.data.PuObject;
import com.nhb.common.db.beans.UUIDBean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Article extends UUIDBean {

	private static final long serialVersionUID = 5338622715385201294L;

	@Builder.Default
	private int status = 0; // 0 for new, 1 is accepted, 2 is rejected, 3 is ignored

	private Date time;
	private String title;
	private String shortDescription;
	private String content;
	private String url;
	private String province;
	private String disease;
	private String group;

	public long getTimestamp() {
		if (this.getTime() == null) {
			return -1l;
		}
		return time.getTime();
	}

	@Transparent
	private final transient Collection<String> relatedUrls = new HashSet<>();

	public PuObject toPuObject() {
		PuArray detected = new PuArrayList();

		PuObject puo = new PuObject();
		puo.set("headline", this.getTitle());
		puo.set("articleId", this.getUuidString());
		puo.set("content", this.getContent());
		puo.set("provinceName", this.getProvince());
		puo.set("time", this.getTimestamp());
		puo.set("url", this.getUrl());
		puo.set("detected", detected);

		return puo;
	}
}
