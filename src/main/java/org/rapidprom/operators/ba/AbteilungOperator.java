package org.rapidprom.operators.ba;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.rapidprom.ioobjects.XLogIOObject;

/**
 *
 * @author Hendrik
 */
public class AbteilungOperator extends Operator {

    private InputPort inputXLog = getInputPorts()
            .createPort("event log (ProM Event Log)", XLogIOObject.class);

    private OutputPort outputXLog = getOutputPorts()
            .createPort("event log (ProM Event Log)");

    private static final String ABTEILUNG_KEY = "Abteilung";

    public AbteilungOperator(OperatorDescription descrption) {
        super(descrption);

    }

    @Override
    public void doWork() throws OperatorException {

        XLogIOObject input = ((XLogIOObject) inputXLog.getData(XLogIOObject.class));
        XLog eventLog = input.getArtifact();
        XEventClassifier classifier = eventLog.getClassifiers().get(0);
        String classifierKey = classifier.name();
        Set<String> uniqueActivities = eventLog
                .stream()
                .flatMap(List::stream)
                .map((XEvent event)
                        -> event
                        .getAttributes()
                        .get(classifierKey)
                        .toString())
                .collect(Collectors.toSet());
        Map<String, XAttribute> activityToAbteilung = new HashMap<String, XAttribute>();
        for (String uniqueActivity : uniqueActivities) {
            String abteilung = JOptionPane.showInputDialog("Abteilung für Aktivität \"" + uniqueActivity + "\" angeben.");
            XAttributeLiteralImpl attribLit = new XAttributeLiteralImpl(ABTEILUNG_KEY,
                    abteilung);
            activityToAbteilung.put(uniqueActivity, attribLit);

        }

        eventLog
                .stream()
                .flatMap(List::stream)
                .forEach((XEvent event) -> {
                    event.getAttributes().put(
                            ABTEILUNG_KEY,
                            activityToAbteilung.get(event.getAttributes().get(classifierKey).toString()));
                });

        XLogIOObject xLogIOObject = new XLogIOObject(eventLog, input.getPluginContext());

        outputXLog.deliver(xLogIOObject);

    }
}
