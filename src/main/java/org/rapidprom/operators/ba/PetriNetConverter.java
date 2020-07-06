package org.rapidprom.operators.ba;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.DummyPortPairExtender;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.tools.LogService;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.rapidprom.ioobjects.PetriNetIOObject;

/**
 *
 * @author Hendrik
 */
public class PetriNetConverter extends Operator {

    private InputPort inputPetriNet = getInputPorts()
            .createPort("model (ProM Petri Net)", PetriNetIOObject.class);

    private final PortPairExtender dummyPorts
            = new DummyPortPairExtender("through", getInputPorts(),
                    getOutputPorts());

    private static final String ABTEILUNG_KEY = "Abteilung";

    public PetriNetConverter(OperatorDescription descrption) {
        super(descrption);
        dummyPorts.start();
        getTransformer().addRule(dummyPorts.makePassThroughRule());
    }

    @Override
    public void doWork() throws OperatorException {

        Logger logger = LogService.getRoot();

        PetriNetIOObject input = ((PetriNetIOObject) inputPetriNet.getData(PetriNetIOObject.class));
        Petrinet net = input.getArtifact();

        logger.log(Level.INFO, net.getPlaces().toString());
        logger.log(Level.INFO, net.getTransitions().toString());
        for (Transition place : net.getTransitions()) {
            Set<String> keySet = place.getAttributeMap().keySet();
            logger.log(Level.INFO, place.getLabel());
            logger.log(Level.INFO, place.toString());
            for (String key : keySet) {
                logger.log(Level.INFO, place.getAttributeMap().get(key).toString());
            }
        }

        // PASS THROUGH PORTS
        dummyPorts.passDataThrough();
    }
}
