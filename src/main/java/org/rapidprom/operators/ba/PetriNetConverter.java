package org.rapidprom.operators.ba;

import com.google.gwt.dev.util.collect.HashMap;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.impl.XAttributeContinuousImpl;
import org.deckfour.xes.model.impl.XAttributeDiscreteImpl;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;
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
import org.rapidprom.ba.XLogUtil;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.PetriNetIOObject;
import org.rapidprom.ioobjects.XLogIOObject;
import org.rapidprom.operators.abstr.AbstractRapidProMExporterOperator;

/**
 * This operator takes a petrinet and the eventlog that it was created with and
 * converts it to a zip-file that can be loaded into aixperanto to visualize the
 * process model
 * 
 * @author Hendrik
 */
public class PetriNetConverter
		extends AbstractRapidProMExporterOperator<PetriNetIOObject, Petrinet, AixperantoFileFormat> {

	private InputPort inputXLog = getInputPorts().createPort("event log (ProM Event Log)", XLogIOObject.class);

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
			try {
				exportAixperantoZIP(petrinet, file);
			} catch (OperatorException e) {
				throw new IOException(e);
			}
			break;
		}
	}

	private void exportAixperantoZIP(Petrinet petrinet, File file) throws IOException, OperatorException {

		Logger logger = LogService.getRoot();

		XLogIOObject input = ((XLogIOObject) inputXLog.getData(XLogIOObject.class));
		XLog eventLog = input.getArtifact();
		
		final String ORGANIZATIONALUNIT_KEY = XLogUtil.getAttributeKey(eventLog, Prozess.ORGANIZATIONALUNIT_KEY);
		final String COSTS_KEY = Prozess.COSTS_KEY;
		final String WERTSCHOEPFUNGSGRAD_KEY = XLogUtil.getAttributeKey(eventLog, Prozess.WERTSCHOEPFUNGSGRAD_KEY);
		final String PROCESSING_TIME_KEY = XLogUtil.getAttributeKey(eventLog, Prozess.PROCESSING_TIME_KEY);
		final String TRANSITION_TIME_KEY = XLogUtil.getAttributeKey(eventLog, Prozess.TRANSITION_TIME_KEY);
		
		XEventClassifier classifier = eventLog.getClassifiers().get(0);
		String classifierKey = classifier.name();
		logger.log(Level.INFO, "classifierKey:\t" + classifierKey);

		List<XEvent> xevents = eventLog.stream().flatMap(List::stream).collect(Collectors.toList());

		Map<String, Prozess> prozesse = new HashMap();

		for (Transition transition : petrinet.getTransitions()) {

			String activity = transition.getLabel();
			logger.log(Level.INFO, activity);

			Prozess prozess = new Prozess(activity);
			prozesse.put(transition.getLabel(), prozess);

			// find any event from the eventlog with given activity name
			List<XEvent> eventsWithActitvityName = xevents.stream().filter((XEvent evt) -> {
				XAttributeLiteralImpl activityXAttribute = (XAttributeLiteralImpl) evt.getAttributes()
						.get(classifierKey);
				String acvityFromEvent = activityXAttribute.getValue();
				return activity.equals(acvityFromEvent);
			}).collect(Collectors.toList());

			// list cant possibly be empty, still checking just in case so program doesnt
			// crash
			if (eventsWithActitvityName.isEmpty()) {
				logger.log(Level.INFO, "No events found in eventlog with classifier name :\t" + activity);
				continue;
			}

			XEvent firstEvent = eventsWithActitvityName.get(0);

			// set abteilung for prozessschritt
			XAttributeLiteralImpl abteilungAttribute = (XAttributeLiteralImpl) firstEvent.getAttributes()
					.get(ORGANIZATIONALUNIT_KEY);
			String abteilung = abteilungAttribute != null ? abteilungAttribute.toString() : "";
			prozess.abteilung = abteilung;

			// set costs from average costs of all acitivites
			Double costs = eventsWithActitvityName.stream().map((XEvent evt) -> {
				XAttributeLiteralImpl costsAttribute = (XAttributeLiteralImpl) evt.getAttributes()
						.get(COSTS_KEY);
				if (costsAttribute != null && !costsAttribute.getValue().isEmpty()) {
					return Double.valueOf(costsAttribute.getValue());
				} else {
					return 0.0;
				}
			}).reduce(Double::sum).get() / eventsWithActitvityName.size();
			prozess.kosten = String.valueOf(costs);

			// set wertschopefungsgrad for prozesschritt from first event
			XAttributeLiteralImpl wertschoepfungsgradAttribut = (XAttributeLiteralImpl) firstEvent.getAttributes()
					.get(WERTSCHOEPFUNGSGRAD_KEY);
			String wertschoperfungsgrad = wertschoepfungsgradAttribut != null ? wertschoepfungsgradAttribut.getValue()
					: "";

			switch (wertschoperfungsgrad.toLowerCase(Locale.GERMAN)) {
			case "wertschöpfend":
			case "wertschoepfend":
				prozess.icon2 = 0;
				break;
			case "verschwendung":
				prozess.icon2 = 2;
				break;
			case "bedingt wertschoepfend":
			case "bedingt wertschöpfend":
			default:
				prozess.icon2 = 1;
			}

			Long sumTimeMilliseconds = 0L;
			Long sumUebergangsZeitMilliseconds = 0L;

			for (XEvent event : eventsWithActitvityName) {
				XAttributeDiscreteImpl zeitXAttribute = (XAttributeDiscreteImpl) event.getAttributes()
						.get(PROCESSING_TIME_KEY);
				XAttributeDiscreteImpl uebergangXAttribute = (XAttributeDiscreteImpl) event.getAttributes()
						.get(TRANSITION_TIME_KEY);

				if (zeitXAttribute != null) {
					sumTimeMilliseconds += zeitXAttribute.getValue();
				}
				if (uebergangXAttribute != null) {
					sumUebergangsZeitMilliseconds += uebergangXAttribute.getValue();
				}

			}

			prozess.zeit = String.valueOf(sumTimeMilliseconds / eventsWithActitvityName.size());
			prozess.uebergang = String.valueOf(sumUebergangsZeitMilliseconds / eventsWithActitvityName.size());
		}

		for (Transition transition : petrinet.getTransitions()) {
			Prozess prozess = prozesse.get(transition.getLabel());

			Collection<Transition> nachfolger = transition.getVisibleSuccessors();
			Prozess dummy = prozess;
			if (nachfolger.size() > 1) {
				String bezeichnung = String.join("-",
						nachfolger.stream().map(Transition::getLabel).collect(Collectors.toList()));
				Entscheidung entscheidung = new Entscheidung(bezeichnung);
				entscheidung.beschreibung = "";
				entscheidung.abteilung = prozess.abteilung;
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
