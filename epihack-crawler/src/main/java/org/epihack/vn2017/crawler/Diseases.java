package org.epihack.vn2017.crawler;

import java.util.Collection;
import java.util.HashSet;

import com.nhb.common.annotations.Transparent;

import lombok.Data;

@Data
public class Diseases {

	private HashSet<Disease> groupA;
	private HashSet<Disease> groupB;
	private HashSet<Disease> groupC;

	@Transparent
	private transient Collection<Disease> diseases;

	public Collection<Disease> getDiseases() {
		if (diseases == null) {
			diseases = new HashSet<>();
			diseases.addAll(this.groupA);
			diseases.addAll(this.groupB);
			diseases.addAll(this.groupC);
		}
		return diseases;
	};
}
