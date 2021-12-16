package moa.classifiers.meta.PBSA;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.joda.time.Days;
import org.joda.time.Instant;

import com.github.javacliparser.IntOption;
import com.github.javacliparser.StringOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.core.Measurement;

/**
 * Prediction Based Sampling Adjustment (PBSA) Algorithm.
 *
 * @author George Cabral (george.gcabral@ufrpe.br)
 * @version $Revision: 1 $
 */
public class PBSA extends PBSA_RESAMPLING {

	int limitInstancesPassedAfterLastRecovering = 30;

	public ArrayList<Double> arrWindowsPredictions = new ArrayList<>();

	public int lastCDCleanClass = 0;

	public int lastSkewTowardsDefect = 0;
	public int lastSkewTowardsClean = 0;

	public double pbsaPercentageDifferenceTh;
	public int pbsaAlfa = 5;
	public int pbsaBeta = 2;

	public String params;

	@Override
	public String getPurposeString() {
		return "Prediction-Based Sampling Adjustment Algorithm.";
	}

	private static final long serialVersionUID = 1L;

	public IntOption waitingTime = new IntOption("waitingTime", 'w',
			"The time (in days) we have to wait before using a commit as a training example in order to know whether "
					+ "it led to a defect or not.",
			90, 1, Integer.MAX_VALUE);

	public IntOption unixTimeStampIndex = new IntOption("unixTimeStampIndex", 'i',
			"The index of the input attribute containing the unix time stamp when the example was created (starting from 0). "
					+ "It must be an attribute index ***after*** the class attribute index.",
			15, 0, Integer.MAX_VALUE);

	public StringOption paramORB = new StringOption("paramORB", 'p',
			"set all the parameters for the ORB (Cabral-ICSE'19)", "100;0.4;10;12;1.5;3");

	public StringOption dataset = new StringOption("paramDS", 'd', "dataset name for description purposes", "none");

	// PBSA parameters (p;alfa;beta)
	public StringOption paramPBSA = new StringOption("paramPBSA", 'a', "set all the parameters for the PBSA",
			"0.25;5;2");

	/*
	 * training examples that are waiting for waitingTime days before they are used
	 * for training
	 */
	protected ArrayList<Instance> trainingExamplesQueue = null;


	public ArrayList<Double> pastRecall0 = null;
	public ArrayList<Double> pastRecall1 = null;
	public ArrayList<Double> predictionsMAHistory = null;
	public ArrayList<Double> predictionsMAHistoryComplete = null;
	public Double thresholdMA = 10000.0;
	public Double oldGmeanMinusDiffRecalls = 0.0;
	public int smallerMeanRecallsTimeStep = 0;

	public PBSA() {
		super();
		pastRecall0 = new ArrayList<>();
		pastRecall1 = new ArrayList<>();
		predictionsMAHistory = new ArrayList<>();
		predictionsMAHistoryComplete = new ArrayList<>();
		trainingExamplesQueue = new ArrayList<Instance>(waitingTime.getValue());

	}

	private void setPbsaParams() {

		String htParam = paramPBSA.getValue();
		StringTokenizer strTok = new StringTokenizer(htParam, ";");
		this.pbsaPercentageDifferenceTh = Double.valueOf(strTok.nextToken());
		pbsaAlfa = Integer.valueOf(strTok.nextToken());
		pbsaBeta = Integer.valueOf(strTok.nextToken());

	}

	@Override
	public void resetLearningImpl() {
		super.resetLearningImpl();
		trainingExamplesQueue = new ArrayList<Instance>(waitingTime.getValue());
	}

	// This method does not directly perform any training. Instead, it stores
	// instances for training later on.
	// We update the models with only with clean examples produced at lest waitingTime
	// days ago or when defect-inducing examples are found.
	@Override
	public void trainOnInstanceImpl(Instance inst) {

		if (inst.classValue() == 1) {
			Instance trainInst = inst.copy();
			super.trainOnInstanceImpl(trainInst);

		} else {
			trainingExamplesQueue.add(inst.copy());
		}

	}

	// Method to allow classes that inherit from this one to call the super
	// method from this class
	public void superTrainOnInstanceImpl(Instance inst) {
		super.trainOnInstanceImpl(inst);
	}

	// Method to allow classes that inherit from this one to call the super
	// method from this class
	public double[] superGetVotesForInstance(Instance inst) {
		return super.getVotesForInstance(inst);
	}


	public void setORBParam() {
		String orbParam = paramORB.getValue();

		// orb parameters (ws th l0 l1 m n)
		StringTokenizer strTok = new StringTokenizer(orbParam, ";");
		this.predictionsWindowSize = Integer.valueOf(strTok.nextToken());
		this.th = Double.valueOf(strTok.nextToken());
		this.l0 = Double.valueOf(strTok.nextToken());
		this.l1 = Double.valueOf(strTok.nextToken());
		this.m = Double.valueOf(strTok.nextToken());
		
	}

	// inst is the current instance to be predicted. It is used here to
	// determine which examples
	// can already be used for training, based on the waitingTime between the
	// production of those examples
	// and of the new example to be predicted.
	protected void trainOnInstancesWaitingTime() {

		while (trainingExamplesQueue.size() != 0) {

			Instance trainingExampleToPop = trainingExamplesQueue.get(0);
			Instant timeTrainingExampleToPop = new Instant(
					(long) trainingExampleToPop.value(unixTimeStampIndex.getValue()) * 1000);

			Instant timeTestingInstance = new Instant(currentTimeStamp * 1000);

			Days daysWaited = Days.daysBetween(timeTrainingExampleToPop, timeTestingInstance);

			if (daysWaited.getDays() >= waitingTime.getValue()) {

				// if it is non defective instance, store in the array of non
				// defective reference instances
				super.trainOnInstanceImpl(trainingExampleToPop);
				trainingExamplesQueue.remove(0);

			} else { // if the current training example has less than
				// waitingTime, all examples after it will also have.
				// So, we can return.
				break;
			}

		}

	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		Measurement[] measure = super.getModelMeasurementsImpl();
		Measurement[] measurePlus = new Measurement[measure.length + 2];
		for (int i = 0; i < measure.length; ++i) {
			measurePlus[i] = measure[i];
		}

		measurePlus[measure.length] = new Measurement("training queue size", trainingExamplesQueue.size());
		measurePlus[measure.length + 1] = new Measurement("time stamp", super.currentTimeStamp);

		return measurePlus;

	}

	// given that the classifier became exceedingly skewed towards class 1 (defect-inducing), 
	// most recent instances in the queue (labeling them as clean) will be used to train the classifier
	// to make it improve its performance on the clean class. These instances will be picked up
	// based on a beta statistical distribution. 
	@SuppressWarnings("unchecked")
	public void recoverFromSkewTowardDefectInducingClass() {

		ArrayList<Instance> arrTestInstances = new ArrayList<>();

		if (trainingExamplesQueue.size() > predictionsWindowSize) {
			for (int i = (trainingExamplesQueue.size() - 1); i >= (trainingExamplesQueue.size()
					- predictionsWindowSize); i--) {
				arrTestInstances.add(trainingExamplesQueue.get(i).copy());
			}
		} else {
			arrTestInstances = (ArrayList<Instance>) trainingExamplesQueue.clone();
		}

		ArrayList<Integer> predictions = new ArrayList<>();

		BetaDistribution b = new BetaDistribution(pbsaAlfa, pbsaBeta);

		double oldAvg = 1;
		int counterNoIncrease = 0;
		double avg = 0;
		while (true) {
			int idx = Double.valueOf(b.sample() * (trainingExamplesQueue.size() - 1)).intValue();
			Instance ins = trainingExamplesQueue.get(idx).copy();
			ins.setClassValue(0);

			super.trainModelRecoverCD(ins);
			predictions = new ArrayList<>();
			for (int i = 0; i < arrTestInstances.size(); i++) {
				double[] p = super.getVotes(arrTestInstances.get(i));
				if (p.length > 1) {
					if (p[0] > p[1]) {
						predictions.add(0);
					} else {
						predictions.add(1);
					}
				} else {
					predictions.add(0);
				}

			}

			avg = predictions.stream().mapToInt(val -> val).average().orElse(0.0);

			if (oldAvg < avg || counterNoIncrease > 100) {
				break;
			} else {
				if (avg == oldAvg) {
					counterNoIncrease++;
				} else {
					oldAvg = avg;
				}
			}

			if (avg < this.th) {
				break;
			}
		}

	}

	
	
	// given that the classifier became exceedingly skewed towards class 0 (clean), 
	// most recent instances in the defect inducing pool will be used to train the classifier
	// to make it improve its performance on the defect-inducing class. These instances will be picked up
	// based on a beta statistical distribution.@SuppressWarnings("unchecked")
	public void recoverFromSkewTowardCleanClass() {

		ArrayList<Instance> arrTestInstances = new ArrayList<>();

		if (trainingExamplesQueue.size() > predictionsWindowSize) {
			for (int i = (trainingExamplesQueue.size() - 1); i >= (trainingExamplesQueue.size()
					- predictionsWindowSize); i--) {
				arrTestInstances.add(trainingExamplesQueue.get(i).copy());
			}
		} else {
			arrTestInstances = (ArrayList<Instance>) trainingExamplesQueue.clone();
		}

		ArrayList<Integer> predictions = new ArrayList<>();

		BetaDistribution b = new BetaDistribution(pbsaAlfa, pbsaBeta);

		int counterNoIncrease = 0;
		double oldAvg = 0;

		if (poolDefs.size() > 0) {
			quicksortTimestep(poolDefs, 0, poolDefs.size() - 1);
		}

		double avg = 0;

		while (true) {
			int idx = Double.valueOf(b.sample() * (poolDefs.size() - 1)).intValue();

			if (poolDefs.isEmpty()) {
				break;
			}

			Instance ins = poolDefs.get(idx).copy();

			ins.deleteAttributeAt(15);

			super.trainModelRecoverCD(ins);

			predictions = new ArrayList<>();
			for (int i = 0; i < arrTestInstances.size(); i++) {
				double[] p = super.getVotes(arrTestInstances.get(i));
				if (p.length > 1) {
					if (p[0] > p[1]) {
						predictions.add(0);
					} else {
						predictions.add(1);
					}
				} else {
					predictions.add(0);
				}

			}

			avg = predictions.stream().mapToInt(val -> val).average().orElse(0.0);

			// if oldavg is larger than the current avg, it means that the last
			// training loop
			// made the classifier more skewed toward the clean class. Then stop
			// the recovery training
			if (oldAvg > avg || counterNoIncrease > 100) {
				break;
			} else {
				if (avg == oldAvg) {
					counterNoIncrease++;
				} else {
					oldAvg = avg;
				}
			}

			if (avg > this.th) {
				break;
			}
		}

	}

	public static void quicksortTimestep(ArrayList<Instance> vetDefects, int left, int right) {
		int i, j;
		Instance tmp;
		Double mid;

		i = left;
		j = right;
		mid = vetDefects.get((left + right) / 2).value(15);
		do {
			while (vetDefects.get(i).value(15) < mid)
				i++;
			while (mid < vetDefects.get(j).value(15))
				j--;
			if (i <= j) {
				tmp = vetDefects.get(i).copy();
				vetDefects.set(i, vetDefects.get(j).copy());
				vetDefects.set(j, tmp);
				i++;
				j--;
			}
		} while (i <= j);
		if (left < j)
			quicksortTimestep(vetDefects, left, j);
		if (i < right)
			quicksortTimestep(vetDefects, i, right);
	}

	public double[] getVotesForInstance(Instance inst) {

		//initialize attributes and set parameters as the first example arrives
		if (idxTimestamp == -1) {
			idxTimestamp = unixTimeStampIndex.getValue();
			setORBParam();

			setPbsaParams();
			params = th + "-" + pbsaPercentageDifferenceTh;
			firstTimeStamp = (long) inst.value(unixTimeStampIndex.getValue());

		}

		this.currentTimeStamp = (long) inst.value(unixTimeStampIndex.getValue());

		trainOnInstancesWaitingTime();

		// Attribute attTmp = inst.attribute(unixTimeStampIndex.getValue());
		Instance instTmp = inst.copy();
		instTmp.deleteAttributeAt(unixTimeStampIndex.getValue()); // remove the
		double[] ret = super.getVotesForInstance(instTmp);


		// monitor the moving average of the last 20 (hardcoded) incoming test instances in order to 
		// detect if the classifier is skewed or not towards a specific class
		double predictionsMovingAvg = 0.0;
		if (ret.length > 1) {
			predictionsMovingAvg = computeMAPredictions(20, ret[0] > ret[1] ? 0 : 1);
		} else {
			predictionsMovingAvg = computeMAPredictions(20, 0);
		}

		if (predictionsMovingAvg > (th + ((1 - th) * pbsaPercentageDifferenceTh))
				&& (lastSkewTowardsDefect + limitInstancesPassedAfterLastRecovering) < currentTestInstanceIndex) {

			recoverFromSkewTowardDefectInducingClass();

			lastSkewTowardsDefect = currentTestInstanceIndex;

		}

		if (predictionsMovingAvg < (th - (th * pbsaPercentageDifferenceTh))
				&& (lastSkewTowardsClean + limitInstancesPassedAfterLastRecovering) < currentTestInstanceIndex) {

			recoverFromSkewTowardCleanClass();

			lastSkewTowardsClean = currentTestInstanceIndex;

		}

		currentTestInstanceIndex++;

		return ret;
	}

	private double computeMAPredictions(int vSize, int output) {

		if (arrWindowsPredictions.size() < predictionsWindowSize) {
			arrWindowsPredictions.add(Double.valueOf(output));
		} else {
			arrWindowsPredictions.remove(0);
			arrWindowsPredictions.add(Double.valueOf(output));
		}

		if (predictionsMAHistory.size() < vSize) {
			predictionsMAHistory.add(arrWindowsPredictions.stream().mapToDouble(a -> a).average().getAsDouble());

		} else {
			predictionsMAHistory.remove(0);
			predictionsMAHistory.add(arrWindowsPredictions.stream().mapToDouble(a -> a).average().getAsDouble());
		}

		predictionsMAHistoryComplete.add(arrWindowsPredictions.stream().mapToDouble(a -> a).average().getAsDouble());

		double meanMAPreds = predictionsMAHistory.stream().mapToDouble(a -> a).average().getAsDouble();

		return meanMAPreds;

	}

}
