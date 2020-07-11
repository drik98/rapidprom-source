package org.rapidprom.ba;

import java.util.ArrayList;
import java.util.List;

public class Prozess {
	public final String kurzbezeichnung;
	public Integer icon1 = null;
	public Integer icon2 = null;
	public Integer icon3 = null;
	public Integer typ = 0;
	public String beschreibung = "";
	public String abteilung;
	public String eingang = "";
	public String ausgang = "";
	public String uebergang = ""; // uebergangszeit
	public String zeit = ""; // prozesszeit
	public String hilfsmittel = "";
	public String oee = "";
	public String kosten = "";

	public List<Prozess> nachfolger = new ArrayList();

	public static final String SEPERATOR = ";";
	public static final String WERTSCHOEPFUNGSGRAD_KEY = "Wertschöpfung";
	public static final String ORGANIZATIONALUNIT_KEY = "Abteilung";
	public static final String COSTS_KEY = "Costs";
	public static final String PROCESSING_TIME_KEY = "Processingtime";
	public static final String END_TIMESTAMP_KEY = "end:timestamp";
	public static final String START_TIMESTAMP_KEY = "time:timestamp";
	public static final String TRANSITION_TIME_KEY = "Transitiontime";

	public Prozess(String beschreibung) {
		this.beschreibung = beschreibung;
		this.kurzbezeichnung = beschreibung.replaceAll("\\s", "");
	}

	public String[] toCSV() {
		return new String[] { this.kurzbezeichnung, String.valueOf(this.icon1), String.valueOf(this.icon2),
				String.valueOf(this.icon3), String.valueOf(this.typ), this.beschreibung, this.abteilung, this.eingang,
				this.ausgang, this.uebergang, this.hilfsmittel, this.oee, this.zeit, this.kosten };
	}
}
