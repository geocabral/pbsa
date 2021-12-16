package teste;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.text.StrTokenizer;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;

import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.Classifier;
import moa.classifiers.core.driftdetection.EDDM;
import moa.classifiers.core.driftdetection.PageHinkleyDM;
import moa.classifiers.trees.HoeffdingTree;
import moa.core.TimingUtils;
import moa.streams.ArffFileStream;

public class ExperimentCD {

	public ExperimentCD() {
	}

	public void run(int numInstances, boolean isTesting) {
		Classifier learner = new HoeffdingTree();
		ArffFileStream stream = new ArffFileStream(
				"/Users/georgegomescabral/ProjetosSoftwares/Java/workspace/JournalSDP2018/arffs/delayAnticipate/tomcat.arff",
				15);
		stream.prepareForUse();

		learner.setModelContext(stream.getHeader());
		learner.prepareForUse();

		PageHinkleyDM cd = new PageHinkleyDM();
		// cd.deltaAdwinOption.setValue(0.000000000000000000000000001);

		ArrayList<Double> arr = new ArrayList<>();

		int numberSamplesCorrect = 0;
		int numberSamples = 0;
		long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
		int ctChanges = 0;
		int att = 7;

		ArrayList<Double> arrMovingAverages = new ArrayList<>();

		int consecChanges = 0;

		while (stream.hasMoreInstances() && numberSamples < numInstances) {
			Instance trainInst = stream.nextInstance().getData();

			if (arr.size() < 100) {
				arr.add(trainInst.value(att));
			} else {
				arr.remove(0);
				arr.add(trainInst.value(att));
				double[] mean_stddev = std_dev(arr, 100);

				if (arrMovingAverages.size() < 200) {
					arrMovingAverages.add(mean_stddev[0]);
				} else {
					arrMovingAverages.remove(0);
					arrMovingAverages.add(mean_stddev[0]);

				}

			}

			if (numberSamples > 2000) {

				double movingAverage = 0;

				for (int i = 150; i < 200; i++) {
					movingAverage += arrMovingAverages.get(i);
				}

				movingAverage = movingAverage / 50;

				double[] mean_stddev = std_dev(arrMovingAverages, 100);

				double[] mean_stddevTest = std_dev(arrMovingAverages.subList(150, 200), 50);

				// double mean = arr.subList(0,
				// 100).stream().mapToDouble(a->a).average().getAsDouble();
				// System.out.println(numberSamples+" -- "+mean);

				// if(numberSamples > 14500 && numberSamples < 16000){
				// System.out.println(numberSamples + ": " + mean_stddev[0] + "
				// - " + mean_stddev[1] + " --> ma: "
				// + movingAverage+" last:
				// "+arrMovingAverages.get(arrMovingAverages.size()-1));
				// }

				double times = 2;

				if (mean_stddev[0] - times * mean_stddev[1] > mean_stddevTest[0] + times * mean_stddevTest[1]
						|| mean_stddev[0] + times * mean_stddev[1] < mean_stddevTest[0] - times * mean_stddevTest[1]) {
					consecChanges++;
					System.out.println(numberSamples + " - change detected  " + consecChanges);

				} else {
					consecChanges = 0;
				}

				// double times = 2.1;
				// if (movingAverage > (mean_stddev[0] + (times *
				// mean_stddev[1]))
				// || movingAverage < (mean_stddev[0] - (times *
				// mean_stddev[1]))) {
				// if(numberSamples < 17000){
				// System.out.println(numberSamples + ": " + mean_stddev[0] + "
				// - " + mean_stddev[1] + " --> ma: "
				// + mean_stddevTest[0]+" std: "+mean_stddevTest[1]);
				// }
				//
				// }

			}

			// cd.input(movingAverage);
			//// if(numberSamples < 17000){
			//// System.out.println(numberSamples+" - "+movingAverage);
			//// }
			//
			//
			// if(cd.getChange()){
			// System.out.println("change!!"+" sample:"+numberSamples);
			// ctChanges++;
			// }
			//

			numberSamples++;

		}
		double accuracy = 100.0 * (double) numberSamplesCorrect / (double) numberSamples;
		double time = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - evaluateStartTime);
		System.out.println(numberSamples + " instances processed with " + accuracy + "% accuracy in " + time
				+ " seconds. changes:" + ctChanges);
	}

	public void run2(int numInstances, boolean isTesting) {
		Classifier learner = new HoeffdingTree();
		ArffFileStream stream = new ArffFileStream(
				"/Users/georgegomescabral/ProjetosSoftwares/Java/workspace/JournalSDP2018/arffs/delayAnticipate/tomcat.arff",
				15);
		stream.prepareForUse();

		learner.setModelContext(stream.getHeader());
		learner.prepareForUse();

		EDDM cd = new EDDM();
		// cd.deltaAdwinOption.setValue(0.000000000000000000000000001);

		ArrayList<Double> arr = new ArrayList<>();

		int numberSamplesCorrect = 0;
		int numberSamples = 0;
		long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
		int ctChanges = 0;
		int att = 7;

		ArrayList<Double> arrMovingAverages = new ArrayList<>();

		int consecChanges = 0;

		while (stream.hasMoreInstances() && numberSamples < numInstances) {
			Instance trainInst = stream.nextInstance().getData();

			cd.input(trainInst.classValue());

			if (numberSamples > 2000) {

				if (cd.getChange()) {
					System.out.println(numberSamples + " -- changed ");
				}

			}

			// cd.input(movingAverage);
			//// if(numberSamples < 17000){
			//// System.out.println(numberSamples+" - "+movingAverage);
			//// }
			//
			//
			// if(cd.getChange()){
			// System.out.println("change!!"+" sample:"+numberSamples);
			// ctChanges++;
			// }
			//

			numberSamples++;

		}
		double accuracy = 100.0 * (double) numberSamplesCorrect / (double) numberSamples;
		double time = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - evaluateStartTime);
		System.out.println(numberSamples + " instances processed with " + accuracy + "% accuracy in " + time
				+ " seconds. changes:" + ctChanges);
	}

	public void run3(int numInstances, boolean isTesting) throws FileNotFoundException {

		EDDM cd = new EDDM();
		cd.prepareForUse();

		File file = new File(
				"/Users/georgegomescabral/ProjetosSoftwares/Java/workspace/JournalSDP2018/outputPattern.txt");

		Scanner input = new Scanner(file);

		int ct = 1;
		while (input.hasNextLine()) {

			double output = new Double(input.nextLine());
			cd.input(output);

			if (ct > 2000) {
				if (cd.getChange()) {
					System.out.println(ct + "  changed");
				}
			}

			ct++;
		}

	}

	public static double[] std_dev(List<Double> a, int limit) {

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

	public static void movingAveragePredictions() throws IOException {
		int lastPredictionCD = 0;
		int numberConsecChanges = 30;
		double times = 2.;
		int numberSamples = 0;
		String ds = "jgroups";
		int att7 = 13;
		int consecChanges = 0;
		final XYSeries series = new XYSeries("Moving average Preds");
		ArrayList<Double> arrMovPredictions = new ArrayList<>();
		

		// concept drift detector for the predictions
		PageHinkleyDM cdPrediction = new PageHinkleyDM();
		cdPrediction.prepareForUse();

		// simulate the predictions data
		File file = new File("/Volumes/george.cabral@gmail.com/posdoc/icseExtension/Experiments/hoeff/wfloza/" + ds
				+ "(i15t0.99w90r10s20u4)-2.csv");
		Scanner input = new Scanner(file);
		ArrayList<Double> arrPredictions = new ArrayList<>();
		int ct = 0;
		while (input.hasNextLine()) {
			String str = input.nextLine();
			if (ct > 0) {
				@SuppressWarnings("deprecation")
				StrTokenizer strTok = new StrTokenizer(str, ',');
				for (int i = 0; i < 15; i++) {
					strTok.nextToken();
				}
				double pred = new Double(strTok.nextToken());
				// System.out.println(pred);
				arrPredictions.add(pred);
			}
			ct++;
		}

		ArrayList<Double> arrValuesPreds = new ArrayList<>();
		ArrayList<Double> arrMovingAveragesPreds = new ArrayList<>();

		for (int i = 0; i < arrPredictions.size(); i++) {

			if (arrValuesPreds.size() < 100) {
				arrValuesPreds.add(arrPredictions.get(i));
			} else {
				arrValuesPreds.remove(0);
				arrValuesPreds.add(arrPredictions.get(i));
				double[] mean_stddev = std_dev(arrValuesPreds, 100);

				if (arrMovingAveragesPreds.size() < 300) {
					arrMovingAveragesPreds.add(mean_stddev[0]);
				} else {
					arrMovingAveragesPreds.remove(0);
					arrMovingAveragesPreds.add(mean_stddev[0]);
				}

			}

			if (i > 400) {
				double[] mean_stddev = std_dev(arrMovingAveragesPreds, 100);
				// System.out.println(mean_stddev[0]);

				arrMovPredictions.add(mean_stddev[0]);
				series.add(i, mean_stddev[0]);
				double[] mean_stddevTest = std_dev(arrMovingAveragesPreds.subList(100, 300), 200);

				if (mean_stddev[0] - times * mean_stddev[1] > mean_stddevTest[0] + times * mean_stddevTest[1]
						|| mean_stddev[0] + times * mean_stddev[1] < mean_stddevTest[0] - times * mean_stddevTest[1]) {

					if (consecChanges >= numberConsecChanges) {
						System.out.println(i + " - change detected " + consecChanges);
					}

					consecChanges++;

				} else {
					consecChanges = 0;

				}
			}

		}

		
		double[] mean_stddevTest = std_dev(arrMovPredictions, arrMovPredictions.size());
		System.out.println(mean_stddevTest[0]+" - "+mean_stddevTest[1]);
		
		final XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(series);

		JFreeChart xylineChart = ChartFactory.createXYLineChart("title", "time step", "mov average preds", dataset,
				PlotOrientation.VERTICAL, true, true, false);

		ChartPanel chartPanel = new ChartPanel(xylineChart);
		chartPanel.setPreferredSize(new java.awt.Dimension(560, 367));
		final XYPlot plot = xylineChart.getXYPlot();

		ApplicationFrame frame = new ApplicationFrame("title");
		frame.pack();

		frame.setContentPane(chartPanel);
		frame.setVisible(true);
		frame.setSize(600, 400);

	}

	public static void simulateStream() throws IOException {
		int lastPredictionCD = 0;
		int numberConsecChanges = 30;
		double times = 2;
		int numberSamples = 0;
		String ds = "nova";
		int att7 = 12;

		// concept drift detector for the predictions
		EDDM cdPrediction = new EDDM();
		cdPrediction.prepareForUse();

		// simulate the predictions data
		File file = new File("/Volumes/george.cabral@gmail.com/posdoc/icseExtension/Experiments/hoeff/wfloza/" + ds
				+ "(i15t0.99w90r10s20u4)-1.csv");
		Scanner input = new Scanner(file);
		ArrayList<Double> arrPredictions = new ArrayList<>();
		int ct = 0;
		while (input.hasNextLine()) {
			String str = input.nextLine();
			if (ct > 0) {
				@SuppressWarnings("deprecation")
				StrTokenizer strTok = new StrTokenizer(str, ',');
				for (int i = 0; i < 15; i++) {
					strTok.nextToken();
				}
				double pred = new Double(strTok.nextToken());
				// System.out.println(pred);
				arrPredictions.add(pred);
			}
			ct++;
		}

		ArffFileStream stream = new ArffFileStream(
				"/Users/georgegomescabral/ProjetosSoftwares/Java/workspace/JournalSDP2018/arffs/delayAnticipate/" + ds
						+ ".arff",
				15);
		stream.prepareForUse();
		EDDM cdFeat7 = new EDDM();

		ArrayList<Double> arrValuesFeat7 = new ArrayList<>();
		ArrayList<Double> arrMovingAveragesFeat7 = new ArrayList<>();

		int consecChanges = 0;
		int timeAfterLastChangePrediction = 0;

		while (stream.hasMoreInstances()) {
			Instance trainInst = stream.nextInstance().getData();

			cdPrediction.input(arrPredictions.get(numberSamples));

			// feed the arrays containing the moving average and values for
			// feature 7

			if (arrValuesFeat7.size() < 300) {
				arrValuesFeat7.add(trainInst.value(att7));
			} else {
				arrValuesFeat7.remove(0);
				arrValuesFeat7.add(trainInst.value(att7));
				double[] mean_stddev = std_dev(arrValuesFeat7, 300);

				if (arrMovingAveragesFeat7.size() < 150) {
					arrMovingAveragesFeat7.add(mean_stddev[0]);
				} else {
					arrMovingAveragesFeat7.remove(0);
					arrMovingAveragesFeat7.add(mean_stddev[0]);

				}
			}

			// the detection only begins after time step 2000
			if (numberSamples > 2000) {

				double movingAverageFeat7 = 0;

				// for (int i = 100; i < 200; i++) {
				// movingAverageFeat7 += arrMovingAveragesFeat7.get(i);
				// }
				//
				// movingAverageFeat7 = movingAverageFeat7 / 100;

				double[] mean_stddev = std_dev(arrMovingAveragesFeat7, 50);

				double[] mean_stddevTest = std_dev(arrMovingAveragesFeat7.subList(100, 150), 50);

				if (mean_stddev[0] - times * mean_stddev[1] > mean_stddevTest[0] + times * mean_stddevTest[1]
						|| mean_stddev[0] + times * mean_stddev[1] < mean_stddevTest[0] - times * mean_stddevTest[1]) {
					consecChanges++;
					// System.out.println(numberSamples + " - change detected "
					// + consecChanges);

				} else {
					if (consecChanges >= numberConsecChanges) {// &&
																// ((numberSamples
																// -
																// lastPredictionCD)
																// < 200 ||
																// timeAfterLastChangePrediction
																// > 0)) {// ||
						// timeAfterLastChange
						// <
						// 200){
						System.out.println(numberSamples + " change in prediction and feature: "
								+ trainInst.inputAttribute(att7).name() + "   consecChanges: " + consecChanges);
					}

					consecChanges = 0;

				}

				if (cdPrediction.getChange()) {
					// System.out.println(numberSamples);
					lastPredictionCD = numberSamples;
					timeAfterLastChangePrediction = 150;
				} else {
					timeAfterLastChangePrediction--;
				}

			}

			numberSamples++;
		}

	}

	public static void main(String[] args) throws IOException {
		ExperimentCD exp = new ExperimentCD();
		 //exp.run3(1000000, true);
		//exp.simulateStream();
		exp.movingAveragePredictions();
	}
}