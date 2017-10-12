package org.epihack.vn2017.crawler.db.bean;

import com.nhb.common.data.PuObject;

import lombok.Data;

@Data
public class DiseaseNumCases {

	private String name;
	private int numberOfCases;

	public PuObject toPuObject() {
		PuObject puo = new PuObject();
		puo.set("name", this.getName());
		puo.set("id", this.getName());
		puo.set("numberOfCases", this.getNumberOfCases());
		return puo;
	}
}
