# pbsa
Source code for the algorithm Prediction Based Sampling Adjustment (PBSA) designed for online Just-in-time Software Defect Prediction and considering the verification latency.
 
The implamentation is based on the Massive Online Analysis ([MOA](https://moa.cms.waikato.ac.nz/)) framework, Release 2018.06, which is included in this repository following the GNU GENERAL PUBLIC LICENSE Version 3.
 
The main class is placed at scr/moa/experiment/RunPBSA.java. The evaluation method EvaluatePrequentialPBSA runs PBSA and evaluates its predictive performance in a prequential manner while taking verification latency into account.

An example of command line to run the code is the following:

```{r}
EvaluatePrequentialPBSA -l (meta.PBSA.PBSA -i 15 -s 20 -t 0.99 -w 90 -p 100;0.4;10;12;1.5;3 -a 0.25;5;2) 
				-s (ArffFileStream -f (datasets/neutron.arff) -c 15) 
				-e (FadingFactorEachClassPerformanceEvaluator -a 0.99) -f 1 
				-d results/neutronRes.csv 
```{r}				

where the hyperparameters fed to the PBSA method are:

-i is the index (column) of the timestamp in the dataset. Indices start with 0. Therefore, if the timestamp is in the second column of the dataset file, its index is 1.
-s is the ensemble size used for the base learning algorithm, which in this implementation is [ORB](https://github.com/geocabral/spdisc-icse19).
-t is the time decay factor used for tracking the class size in [ORB](https://github.com/geocabral/spdisc-icse19).
-w is the waiting time in days used for obtaining the label of software changes.
-p are the hyperparameters of the base learner [ORB](https://github.com/geocabral/spdisc-icse19).
-a are the hyperparameters percentageDeviationFromTh;alfa;beta of the PBSA approach. Warning: alfa and beta must be entered as integers!

and the parameters of the MOA framework are:
-l the machine learning algorithm to be used.
-s (ArffFileStream -f <path to dataset> -c <class label index>) is the path to the dataset in arff format, with -c indicating the index of the class label in the dataset file.
-e (FadingFactorEachClassPerformanceEvaluator -a <fading factor>) is the performance evaluator to be used, with -a indicating the fading factor to be adopted. 
-d is the path to the output file where the results of the experiments will be saved

