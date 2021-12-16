package moa.classifiers.core.driftdetection;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

public class PredictionBasedSDPDriftDetector extends AbstractChangeDetector {

	private static final long serialVersionUID = 4363046403413642539L;

	public int instancesSeen;

	public int lastChangeTimeStep = -1;

	public int lastConfirmedChangeTimeStep;

	public int consecChangesThreshold = 30;

	public int currentConsecChangesClass0;
	public int currentConsecChangesClass1;

	ArrayList<Double> arrPredictsValues = new ArrayList<>();
	ArrayList<Double> arrMovAvgPredValues = new ArrayList<>();

	ArrayList<Double> lastMovingAverages = new ArrayList<>();
	
	
	public int numberInstancesAvgComputation = 100;
	public int movingAverageVectorSize = 500;

	public int numberStdDevIntervalLimit = 1;
	
	public boolean isLoad = false;

	@Override
	public void input(double inputValue) {

		this.isChangeDetectedClass0 = false;

		if (arrPredictsValues.size() < numberInstancesAvgComputation) {
			arrPredictsValues.add(inputValue);
		} else {
			arrPredictsValues.remove(0);
			arrPredictsValues.add(inputValue);
			double[] mean_stddev = std_dev(arrPredictsValues, numberInstancesAvgComputation);

			if (arrMovAvgPredValues.size() < movingAverageVectorSize) {
				arrMovAvgPredValues.add(mean_stddev[0]);
			} else {
				arrMovAvgPredValues.remove(0);
				arrMovAvgPredValues.add(mean_stddev[0]);

			}
		}

		if (instancesSeen > 2000) {

			// TODO parameterize this part of the code
			double[] mean_stddevTr = std_dev(arrMovAvgPredValues, 100);

			double[] mean_stddevTest = std_dev(arrMovAvgPredValues.subList(400, 500), 100);

//			if(instancesSeen > 17000 && instancesSeen < 18000){
//				System.out.println(instancesSeen+": ("+mean_stddevTr[0]+","+mean_stddevTr[1]+")\t("+mean_stddevTest[0]+","+mean_stddevTest[1]+")");	
//			}
			
			
			// if (mean_stddevTr[0] - numberStdDevIntervalLimit *
			// mean_stddevTr[1] > mean_stddevTest[0]
			// + numberStdDevIntervalLimit * mean_stddevTest[1]
			// || mean_stddevTr[0] + numberStdDevIntervalLimit *
			// mean_stddevTr[1] < mean_stddevTest[0]
			// - numberStdDevIntervalLimit * mean_stddevTest[1] &&
			// (mean_stddevTest[0] < 0.25 || mean_stddevTest[0] > 0.75 )) {

			// if (mean_stddevTr[0] - numberStdDevIntervalLimit *
			// mean_stddevTr[1] > mean_stddevTest[0]
			// + numberStdDevIntervalLimit * mean_stddevTest[1]
			// || mean_stddevTr[0] + numberStdDevIntervalLimit *
			// mean_stddevTr[1] < mean_stddevTest[0]
			// - numberStdDevIntervalLimit * mean_stddevTest[1]
			// && (mean_stddevTest[0] > 0.50)) {
			//
			if ((mean_stddevTr[0] + (numberStdDevIntervalLimit * mean_stddevTr[1]) < mean_stddevTest[0]
					- (numberStdDevIntervalLimit * mean_stddevTest[1])) && (mean_stddevTest[0] > 0.60)) {

				// System.out.println(mean_stddevTest[0]);
				// if ((mean_stddevTest[0] > 0.70)) {

				//System.out.println(currentConsecChangesClass0);
				currentConsecChangesClass0++;
			} else {
				if (currentConsecChangesClass0 >= consecChangesThreshold) {
					this.isChangeDetectedClass0 = true;
					System.out.println("change in prediction: " + instancesSeen+"       "+mean_stddevTr[0]+ "   "+mean_stddevTest[0]);
					
					lastChangeTimeStep = instancesSeen;
				}

				currentConsecChangesClass0 = 0;

			}

		}

		instancesSeen++;
	}

	/**
	 * Gets whether there is change detected for class 0.
	 *
	 * @return true if there is change
	 */
	public boolean getChange0() {
		return this.isChangeDetectedClass0;
	}

	/**
	 * Gets whether there is change detected for class 1.
	 *
	 * @return true if there is change
	 */
	public boolean getChange1() {
		return this.isChangeDetectedClass1;
	}

	public void input2(double inputValue, double threshold) {

		this.isChangeDetectedClass0 = false;

		if (arrPredictsValues.size() < 100) {
			arrPredictsValues.add(inputValue);
		} else {
			arrPredictsValues.remove(0);
			arrPredictsValues.add(inputValue);
			double[] mean_stddev = std_dev(arrPredictsValues, 100);

		}

		if (instancesSeen > 2000) {

			// TODO parameterize this part of the code
			double[] mean_stddevTr = std_dev(arrPredictsValues, 100);
			// an increase of 25% will raise a change detection
			double limitIncreaseMAPrediction = ((1 - threshold) * 0.25);

			// check if a change in the predictions affecting class 0 took place
			if ((mean_stddevTr[0] > (threshold + limitIncreaseMAPrediction))
					&& currentConsecChangesClass0 < consecChangesThreshold) {

				currentConsecChangesClass0++;
			} else {
				if (currentConsecChangesClass0 >= consecChangesThreshold) {
					this.isChangeDetectedClass0 = true;
					System.out.println("change in prediction: " + instancesSeen);
					lastChangeTimeStep = instancesSeen;
				}

				currentConsecChangesClass0 = 0;

			}

			// check if a change in the predictions affecting class 1 took place
			if ((mean_stddevTr[0] < (threshold - (threshold * 0.5)))
					&& currentConsecChangesClass1 < consecChangesThreshold) {

				currentConsecChangesClass1++;
			} else {
				if (currentConsecChangesClass1 >= consecChangesThreshold) {
					this.isChangeDetectedClass1 = true;
					System.out.println("change in prediction: " + instancesSeen);
					lastChangeTimeStep = instancesSeen;
				}

				currentConsecChangesClass1 = 0;

			}

		}

		instancesSeen++;
	}

	public void input3(double maPred, double threshold) {

		this.isChangeDetectedClass0 = false;

		this.isChangeDetectedClass1 = false;

		int idxStart = 2000;
		
		if(isLoad){
			idxStart = 0;
		}
		if (instancesSeen > idxStart) {

			// an increase of 25% will raise a change detection
			double limitIncreaseMAPrediction = ((1 - threshold) * 0.2);

			// check if a change in the predictions affecting class 0 took place
			if ((maPred > (threshold + limitIncreaseMAPrediction))
					&& currentConsecChangesClass0 < consecChangesThreshold) {

				currentConsecChangesClass0++;
			} else {
				if (currentConsecChangesClass0 >= consecChangesThreshold) {
					this.isChangeDetectedClass0 = true;
					System.out.println("change in prediction: " + instancesSeen);
					lastChangeTimeStep = instancesSeen;
				}

				currentConsecChangesClass0 = 0;

			}

			// check if a change in the predictions affecting class 1 took place
			if ((maPred < (threshold - (threshold * 0.2))) && currentConsecChangesClass1 < consecChangesThreshold) {

				currentConsecChangesClass1++;
			} else {
				if (currentConsecChangesClass1 >= consecChangesThreshold) {
					this.isChangeDetectedClass1 = true;
					System.out.println("change in prediction: " + instancesSeen);
					lastChangeTimeStep = instancesSeen;
				}

				currentConsecChangesClass1 = 0;

			}

		}

		instancesSeen++;
	}
	
	
	public void input4(double maPred, double threshold) {

		
		if(lastMovingAverages.size()<20){
			lastMovingAverages.add(maPred);
		}else{
			lastMovingAverages.remove(0);
			lastMovingAverages.add(maPred);
		}
		
		double sumMa = 0;
		
		for(int i = 0; i < lastMovingAverages.size(); i++){
			sumMa += lastMovingAverages.get(i);
		}
		sumMa = sumMa/lastMovingAverages.size();
		this.isChangeDetectedClass0 = false;

		this.isChangeDetectedClass1 = false;

		int idxStart = 0;
		
		if(isLoad){
			idxStart = 0;
		}
		if (instancesSeen > idxStart) {

			// an increase of 25% will raise a change detection
			double limitIncreaseMAPrediction = ((1 - threshold) * 0.2);

			// check if a change in the predictions affecting class 0 took place
			if ((sumMa > (threshold + limitIncreaseMAPrediction))) {
				this.isChangeDetectedClass0 = true;
				//System.out.println("change in prediction: 0 " + instancesSeen);
			} 
			// check if a change in the predictions affecting class 1 took place
			if ((sumMa < (threshold - (threshold * 0.2)))) {

				this.isChangeDetectedClass1 = true;
				//System.out.println("change in prediction: 1 " + instancesSeen);
			} 
		}

		instancesSeen++;
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		// TODO Auto-generated method stub

	}

	public double[] std_dev(List<Double> a, int limit) {

		double[] ret = new double[2];

		if (a.size() == 0)
			return ret;
		double sum = 0;
		double sq_sum = 0;
		for (int i = 0; i < limit; i++) {
			sum += a.get(i);
			sq_sum += a.get(i) * a.get(i);
		}

		double[] array = ArrayUtils.toPrimitive(a.toArray(new Double[a.size()]));

		double mean = sum / limit;
		// double variance = (sq_sum - (mean * mean))/limit;

		StandardDeviation s = new StandardDeviation();

		ret[0] = mean;
		ret[1] = s.evaluate(array);

		return ret;
	}

}
