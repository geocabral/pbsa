package moa.experiment;

import java.io.IOException;
import java.io.Writer;

import moa.evaluation.EachClassPerformanceEvaluator;
import moa.options.ClassOption;
import moa.tasks.EvaluatePrequential;
import moa.tasks.MainTask;
import moa.tasks.TaskThread;

public class RunPBSA {

	static MainTask currentTask = new EvaluatePrequential();
	static Writer writer;

	public RunPBSA() {

	}

	public static void main(String[] args) throws IOException {

		String[] datasetsArray = { "fabric", "jgroups", "camel", "tomcat", "brackets", "neutron", "spring-integration",
				"broadleaf", "nova", "npm" };

		int numDatasetIdx = 0;
		String strParamsORB = "100;0.4;10;12;1.5;3";
		int numWaitingTime = 90;
		int numEnsembleSize = 20;
		double numDecayFactor = 0.99;
		String strParamsPBSA = "0.25;5;2"; // percentage tolerated of deviation from th for retraining, alfa and beta

		// ** EvaluatePrequentialPBSA - the evaluation method. In this case, the
		// evaluator considers the verification latency (i.e. the delay for obtaining
		// the true commit labels)
		
		//The complete command is as follows:
		// EvaluatePrequentialPBSA -l (meta.PBSA.PBSA -i 15 -t 0.99 -w 90 -p 100;0.4;10;12;1.5;3 -s 20 -a
		// 0.25;5;2) -s (ArffFileStream -f (datasets/neutron.arff) -c 15) -e
		// (FadingFactorEachClassPerformanceEvaluator -a 0.99) -f 1 -d
		// results/neutronRes.csv- the PBSA learner where:

		// (1) "-s 20" is the ensemble size
		// (2) "-t 0.99" The time decay factor for updating the class size
		// (3) "-w 90" is the waiting time for obtaining the true commit label
		// (4) "-p 100;0.4;10;12;1.5;3 - the parameters for the ORB.
		// (5) "-a percentageDeviationFromTh;alfa;beta - parameters for PBSA (recall
		// that alfa and beta are integers!!)
		// ** -s (ArffFileStream -f (datasets/neutron.arff) -c 15) - indicates the
		// dataset where the class label is in index 15
		// ** -e (FadingFactorEachClassPerformanceEvaluator -a 0.99) - indicates the
		// performance evaluator. In this case
		// the fading factor evaluator with fading factor value 0.99
		// ** -d results/neutronRes.csv - indicates the output file (recall that alfa
		// and beta are integers!!)

		String destResFolder = "results/";

		// for exhaustive experiments do not show the plots. Set it false
		EachClassPerformanceEvaluator.showChart = true;

		String task = "EvaluatePrequentialPBSA -l (meta.PBSA.PBSA -i 15 -t " + numDecayFactor + " -w " + numWaitingTime
				+ " -p " + strParamsORB + " -s " + numEnsembleSize + " -a " + strParamsPBSA
				+ " )  -s (ArffFileStream -f (datasets/" + datasetsArray[numDatasetIdx]
				+ ".arff) -c 15) -e (FadingFactorEachClassPerformanceEvaluator -a 0.99) -f 1 -d " + destResFolder
				+ datasetsArray[numDatasetIdx] + "(" + strParamsORB.replaceAll(";", "-") + ").csv";

		try {

			System.out.println(task);
			currentTask = (MainTask) ClassOption.cliStringToObject(task, MainTask.class, null);

			TaskThread thread = new TaskThread((moa.tasks.Task) currentTask);

			thread.start();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

}
