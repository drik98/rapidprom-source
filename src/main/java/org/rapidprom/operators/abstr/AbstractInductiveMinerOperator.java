package org.rapidprom.operators.abstr;

import java.util.List;

import org.processmining.plugins.inductiveminer2.mining.MiningParameters;
import org.processmining.plugins.inductiveminer2.mining.MiningParametersAbstract;
import org.processmining.plugins.inductiveminer2.variants.MiningParametersIM;
import org.processmining.plugins.inductiveminer2.variants.MiningParametersIMInfrequent;
import org.processmining.plugins.inductiveminer2.variants.MiningParametersIMLifeCycle;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualStringCondition;

public abstract class AbstractInductiveMinerOperator extends AbstractLabelAwareRapidProMDiscoveryOperator {

	public AbstractInductiveMinerOperator(OperatorDescription description) {
		super(description);
	}

	private static final String PARAMETER_KEY_VARIATION = "Variation",
			PARAMETER_DESCR__VARIATION = "The \"Inductive Miner\" variation is described in: "
					+ "http://dx.doi.org/10.1007/978-3-642-38697-8_17. \nThe \"Inductive Miner"
					+ " - Infrequent\" variation is described in: "
					+ "http://dx.doi.org/10.1007/978-3-319-06257-0_6. \nThe \"Inductive Miner"
					+ " - Incompleteness\" variation is described in:"
					+ "http://dx.doi.org/10.1007/978-3-319-07734-5_6. \nThe \"Inductive Miner"
					+ " - exhaustive K-successor\" variation applies a brute-force approach: "
					+ "in each recursion, it tries all 4*2^n cuts and measures which one "
					+ "fits the event log best. It measures this using the k-successor, "
					+ "which is a relation between pairs of activities, denoting how many "
					+ "events are in between them in any trace at minimum."
					+ "- Life cycle variation makes use of the 'start' and 'complete' transitions in the event log.",
			PARAMETER_KEY_NOISE_THRESHOULD = "Noise Threshold",
			PARAMETER_DESCR_NOISE_THRESHOULD = "This threshold represents the percentage of infrequent (noisy) "
					+ "traces that are filtered out. The remaining traces are used to discover a model. ";

	private static final String IM = "Inductive Miner", //
			IMi = "Inductive Miner - Infrequent", //
			//IMin = "Inductive Miner - Incompleteness", //
			//IMeks = "Inductive Miner - exhaustive K-successor", //
			IMflc = "Inductive Miner - Life cycle";

	@Override
	public List<ParameterType> getParameterTypes() {

		List<ParameterType> parameterTypes = super.getParameterTypes();

		ParameterTypeCategory parameter1 = new ParameterTypeCategory(PARAMETER_KEY_VARIATION, PARAMETER_DESCR__VARIATION,
		        new String[] { IM, IMi,IMflc }, 1);
		parameterTypes.add(parameter1);

		ParameterTypeDouble parameter2 = new ParameterTypeDouble(PARAMETER_KEY_NOISE_THRESHOULD , PARAMETER_DESCR_NOISE_THRESHOULD, 0, 1, 0.2);
		
		parameter2.setOptional(true);
		parameter2.registerDependencyCondition(
				new EqualStringCondition(this, PARAMETER_KEY_VARIATION, true, new String[] { IMi }));
		parameterTypes.add(parameter2);
		
		return parameterTypes;
	}

	protected MiningParameters getConfiguration() throws UserError {
		MiningParameters miningParameters = null;
		try {
			if (getParameterAsString(PARAMETER_KEY_VARIATION).equals(IM))
				miningParameters = new MiningParametersIM();
			else if (getParameterAsString(PARAMETER_KEY_VARIATION).equals(IMi))
				miningParameters = new MiningParametersIMInfrequent();
			//else if (getParameterAsString(PARAMETER_KEY_VARIATION).equals(IMin))
			//	miningParameters = (MiningParameters) new MiningParametersIMf();
			//else if (getParameterAsString(PARAMETER_KEY_VARIATION).equals(IMeks))
			//	miningParameters = (MiningParameters) new MiningParametersEKS();
			else if (getParameterAsString(PARAMETER_KEY_VARIATION).equals(IMflc))
				miningParameters = new MiningParametersIMLifeCycle();
			else
				throw new IllegalArgumentException(
						"Unknown inductive miner type " + getParameterAsString(PARAMETER_KEY_VARIATION));

			((MiningParametersAbstract) miningParameters).setNoiseThreshold((float) getParameterAsDouble(PARAMETER_KEY_NOISE_THRESHOULD ));
			((MiningParametersAbstract) miningParameters).setClassifier(getXEventClassifier());
		} catch (UndefinedParameterError e) {
			e.printStackTrace();
		}
		return miningParameters;
	}
}
