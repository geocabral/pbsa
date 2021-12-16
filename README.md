# pbsa
 Source code for the algorithm Prediction Based Sampling Adjustment designed for online Just-in-time Software Defect Prediction and considering the verification latency.
 
The main class is placed at moa.experiment.RunPBSA.java. Inside this class there is plenty of information for executing a sample experiment. 

It is also strongly recommended to become familiar with the datasets structure in order to apply the algorithm to your own datasets.

The basic parameters can be assigned based on the following explanation of moa command:

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
		// ** -d results/neutronRes.csv - indicates the output file 

