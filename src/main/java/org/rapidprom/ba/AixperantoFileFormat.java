package org.rapidprom.ba;

public enum AixperantoFileFormat {
	ZIP("zip");

	private final String format;

	private AixperantoFileFormat(String format) {
		this.format = format;
	}

	@Override
	public String toString() {
		return format;
	}

}
