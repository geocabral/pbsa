package moa.evaluation;

import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;

import com.yahoo.labs.samoa.instances.Instance;

import ggc2.realtimechart.RealTimeRecallsChart2;
import moa.core.Example;
import moa.core.Measurement;
import moa.core.Utils;

public class EachClassPerformanceEvaluator extends BasicClassificationPerformanceEvaluator {

	public RealTimeRecallsChart2 chart;
	public static boolean showChart = true;

	
	public static double sumRec0 = 0;
	public static double sumRec1 = 0;
	public static double sumGmean = 0;
	public static double sumDiff = 0;
	
	
	public double recall0 = 0;
	public double recall1 = 0;

	public int instancesProcessed = 0;

	private static final long serialVersionUID = 1L;
	protected Estimator[] weightCorrectEachClass;
	protected Estimator[] weightIncorrectEachClass;
	private double[] totalWeightObservedEachClass;

	public EachClassPerformanceEvaluator() {
		super();
	}

	@Override
	public void reset() {
		reset(this.numClasses);
	}

	@Override
	public void reset(int numClasses) {
		super.reset(numClasses);
		
		//@ggc2
    	if(numClasses == 0){
    		numClasses = 2;
    	}
		
		weightCorrectEachClass = new Estimator[numClasses];
		weightIncorrectEachClass = new Estimator[numClasses];
		totalWeightObservedEachClass = new double[numClasses];
		for (int i = 0; i < numClasses; ++i) {
			weightCorrectEachClass[i] = newEstimator();
			weightIncorrectEachClass[i] = newEstimator();
			totalWeightObservedEachClass[i] = 0;
		}
	}

	@Override
	public void addResult(Example<Instance> example, double[] classVotes) {
		super.addResult(example, classVotes);
		Instance inst = example.getData();
		double weight = inst.weight();
		if (inst.classIsMissing() == false) {
			int trueClass = (int) inst.classValue();
			int predictedClass = Utils.maxIndex(classVotes);
			if (weight > 0.0) {
				if (weightCorrectEachClass.length == 0) {
					this.reset(inst.dataset().numClasses());
				}
				if (trueClass < 0) {
					System.err.println(
							"Error: unexpected class found while evaluating classifer (method addResult in EachClassPerformanceEvaluator.java).");
					System.exit(-1);
				}
				this.weightCorrectEachClass[trueClass].add(predictedClass == trueClass ? weight : 0);
				this.weightIncorrectEachClass[trueClass].add(predictedClass != trueClass ? weight : 0);
				this.totalWeightObservedEachClass[trueClass] += weight;
			}

		}
	}

	
	
	
	public void updateChart(int instancesProcessed){
	
		if (chart.xylineChart == null) {
			setRecallChart();
		}
		
		for (int i = 0; i < numClasses; ++i) {

			if (instancesProcessed % 1 == 0 || instancesProcessed == 0) {

				if (i == 0) {
					if (showChart) {
						((XYSeriesCollection) chart.xylineChart.getXYPlot().getDataset()).getSeries(1)
								.add(instancesProcessed, getFractionCorrectlyClassified(i) * 100.0);
					}
				} else {

					if (showChart) {
						((XYSeriesCollection) chart.xylineChart.getXYPlot().getDataset()).getSeries(0)
								.add(instancesProcessed, getFractionCorrectlyClassified(i) * 100.0);
						((XYSeriesCollection) chart.xylineChart.getXYPlot().getDataset()).getSeries(2)
								.add(instancesProcessed, Math.sqrt(recall1 * recall0) * 100.0);
					}

				}

			}

			
		}
		
	}
	
	
	@Override
	public Measurement[] getPerformanceMeasurements() {

		

		Measurement[] measure = super.getPerformanceMeasurements();
		Measurement[] measurePlus = new Measurement[measure.length + numClasses * 3];
		for (int i = 0; i < measure.length; ++i) {
			measurePlus[i] = measure[i];
		}

		for (int i = 0; i < numClasses; ++i) {
			String str = "classified instances of class " + i;
			measurePlus[measure.length + i] = new Measurement(str, getTotalWeightObserved(i));
		}

		
		
		for (int i = 0; i < numClasses; ++i) {
			String str = "recall (true positive) for class " + i + " (percent)";

			if (instancesProcessed % 1 == 0 || instancesProcessed == 0) {

				if (i == 0) {
				
					recall0 = getFractionCorrectlyClassified(i);
				} else {

					recall1 = getFractionCorrectlyClassified(i);

				}

			}

			measurePlus[measure.length + numClasses + i] = new Measurement(str,
					getFractionCorrectlyClassified(i) * 100.0);
		}
		
		sumRec0 += getFractionCorrectlyClassified(0) * 100.0;
		sumRec1 += getFractionCorrectlyClassified(1) * 100.0;
		sumGmean += Math.sqrt((getFractionCorrectlyClassified(0) * 100.0) * (getFractionCorrectlyClassified(1) * 100.0));
		sumDiff += Math.abs((getFractionCorrectlyClassified(0) * 100.0  ) - (getFractionCorrectlyClassified(1) * 100.0));
		

		

		for (int i = 0; i < numClasses; ++i) {
			String str = "incorrect classifications (false positive) for class " + i + " (percent)";
			measurePlus[measure.length + numClasses * 2 + i] = new Measurement(str,
					getFractionIncorrectlyClassified(i) * 100.0);

		}

		return measurePlus;

	}

	public double getFractionCorrectlyClassified(int classId) {
		return this.weightCorrectEachClass[classId].estimation();
	}

	public double getFractionIncorrectlyClassified(int classId) {
		return this.weightIncorrectEachClass[classId].estimation();
	}

	public double getTotalWeightObserved(int classId) {
		return this.totalWeightObservedEachClass[classId];
	}

	public void setRecallChart() {

		if (showChart) {
			chart = new RealTimeRecallsChart2("Defective Software Prediction", "Defective Software Prediction");

			chart.pack();
			RefineryUtilities.centerFrameOnScreen(chart);
			chart.setVisible(true);
		}

	}
}
