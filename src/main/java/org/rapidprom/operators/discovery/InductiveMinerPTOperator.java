package org.rapidprom.operators.discovery;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.processmining.framework.packages.PackageManager.Canceller;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.inductiveminer2.mining.MiningParameters;
import org.processmining.plugins.inductiveminer2.plugins.InductiveMinerPlugin;
import org.processmining.processtree.ProcessTree;
import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTree2processTree;
import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTree;
import org.processmining.plugins.InductiveMiner.efficienttree.UnknownTreeNodeException;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.ProcessTreeIOObject;
import org.rapidprom.operators.abstr.AbstractInductiveMinerOperator;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.tools.LogService;

public class InductiveMinerPTOperator extends AbstractInductiveMinerOperator {

	OutputPort output = getOutputPorts().createPort("model (ProM ProcessTree)");

	public InductiveMinerPTOperator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new GenerateNewMDRule(output, ProcessTreeIOObject.class));			
	}

	public void doWork() throws OperatorException {
		Logger logger = LogService.getRoot();
		logger.log(Level.INFO, "Start: inductive miner - pt");
		long time = System.currentTimeMillis();

		PluginContext pluginContext = RapidProMGlobalContext.instance().getPluginContext();
		MiningParameters param = getConfiguration();
		
		EfficientTree effTree = null;
		try {
			effTree= InductiveMinerPlugin.mineTree(param.getIMLog(getXLog()), param, new Canceller() {
				@Override
				public boolean isCancelled() {
					return false;
				}
			});
		} catch (UnknownTreeNodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ProcessTree procTree= EfficientTree2processTree.convert(effTree);
		ProcessTreeIOObject result = new ProcessTreeIOObject(procTree,pluginContext);


		output.deliver(result);
		logger.log(Level.INFO, "End: inductive miner - pt (" + (System.currentTimeMillis() - time) / 1000 + " sec)");
	}
	
}
