package moa.experiment;

import moa.evaluation.EachClassPerformanceEvaluator;
import moa.options.ClassOption;
import moa.tasks.EvaluatePrequential;
import moa.tasks.MainTask;
import moa.tasks.TaskThread;

public class RunExperimentsTSE {
	
	static MainTask currentTask = new EvaluatePrequential();

	public static void main(String[] args) {
		
		//---------IMPORTANT---------
		//Notice that these loops start 300 (10 * 30) threads that run in parallel. 
		//This is not the best way to conduct the experiment,
		//specially if you have a computer cluster available.
		//Executing 300 threads at once will probably result in memory issues.
		
		for(int i = 0; i < 10; i++) {
			for(int j = 0; j < 30; j++) {
			
				executeExp(i, "100;0.4;10;12;1.5;3", 90, 20, 0.99, "0.25;5;2", j);
				
			}
		}

	}

	
	public static void executeExp(int pDsIdx, String pParamsORB, int pWaitingTme, int pEnsSize, double pFF, String pPBSAParam, int pExecutionIdx) {
		String[] datasetsArray = { "fabric", "jgroups", "camel", "tomcat", "brackets", "neutron", "spring-integration",
				"broadleaf", "nova", "npm" };

		int numDatasetIdx = pDsIdx;
		String strParamsORB = pParamsORB;
		int numWaitingTime = pWaitingTme;
		int numEnsembleSize = pEnsSize;
		double numDecayFactor = pFF;
		String strParamsPBSA = pPBSAParam; // percentage tolerated of deviation from th for retraining, alfa and beta

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
		EachClassPerformanceEvaluator.showChart = false;

		String task = "EvaluatePrequentialPBSA -l (meta.PBSA.PBSA -i 15 -t " + numDecayFactor + " -w " + numWaitingTime
				+ " -p " + strParamsORB + " -s " + numEnsembleSize + " -a " + strParamsPBSA
				+ " )  -s (ArffFileStream -f (datasets/" + datasetsArray[numDatasetIdx]
				+ ".arff) -c 15) -e (FadingFactorEachClassPerformanceEvaluator -a 0.99) -f 1 -d " + destResFolder
				+ datasetsArray[numDatasetIdx] + "(" + strParamsORB.replaceAll(";", "-") + ")"+pExecutionIdx+".csv";

		try {

			//System.out.println(task);
			currentTask = (MainTask) ClassOption.cliStringToObject(task, MainTask.class, null);

			TaskThread thread = new TaskThread((moa.tasks.Task) currentTask);

			thread.start();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
}
