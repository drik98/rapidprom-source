package org.rapidprom.operators.ba;

import com.google.gwt.dev.util.collect.HashMap;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.DummyPortPairExtender;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.tools.LogService;

import au.com.bytecode.opencsv.CSVWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.petrinets.PetriNetFileFormat;
import org.processmining.plugins.pnml.exporting.PnmlExportNetToEPNML;
import org.processmining.plugins.pnml.exporting.PnmlExportNetToPNML;
import org.rapidprom.ba.AixperantoContentWriter;
import org.rapidprom.ba.AixperantoFileFormat;
import org.rapidprom.ba.AixperantoNachfolgerWriter;
import org.rapidprom.ba.AixperantoParallelWriter;
import org.rapidprom.ba.AixperantoProzessWriter;
import org.rapidprom.ba.AixperantoSchwachstelleWriter;
import org.rapidprom.ba.Entscheidung;
import org.rapidprom.ba.Prozess;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.PetriNetIOObject;
import org.rapidprom.operators.abstr.AbstractRapidProMExporterOperator;

/**
 *
 * @author Hendrik
 */
public class PetriNetConverter
		extends AbstractRapidProMExporterOperator<PetriNetIOObject, Petrinet, AixperantoFileFormat> {

	public PetriNetConverter(OperatorDescription description) {
		super(description, PetriNetIOObject.class,
				EnumSet.allOf(AixperantoFileFormat.class)
						.toArray(new AixperantoFileFormat[EnumSet.allOf(AixperantoFileFormat.class).size()]),
				AixperantoFileFormat.ZIP);
	}

	@Override
	protected void writeToFile(File file, Petrinet petrinet, AixperantoFileFormat format) throws IOException {
		switch (format) {
		case ZIP:
		default:
			exportAixperantoZIP(petrinet, file);
			break;
		}
	}

	private void exportAixperantoZIP(Petrinet petrinet, File file) throws IOException {

		Map<String, Prozess> prozesse = new HashMap();

		Logger logger = LogService.getRoot();

		logger.log(Level.INFO, petrinet.getPlaces().toString());
		logger.log(Level.INFO, petrinet.getTransitions().toString());
		for (Transition transition : petrinet.getTransitions()) {

			Prozess prozess = new Prozess(transition.getLabel());
			prozesse.put(transition.getLabel(), prozess);

		}
		
	

		
		for (Transition transition : petrinet.getTransitions()) {
			Prozess prozess = prozesse.get(transition.getLabel());
			
			Collection<Transition> nachfolger =  transition.getVisibleSuccessors();
			Prozess dummy = prozess;
			if(nachfolger.size()>1) {
				String bezeichnung = String.join("-",nachfolger.stream().map(Transition::getLabel).collect(Collectors.toList()));
				Entscheidung entscheidung = new Entscheidung(bezeichnung);
				entscheidung.beschreibung = "";
				dummy = entscheidung;
				prozess.nachfolger.add(entscheidung);
				prozesse.put(bezeichnung, entscheidung);
			} 
			for (Transition successor : transition.getVisibleSuccessors()) {
				dummy.nachfolger.add(prozesse.get(successor.getLabel()));
			}
		}

		AixperantoContentWriter[] contentWriter = { new AixperantoParallelWriter(), new AixperantoSchwachstelleWriter(),
				new AixperantoNachfolgerWriter(), new AixperantoProzessWriter() };

		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));

		for (AixperantoContentWriter writer : contentWriter) {

			String filename = writer.getFileName();

			ZipEntry entry = new ZipEntry(filename); // create a zip entry and add it to ZipOutputStream
			zos.putNextEntry(entry);

			CSVWriter csvwriter = new CSVWriter(new OutputStreamWriter(zos), ';', CSVWriter.NO_QUOTE_CHARACTER);
			csvwriter.writeNext(writer.getFirstLine());
			csvwriter.writeAll(writer.getContent(prozesse.values()));
			csvwriter.flush(); // flush the writer. Very important!
			zos.closeEntry(); // close the entry. Note : we are not closing the zos just yet as we need to add
								// more files to our ZIP

		}
		zos.close();

	}

}
