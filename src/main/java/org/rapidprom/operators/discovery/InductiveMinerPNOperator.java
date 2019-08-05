package org.rapidprom.operators.discovery;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.framework.packages.PackageManager.Canceller;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.inductiveminer2.mining.MiningParameters;
import org.processmining.plugins.inductiveminer2.plugins.InductiveMinerPlugin;
import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTreeReduce.ReductionFailedException;
import org.processmining.plugins.InductiveMiner.efficienttree.UnknownTreeNodeException;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.PetriNetIOObject;
import org.rapidprom.operators.abstr.AbstractInductiveMinerOperator;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.tools.LogService;

public class InductiveMinerPNOperator extends AbstractInductiveMinerOperator {

	OutputPort output = getOutputPorts().createPort("model (ProM Petri Net)");

	public InductiveMinerPNOperator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new GenerateNewMDRule(output, PetriNetIOObject.class));
	}

	public void doWork() throws OperatorException {

		Logger logger = LogService.getRoot();
		logger.log(Level.INFO, "Start: inductive miner - pn");
		long time = System.currentTimeMillis();

		PluginContext pluginContext = RapidProMGlobalContext.instance().getPluginContext();
		MiningParameters param = getConfiguration();
		
		AcceptingPetriNet accNet = null;
		try {
			accNet = InductiveMinerPlugin.minePetriNet(param.getIMLog(getXLog()), param, new Canceller() {
				@Override
				public boolean isCancelled() {
					return false;
				}
			});
		} catch (UnknownTreeNodeException | ReductionFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Object[] result = IMPetriNet.minePetriNet(pluginContext, getXLog(), param);

		Petrinet net = (Petrinet) accNet.getNet();
		Marking initialMarking = accNet.getInitialMarking();
		Marking finalMarking = (Marking) accNet.getFinalMarkings().toArray()[0];

		if (getLabel() != null && !getLabel().isEmpty()) {
			net.getAttributeMap().put(AttributeMap.LABEL, getLabel());
		}
		output.deliver(new PetriNetIOObject(net, initialMarking, finalMarking, pluginContext));

		logger.log(Level.INFO, "End: inductive miner - pn (" + (System.currentTimeMillis() - time) / 1000 + " sec)");
	}

}
