package org.rapidprom.ba;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AixperantoParallelWriter implements AixperantoContentWriter {

	@Override
	public String[] getFirstLine() {
		return new String[] { "kurzbezeichnung", "beschreibung" };
	}

	@Override
	public List<String[]> getContent(Collection items) {
		return new ArrayList<String[]>();
	}

	@Override
	public String getFileName() {
		return "parallel.csv";
	}

}
