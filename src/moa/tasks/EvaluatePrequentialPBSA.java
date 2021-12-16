/*
 *    EvaluatePrequential.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.OptionalDouble;
import java.util.Vector;

import org.jfree.data.xy.XYSeriesCollection;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.MultiClassClassifier;
import moa.classifiers.meta.PBSA.RecallsAndTimeStep;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.evaluation.EWMAClassificationPerformanceEvaluator;
import moa.evaluation.EachClassPerformanceEvaluator;
import moa.evaluation.FadingFactorClassificationPerformanceEvaluator;
import moa.evaluation.FadingFactorEachClassPerformanceEvaluator;
import moa.evaluation.LearningCurve;
import moa.evaluation.LearningPerformanceEvaluator;
import moa.evaluation.WindowClassificationPerformanceEvaluator;
import moa.learners.Learner;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

/**
 * Task for evaluating a classifier on a stream by testing then training with
 * each example in sequence.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class EvaluatePrequentialPBSA extends ClassificationMainTask {

	private static HashMap<String, Object> hashResults = new HashMap<>();
	private static final String RES_INSTANCE_CLASS = "instanceClass";
	private static final String RES_INSTANCE_PREDICTION = "instancePrediction";
	private static final String RES_RECALL_0 = "recall_0";
	private static final String RES_RECALL_1 = "recall_1";
	private static final String RES_TS = "timestamp";

	long predictionTimeAverage = 0;
	long trainingTimeAverage = 0;

	public int bugsForAddTrainingORB = 0;

	@Override
	public String getPurposeString() {
		return "Evaluates a classifier on a stream by testing then training with each example in sequence.";
	}

	private static final int NOT_BUG = 0;
	private static final int BUG_NOT_DISCOVERED_W_DAYS = 1;
	private static final int BUG_DISCOVERED_W_DAYS = 2;
	private static final int BUG_FOUND = 3;

	private static final long serialVersionUID = 1L;

	public ClassOption learnerOption = new ClassOption("learner", 'l', "Learner to train.", MultiClassClassifier.class,
			"moa.classifiers.bayes.NaiveBayes");

	public ClassOption streamOption = new ClassOption("stream", 's', "Stream to learn from.", ExampleStream.class,
			"generators.RandomTreeGenerator");

	public ClassOption evaluatorOption = new ClassOption("evaluator", 'e',
			"Classification performance evaluation method.", LearningPerformanceEvaluator.class,
			"WindowClassificationPerformanceEvaluator");

	public IntOption instanceLimitOption = new IntOption("instanceLimit", 'i',
			"Maximum number of instances to test/train on  (-1 = no limit).", 100000000, -1, Integer.MAX_VALUE);

	public IntOption timeLimitOption = new IntOption("timeLimit", 't',
			"Maximum number of seconds to test/train for (-1 = no limit).", -1, -1, Integer.MAX_VALUE);

	public IntOption sampleFrequencyOption = new IntOption("sampleFrequency", 'f',
			"How many instances between samples of the learning performance.", 100000, 0, Integer.MAX_VALUE);

	public IntOption memCheckFrequencyOption = new IntOption("memCheckFrequency", 'q',
			"How many instances between memory bound checks.", 100000, 0, Integer.MAX_VALUE);

	public FileOption dumpFileOption = new FileOption("dumpFile", 'd', "File to append intermediate csv results to.",
			null, "csv", true);

	public FileOption outputPredictionFileOption = new FileOption("outputPredictionFile", 'o',
			"File to append output predictions to.", null, "pred", true);

	// New for prequential method DEPRECATED
	public IntOption widthOption = new IntOption("width", 'w', "Size of Window", 1000);

	public FloatOption alphaOption = new FloatOption("alpha", 'a', "Fading factor or exponential smoothing factor",
			.01);
	// End New for prequential methods

	@Override
	public Class<?> getTaskResultType() {
		return LearningCurve.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {

		Learner learner = (Learner) getPreparedClassOption(this.learnerOption);

		ExampleStream stream = (ExampleStream) getPreparedClassOption(this.streamOption);

		LearningPerformanceEvaluator evaluator = (LearningPerformanceEvaluator) getPreparedClassOption(
				this.evaluatorOption);
		LearningCurve learningCurve = new LearningCurve("learning evaluation instances");

		// hash containing the results to be dumped to a file at the end
		hashResults.put(RES_INSTANCE_CLASS, new Vector<Integer>());
		hashResults.put(RES_INSTANCE_PREDICTION, new Vector<Integer>());
		hashResults.put(RES_RECALL_0, new Vector<Double>());
		hashResults.put(RES_RECALL_1, new Vector<Double>());
		hashResults.put(RES_TS, new Vector<String>());

		// New for prequential methods
		if (evaluator instanceof WindowClassificationPerformanceEvaluator) {
			if (widthOption.getValue() != 1000) {
				System.out
						.println("DEPRECATED! Use EvaluatePrequential -e (WindowClassificationPerformanceEvaluator -w "
								+ widthOption.getValue() + ")");
				return learningCurve;
			}
		}
		if (evaluator instanceof EWMAClassificationPerformanceEvaluator) {
			if (alphaOption.getValue() != .01) {
				System.out.println("DEPRECATED! Use EvaluatePrequential -e (EWMAClassificationPerformanceEvaluator -a "
						+ alphaOption.getValue() + ")");
				return learningCurve;
			}
		}
		if (evaluator instanceof FadingFactorClassificationPerformanceEvaluator) {
			if (alphaOption.getValue() != .01) {
				System.out.println(
						"DEPRECATED! Use EvaluatePrequential -e (FadingFactorClassificationPerformanceEvaluator -a "
								+ alphaOption.getValue() + ")");
				return learningCurve;
			}
		}
		// End New for prequential methods

		learner.setModelContext(stream.getHeader());
		int maxInstances = this.instanceLimitOption.getValue();
		long instancesProcessed = 0;
		int maxSeconds = this.timeLimitOption.getValue();
		int secondsElapsed = 0;
		monitor.setCurrentActivity("Evaluating learner...", -1.0);

		File dumpFile = this.dumpFileOption.getFile();
		PrintStream immediateResultStream = null;
		if (dumpFile != null) {
			try {
				if (dumpFile.exists()) {
					immediateResultStream = new PrintStream(new FileOutputStream(dumpFile, true), true);
				} else {
					immediateResultStream = new PrintStream(new FileOutputStream(dumpFile), true);
				}
			} catch (Exception ex) {
				throw new RuntimeException("Unable to open immediate result file: " + dumpFile, ex);
			}
		}
		// File for output predictions
		File outputPredictionFile = this.outputPredictionFileOption.getFile();
		PrintStream outputPredictionResultStream = null;
		if (outputPredictionFile != null) {
			try {
				if (outputPredictionFile.exists()) {
					outputPredictionResultStream = new PrintStream(new FileOutputStream(outputPredictionFile, true),
							true);
				} else {
					outputPredictionResultStream = new PrintStream(new FileOutputStream(outputPredictionFile), true);
				}
			} catch (Exception ex) {
				throw new RuntimeException("Unable to open prediction result file: " + outputPredictionFile, ex);
			}
		}

		while (stream.hasMoreInstances() && ((maxInstances < 0) || (instancesProcessed < maxInstances))
				&& ((maxSeconds < 0) || (secondsElapsed < maxSeconds))) {

			boolean write_results = true;

			Example trainInst = stream.nextInstance();
			Example testInst = (Example) trainInst.copy();

			Attribute a = new Attribute("commit_type");
			int train_as_defective = (int) ((Instance) trainInst.getData()).value(a);

			((Instance) trainInst.getData()).deleteAttributeAt(16);
			((Instance) testInst.getData()).deleteAttributeAt(16);

			int predictedClass = 0;

			if (train_as_defective == NOT_BUG) {

				double[] prediction = learner.getVotesForInstance(testInst);

				if (prediction.length > 1) {
					if (prediction[0] > prediction[1]) {
						predictedClass = 0;
					} else {
						predictedClass = 1;
					}
				} else {
					predictedClass = 0;
				}

				evaluator.addResult(testInst, prediction);
				learner.trainOnInstance(trainInst);
			}

			if (train_as_defective == BUG_NOT_DISCOVERED_W_DAYS) {

				((Instance) testInst.getData()).setClassValue(1);
				double[] prediction = learner.getVotesForInstance(testInst);

				if (prediction.length > 1) {
					if (prediction[0] > prediction[1]) {
						predictedClass = 0;
					} else {
						predictedClass = 1;
					}
				} else {
					predictedClass = 0;
				}

				evaluator.addResult(testInst, prediction);

				((Instance) trainInst.getData()).setClassValue(0);
				learner.trainOnInstance(trainInst);

			}

			if (train_as_defective == BUG_DISCOVERED_W_DAYS) {

				((Instance) testInst.getData()).setClassValue(1);

				double[] prediction = learner.getVotesForInstance(testInst);

				if (prediction.length > 1) {
					if (prediction[0] > prediction[1]) {
						predictedClass = 0;
					} else {
						predictedClass = 1;
					}
				} else {
					predictedClass = 0;
				}

				evaluator.addResult(testInst, prediction);

				bugsForAddTrainingORB++;

			}

			if (train_as_defective == BUG_FOUND) {

				((Instance) trainInst.getData()).setClassValue(1);
				learner.trainOnInstance(trainInst);

				write_results = false;
			} else {

			}

			if (write_results) {
				instancesProcessed++;
				if (instancesProcessed % this.sampleFrequencyOption.getValue() == 0
						|| stream.hasMoreInstances() == false) {

					String ts = (((Instance) testInst.getData()).value(15) + "");
					int idxE = ts.indexOf("E");

					ts = new BigDecimal(((Instance) testInst.getData()).value(15) + "").intValue() + "";

					if (idxE > 0) {

						((Vector<String>) hashResults.get(RES_TS)).add(ts);

						((Vector<Integer>) hashResults.get(RES_INSTANCE_CLASS))
								.add(new Double(((Instance) testInst.getData()).classValue()).intValue());
						((Vector<Integer>) hashResults.get(RES_INSTANCE_PREDICTION)).add(predictedClass);
						((Vector<Double>) hashResults.get(RES_RECALL_0))
								.add(evaluator.getPerformanceMeasurements()[7].getValue());
						((Vector<Double>) hashResults.get(RES_RECALL_1))
								.add(evaluator.getPerformanceMeasurements()[8].getValue());

					}

				}
			}

			// @ggc2
			if (learner instanceof moa.classifiers.meta.PBSA.PBSA && write_results) {

				FadingFactorEachClassPerformanceEvaluator eval = (FadingFactorEachClassPerformanceEvaluator) evaluator;
				moa.classifiers.meta.PBSA.PBSA lear = (moa.classifiers.meta.PBSA.PBSA) learner;

				lear.pastRecall0.add(eval.recall0);
				lear.pastRecall1.add(eval.recall1);

				long instant = (long) 10000;
				RecallsAndTimeStep r = new RecallsAndTimeStep(instant, eval.recall0, eval.recall1);

				if (EachClassPerformanceEvaluator.showChart && instancesProcessed%50==0) {

					eval.updateChart(new Long(instancesProcessed).intValue());

					((XYSeriesCollection) eval.chart.xylineChart.getXYPlot().getDataset()).getSeries(3).add(
							instancesProcessed,
							(lear.predictionsMAHistory.stream().mapToDouble(val -> val).average().orElse(0.0) * 100));

				}

			}

		}

		// print output file
		String filePath = dumpFileOption.getValue();
		try (PrintWriter writer = new PrintWriter(new File(filePath))) {

			StringBuilder sb = new StringBuilder();
			sb.append("instance class;");
			sb.append("predicted class;");
			sb.append("recall(0);");
			sb.append("recall(1);");
			sb.append("timestamp \n");

			for (int i = 0; i < ((Vector<Integer>) hashResults.get(RES_INSTANCE_CLASS)).size(); i++) {
				sb.append(((Vector<Integer>) hashResults.get(RES_INSTANCE_CLASS)).get(i) + ";");
				sb.append(((Vector<Integer>) hashResults.get(RES_INSTANCE_PREDICTION)).get(i) + ";");
				sb.append(((Vector<Double>) hashResults.get(RES_RECALL_0)).get(i) + ";");
				sb.append(((Vector<Double>) hashResults.get(RES_RECALL_1)).get(i) + ";");
				sb.append(((Vector<String>) hashResults.get(RES_TS)).get(i) + "\n");
			}

			writer.write(sb.toString());

			// compute summary of the results
			OptionalDouble averageRec0 = ((Vector<Double>) hashResults.get(RES_RECALL_0)).stream().mapToDouble(a -> a)
					.average();

			OptionalDouble averageRec1 = ((Vector<Double>) hashResults.get(RES_RECALL_1)).stream().mapToDouble(a -> a)
					.average();

			// compute gmean and difference between recalls
			double avg_gmean = 0.;
			double avg_diffRecalls = 0.;

			for (int i = 0; i < ((Vector<Double>) hashResults.get(RES_RECALL_1)).size(); i++) {
				avg_gmean += Math.sqrt(((Vector<Double>) hashResults.get(RES_RECALL_1)).get(i)
						* ((Vector<Double>) hashResults.get(RES_RECALL_0)).get(i));
				avg_diffRecalls += Math.abs(((Vector<Double>) hashResults.get(RES_RECALL_1)).get(i)
						- ((Vector<Double>) hashResults.get(RES_RECALL_0)).get(i));
			}

			avg_diffRecalls /= ((Vector<Double>) hashResults.get(RES_RECALL_1)).size();
			avg_gmean /= ((Vector<Double>) hashResults.get(RES_RECALL_1)).size();

			DecimalFormat df = new DecimalFormat("#0.00");

			System.out.println("\nResults*******\n");
			System.out.println("Tested Instances: " + ((Vector<Double>) hashResults.get(RES_RECALL_1)).size());
			System.out.println("Average Recall(0): " + df.format(averageRec0.getAsDouble()));
			System.out.println("Average Recall(1): " + df.format(averageRec1.getAsDouble()));
			System.out.println("Average Diff_recalls: " + df.format(avg_diffRecalls));
			System.out.println("Average Gmean between recalls: " + df.format(avg_gmean));

			
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		}

		return learningCurve;
	}
}
