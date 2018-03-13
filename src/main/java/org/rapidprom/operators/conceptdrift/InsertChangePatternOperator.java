package org.rapidprom.operators.conceptdrift;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.processmining.changepatterns.algorithms.modelchanges.AddFragment;
import org.processmining.changepatterns.algorithms.modelchanges.RemoveFragment;
import org.processmining.changepatterns.algorithms.modelchanges.SwapFragment;
import org.processmining.changepatterns.algorithms.modelchanges.abstr.ChangePattern;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.processtree.ProcessTree;
import org.rapidprom.external.connectors.prom.RapidProMGlobalContext;
import org.rapidprom.ioobjects.ProcessTreeIOObject;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.LogService;

public class InsertChangePatternOperator extends Operator {

	public static final String TYPE_KEY = "type", TYPE_DESCR = "Type of change pattern to be applied",
			NUMBER_KEY = "number", NUMBER_DESCR = "Number of activities to be changed";
	public static final String ADD = "add fragment", REMOVE = "remove fragment", SWAP = "swap fragment";

	private InputPort input = getInputPorts().createPort("process tree", ProcessTreeIOObject.class);
	private OutputPort output = getOutputPorts().createPort("process tree");

	public InsertChangePatternOperator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new GenerateNewMDRule(output, ProcessTreeIOObject.class));
	}

	public void doWork() throws OperatorException {
		Logger logger = LogService.getRoot();
		logger.log(Level.INFO, "Start: applying change pattern to process tree");
		long time = System.currentTimeMillis();

		PluginContext pluginContext = RapidProMGlobalContext.instance().getPluginContext();

		ChangePattern cp = null;

		switch (getParameterAsString(TYPE_KEY)) {
		case ADD:
			cp = new AddFragment();
			break;
		case REMOVE:
			cp = new RemoveFragment();
			break;
		case SWAP:
			cp = new SwapFragment();
		}

		ProcessTree pt = cp.applyChangePattern(input.getData(ProcessTreeIOObject.class).getArtifact(),
				getParameterAsInt(NUMBER_KEY));
		output.deliver(new ProcessTreeIOObject(pt, pluginContext));

		logger.log(Level.INFO, "End: applying change pattern to process tree ("
				+ (System.currentTimeMillis() - time) / 1000 + " sec)");
	}

	public List<ParameterType> getParameterTypes() {
		List<ParameterType> parameterTypes = super.getParameterTypes();

		ParameterTypeCategory par1 = new ParameterTypeCategory(TYPE_KEY, TYPE_DESCR, new String[] { ADD, REMOVE, SWAP },
				1);
		parameterTypes.add(par1);
		ParameterTypeInt par2 = new ParameterTypeInt(NUMBER_KEY, NUMBER_DESCR, 0, Integer.MAX_VALUE, 1);

		parameterTypes.add(par1);
		parameterTypes.add(par2);

		return parameterTypes;
	}

}
