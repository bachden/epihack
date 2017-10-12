package org.epihack.vn2017.crawler;

import lombok.Getter;

public enum Status {

	OK(0, "ok"),
	UNKNOWN_ERROR(1, "unknown error"),
	MISSING_COMMAND(2, "missing command"),
	INTERNAL_SERVER_ERROR(3, "internal server error"),
	INVALID_PARAMETER(4, "invalid parameter"),
	ERROR(5, "error");

	@Getter
	private final int code;

	@Getter
	private final String message;

	private Status(int code, String message) {
		this.code = code;
		this.message = message;
	}
}
