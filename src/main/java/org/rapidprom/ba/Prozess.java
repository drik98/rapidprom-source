package org.rapidprom.ba;

import java.util.ArrayList;
import java.util.List;

public class Prozess {
	public final String kurzbezeichnung;
	public Integer icon1 = null;
	public Integer icon2 = null;
	public Integer icon3 = null;
	public Integer typ = 0;
	public String beschreibung ="";
	public String abteilung;
	public String eingang = "";
	public String ausgang = "";
	public String uebergang = "";
	public String hilfsmittel = "";
	public String oee = "";

	public List<Prozess> nachfolger = new ArrayList();

	public static final String SEPERATOR = ";";

	public Prozess(String beschreibung) {
		this.beschreibung = beschreibung;
		this.kurzbezeichnung = beschreibung.replaceAll("\\s", "");
	}

	public String[] toCSV() {
		return new String[] {
				this.kurzbezeichnung, String.valueOf(this.icon1), String.valueOf(this.icon2),
				String.valueOf(this.icon3), String.valueOf(this.typ), this.beschreibung, this.abteilung, this.eingang,
				this.ausgang, this.uebergang, this.hilfsmittel, this.oee
		};
	}
}
