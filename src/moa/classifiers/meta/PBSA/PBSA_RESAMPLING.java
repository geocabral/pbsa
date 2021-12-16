package moa.classifiers.meta.PBSA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.Classifier;
import moa.classifiers.meta.OzaBag;
import moa.core.Measurement;
import moa.core.MiscUtils;

public class PBSA_RESAMPLING extends OzaBag {


	// orb parameters (ws th l0 l1 m n)
	public int predictionsWindowSize = 100;
	public double th;
	public double l0;
	public double l1;
	public double m;

	// the index of the time stamp. 
	public int idxTimestamp = -1;

	/*
	 * In the initial phase of the training process, examples from the clean class are absent since the 
	 * model waits w days to assign the label clean to a training example. However, many examples of the defect-inducing
	 * class are discovered in less than w days and become available for training. Since the defect inducing class is the minority class, training 
	 * on these examples would cause the model to predict all the incoming commits to be predicted as defect-inducing. Therefore, the adopted
	 * strategy to mitigate this problem was to create a pool to store the initial training examples of the defect-inducing class and release them
	 * for testing maintaining the class balance, i.e., for each clean example used for training a defect inducing example from the pool is also 
	 * trained and removed from this pool. This avoids the model to be skewed towards the defect-inducing class in the begin of the data stream.   
	 */
	public ArrayList<Instance> poolInitialDefectiveInstances = new ArrayList<>();

	/*
	 * This pool stores all defect inducing training instances in order to retrain the classifier on them when needed. 
	 */
	public ArrayList<Instance> poolDefs = new ArrayList<>();

	
	/*
	 * Sometimes, usually in the begin of the data stream, the model yields a vote vector containing only one element. This may cause 
	 * a malfunction since the likelihood for both classes must be available at each new predicted example. This variable is used to mitigate this problem.
	 */
	public double[] vote;


	/*
	 * This array stores already trained clean examples as reference to decide whether a new training example
	 * is noisy or not. Array introduced in the ORB.
	 */

	/*
	 * Array containing the last predictions in order to know whether the model predictions are skewed towards the clean or
	 * defect-inducing class.
	 */
	public ArrayList<Integer> pastPredictions = new ArrayList<>();


	/*
	 * Auxiliary variables
	 */
	public double[] votes = { 0.0, 0.0 };
	public static int currentTestInstanceIndex = 0;
	protected long firstTimeStamp = -100;
	public long currentTimeStamp = 0l;
	public int idxTr = 0;

	private static final long serialVersionUID = 1L;

	public FloatOption theta = new FloatOption("theta", 't', "The time decay factor for class size.", 0.9, 0, 1);

	protected double classSize[]; // time-decayed size of each class

	@Override
	public String getPurposeString() {
		return "Resampling method implementing the ORB Cabral(ICSE'19) and auxiliary methods to be used by the PBSA.";
	}

	public PBSA_RESAMPLING() {
		super();
		classSize = null;
	}

	
	@Override
	public void trainOnInstanceImpl(Instance inst) {

		if (inst.classValue() == 1) {
			poolInitialDefectiveInstances.add(inst.copy());
		}

		if (inst.classValue() == 0) {
			trainModel(inst);

			if (!poolInitialDefectiveInstances.isEmpty()) {
				trainModel(poolInitialDefectiveInstances.get(0));
				poolInitialDefectiveInstances.remove(0);
			}
		}

	}

	public void trainModel(Instance inst) {

		updateClassSize(inst);

		if (inst.classValue() != 0) {
			poolDefs.add(inst.copy());
		} 
		
		inst.deleteAttributeAt(idxTimestamp); // remove the time
												// stamp before
												// using the

		double lambda = calculatePoissonLambda(inst);
		int k = MiscUtils.poisson(lambda, this.classifierRandom);


		Instance weightedInst = (Instance) inst.copy();
		weightedInst.setWeight(inst.weight() * k);
		super.trainOnInstanceImpl(weightedInst);

	}

	public double getPredAvg() {
		Double average = pastPredictions.stream().mapToInt(val -> val).average().orElse(0.0);

		return average;
	}

	public double getOBFPredAvg() {

		double obf = 1;

		Double average = pastPredictions.stream().mapToInt(val -> val).average().orElse(0.0);

		double threshold = th;

		// oversampling class 1
		if (average < threshold) {

			double y = m;

			average = Math.abs(average - threshold);

			obf = ((Math.pow(y, (average * 10)) - 1) / (Math.pow(y, threshold * 10) - 1) * l1) + 1;

			// oversampling class 0
		} else {

			double y = m;

			obf = ((Math.pow(y, (average * 10)) - Math.pow(y, (threshold * 10)))
					/ (Math.pow(y, (10)) - Math.pow(y, (threshold * 10))) * l0) + 1;

			obf = obf * -1;
		}

		//in the case the obf value is less than 0, obf becomes 1 in order to not discard any
		//training example
		if (Math.abs(obf) < 1) {
			obf = 1;
		}

		return obf;
	}
	

	@Override
	public double[] getVotesForInstance(Instance inst) {

		double[] combinedVote = super.getVotesForInstance(inst);

		if (combinedVote.length == 2) {
			if (combinedVote[0] > combinedVote[1]) {
				pastPredictions.add(0);
			} else {
				pastPredictions.add(1);
			}

			if (pastPredictions.size() > predictionsWindowSize) {
				pastPredictions.remove(0);
			}
		} else {
			pastPredictions.add(0);
			if (pastPredictions.size() > predictionsWindowSize) {
				pastPredictions.remove(0);
			}
		}

		vote = new double[2];

		if (combinedVote.length == 2 && combinedVote[1] > combinedVote[0]) {
			vote[0] = 0;
			vote[1] = 1;
		} else {
			vote[0] = 1;
			vote[1] = 0;
		}

		return combinedVote;
	}

	
	public double[] getVotes(Instance inst) {
		double[] combinedVote = super.getVotesForInstance(inst);

		return combinedVote;
	}

	protected void updateClassSize(Instance inst) {
		if (this.classSize == null) {
			classSize = new double[inst.numClasses()];

			for (int i = 0; i < classSize.length; ++i) {
				classSize[i] = 1d / classSize.length;
			}
		}

		for (int i = 0; i < classSize.length; ++i) {
			classSize[i] = theta.getValue() * classSize[i]
					+ (1d - theta.getValue()) * ((int) inst.classValue() == i ? 1d : 0d);
		}
	}

	// classInstance is the class corresponding to the instance for which we
	// want to calculate lambda
	// will result in an error if classSize is not initialised yet
	// OVERSAMPLING
	public double calculatePoissonLambda(Instance inst) {
		double lambda = 1d;
		int majClass = getMajorityClass();

		lambda = classSize[majClass] / classSize[(int) inst.classValue()];

		return lambda;
	}

	// will result in an error if classSize is not initialised yet
	public int getMajorityClass() {
		int indexMaj = 0;

		for (int i = 1; i < classSize.length; ++i) {
			if (classSize[i] > classSize[indexMaj]) {
				indexMaj = i;
			}
		}
		return indexMaj;
	}

	// will result in an error if classSize is not initialised yet
	public int getMinorityClass() {
		int indexMin = 0;

		for (int i = 1; i < classSize.length; ++i) {
			if (classSize[i] <= classSize[indexMin]) {
				indexMin = i;
			}
		}
		return indexMin;
	}

	public void trainModelRecoverCD(Instance inst) {

		if (inst.classValue() == 0) {
			inst.deleteAttributeAt(idxTimestamp); // remove the time
		} // stamp before
			// using the

		Instance weightedInst = (Instance) inst.copy();
		weightedInst.setWeight(inst.weight());
		super.trainOnInstanceImpl(weightedInst);

	}

	// will result in an error if classSize is not initialised yet
	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		Measurement[] measure = super.getModelMeasurementsImpl();
		Measurement[] measurePlus = null;

		if (classSize != null) {
			measurePlus = new Measurement[measure.length + classSize.length + 2];

			for (int i = 0; i < measure.length; ++i) {
				measurePlus[i] = measure[i];
			}

			for (int i = 0; i < classSize.length; ++i) {
				String str = "size of class " + i;
				measurePlus[measure.length + i] = new Measurement(str, classSize[i]);
			}

			for (int i = 0; i < classSize.length; ++i) {
				String str = "vote for class " + i;
				measurePlus[measure.length + 2 + i] = new Measurement(str, vote[i]);
			}

		} else {
			measurePlus = new Measurement[measure.length + 4];
			for (int i = 0; i < measure.length; ++i) {
				measurePlus[i] = measure[i];
			}

			for (int i = 0; i < 2; ++i) {
				String str = "size of class " + i;
				measurePlus[measure.length + i] = new Measurement(str, 0);
			}

			for (int i = 0; i < 2; ++i) {
				String str = "vote for class " + i;
				measurePlus[measure.length + 2 + i] = new Measurement(str, votes[i]);
			}
		}

		return measurePlus;
	}

	@Override
	public boolean isRandomizable() {
		return true;
	}

}
