package org.rapidprom.operators.ba;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.LogService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JList;
import javax.swing.JOptionPane;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.rapidprom.ba.Prozess;
import org.rapidprom.ba.XLogUtil;
import org.rapidprom.ioobjects.XLogIOObject;

/**
 * This operator is used to add the "Wertschöpfungsgrad" to the events in the
 * log. It identifies all unique activity names and makes you choose a
 * wertschöpfungsgrad for them.
 * 
 * @author Hendrik
 */
public class WertschoepfungsgradOperator extends Operator {

	public enum WertschoperfungsGrad {
		WERTSCHOEPFEND("Wertschöpfend"), BEDINGT_WERTSCHOEPFEND("Bedingt wertschöpfend"),
		VERSCHWENDUNG("Verschwendung");

		private final String format;

		private WertschoperfungsGrad(String format) {
			this.format = format;
		}

		@Override
		public String toString() {
			return format;
		}

		public static String[] stringValues() {
			String[] values = new String[values().length];
			for (int i = 0; i < values.length; i++) {
				values[i] = values()[i].toString();
			}
			return values;
		}
	}

	public static final String PARAMETER_WERTSCHOEPFUNG_KEY = "wertschöpfung key";
	public static final String PARAMETER_WERTSCHOEPFUNG_DEFAULT = Prozess.WERTSCHOEPFUNGSGRAD_KEY;
	public static final String PARAMETER_WERTSCHOEPFUNG_DESC = "This key will be used when searching in events for the attribute that contains information about Wertschöpfungsgrad";

	private InputPort inputXLog = getInputPorts().createPort("event log (ProM Event Log)", XLogIOObject.class);

	private OutputPort outputXLog = getOutputPorts().createPort("event log (ProM Event Log)");

	public WertschoepfungsgradOperator(OperatorDescription descrption) {
		super(descrption);
		getTransformer().addRule(new GenerateNewMDRule(outputXLog, XLogIOObject.class));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeString(PARAMETER_WERTSCHOEPFUNG_KEY, PARAMETER_WERTSCHOEPFUNG_DESC,
				PARAMETER_WERTSCHOEPFUNG_DEFAULT, false));

		return types;
	}

	@Override
	public void doWork() throws OperatorException {

		Logger logger = LogService.getRoot();

		XLogIOObject input = ((XLogIOObject) inputXLog.getData(XLogIOObject.class));
		XLog eventLog = input.getArtifact();

		final String WERTSCHOPEFUNG_KEY = getParameter(PARAMETER_WERTSCHOEPFUNG_KEY);
		XLogUtil.createEventLogAttribute(logger, eventLog, WERTSCHOPEFUNG_KEY, Prozess.WERTSCHOEPFUNGSGRAD_KEY,
				"the wertschöpfungsgrad.", XLogUtil.XType.DISCRETE);

		MetaData md = inputXLog.getMetaData();

		XEventClassifier classifier = eventLog.getClassifiers().get(0);
		String classifierKey = classifier.name();
		Set<String> uniqueActivities = eventLog.stream().flatMap(List::stream)
				.map((XEvent event) -> ((XAttributeLiteralImpl) event.getAttributes().get(classifierKey)).getValue())
				.collect(Collectors.toSet());
		Map<String, XAttribute> activityToWertschoepfung = new HashMap<String, XAttribute>();
		for (String uniqueActivity : uniqueActivities) {
			Object selection = JOptionPane.showInputDialog(null,
					"Please select Wertschöpfungsgrad for activity \"" + uniqueActivity + "\".", "Wertschöpfungsgrad",
					JOptionPane.QUESTION_MESSAGE, null, WertschoperfungsGrad.stringValues(),
					WertschoperfungsGrad.stringValues()[1]);
			XAttributeLiteralImpl attribLit = new XAttributeLiteralImpl(WERTSCHOPEFUNG_KEY, (String) selection);
			activityToWertschoepfung.put(uniqueActivity, attribLit);

		}

		eventLog.stream().flatMap(List::stream).forEach((XEvent event) -> {
			event.getAttributes().put(WERTSCHOPEFUNG_KEY, activityToWertschoepfung
					.get(((XAttributeLiteralImpl) event.getAttributes().get(classifierKey)).getValue()));
		});

		XLogIOObject xLogIOObject = new XLogIOObject(eventLog, input.getPluginContext());

		outputXLog.deliverMD(md);
		outputXLog.deliver(xLogIOObject);

	}
}
