package org.epihack.vn2017.crawler;

import java.util.HashSet;

import lombok.Data;

@Data
public class Disease {
	private String name;
	private HashSet<String> keywords;
}