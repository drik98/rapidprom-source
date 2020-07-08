package org.rapidprom.ba;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AixperantoNachfolgerWriter implements AixperantoContentWriter<Prozess> {

	@Override
	public String[] getFirstLine() {
		return new String[] { "vorgaenger", "nachfolger", "beschreibung" };
	}

	@Override
	public List<String[]> getContent(Collection<Prozess> items) {
		List<String[]> csvContent = new ArrayList();
		for (Prozess p : items) {
			for (Prozess nachfolger : p.nachfolger) {
				csvContent.add(new String[] { p.kurzbezeichnung, nachfolger.kurzbezeichnung, "" });
			}
		}
		return csvContent;
	}

	@Override
	public String getFileName() {
		return "nachfolger.csv";
	}

}
