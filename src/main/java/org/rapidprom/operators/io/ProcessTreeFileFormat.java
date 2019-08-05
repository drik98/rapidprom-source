package org.rapidprom.operators.io;

public enum ProcessTreeFileFormat {
	PTML("ptml");

	private final String format;

	private ProcessTreeFileFormat(String format) {
		this.format = format;
	}
	
	@Override
	public String toString() {
		return format;
	}
}
