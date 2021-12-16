package moa.classifiers.core.driftdetection;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

public class FeatureBasedSDPDriftDetector extends AbstractChangeDetector {

	private static final long serialVersionUID = 4363046403413642539L;

	public int instancesSeen;

	public int lastChangeTimeStep = -1;
	
	public int lastConfirmedChangeTimeStep;
	
	public int consecChangesThreshold = 30;

	public int currentConsecChanges;

	ArrayList<Double> arrFeatValues = new ArrayList<>();
	ArrayList<Double> arrMovAvgFeatValues = new ArrayList<>();

	public int numberInstancesAvgComputation = 300;
	public int movingAverageVectorSize = 300;

	public int numberStdDevIntervalLimit = 2;

	@Override
	public void input(double inputValue) {

		if (arrFeatValues.size() < numberInstancesAvgComputation) {
			arrFeatValues.add(inputValue);
		} else {
			arrFeatValues.remove(0);
			arrFeatValues.add(inputValue);
			double[] mean_stddev = std_dev(arrFeatValues, numberInstancesAvgComputation);

			if (arrMovAvgFeatValues.size() < movingAverageVectorSize) {
				arrMovAvgFeatValues.add(mean_stddev[0]);
			} else {
				arrMovAvgFeatValues.remove(0);
				arrMovAvgFeatValues.add(mean_stddev[0]);

			}
		}

		if (instancesSeen > 2000) {

			// TODO parameterize this part of the code
			double[] mean_stddevTr = std_dev(arrMovAvgFeatValues, 100);

			double[] mean_stddevTest = std_dev(arrMovAvgFeatValues.subList(200, 300), 100);

			if (mean_stddevTr[0] - numberStdDevIntervalLimit * mean_stddevTr[1] > mean_stddevTest[0] + numberStdDevIntervalLimit * mean_stddevTest[1]
					|| mean_stddevTr[0] + numberStdDevIntervalLimit * mean_stddevTr[1] < mean_stddevTest[0] - numberStdDevIntervalLimit * mean_stddevTest[1]) {
				currentConsecChanges++;
			} else {
				if (currentConsecChanges >= consecChangesThreshold) {
					this.isChangeDetectedClass0 = true;
					lastChangeTimeStep = instancesSeen;
				}

				currentConsecChanges = 0;

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

	public  double[] std_dev(List<Double> a, int limit) {

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
