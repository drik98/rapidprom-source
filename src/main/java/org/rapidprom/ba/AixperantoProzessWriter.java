package org.rapidprom.ba;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AixperantoProzessWriter implements AixperantoContentWriter<Prozess> {

	@Override
	public String[] getFirstLine() {
		return new String[]{"kurzbezeichnung","icon1","icon2","icon3","typ","beschreibung","abteilung","eingang","ausgang","uebergang","hilfsmittel","oee","zeit","kosten"};
	}

	@Override
	public List<String[]> getContent(Collection<Prozess> items) {
		return items.stream().map(Prozess::toCSV).collect(Collectors.toList());
	}

	@Override
	public String getFileName() {
		return "prozesse.csv";
	}

}
