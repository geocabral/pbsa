# pbsa
Source code for the algorithm Prediction Based Sampling Adjustment (PBSA) designed for online Just-in-time Software Defect Prediction and considering the verification latency.
 
The implamentation is based on the Massive Online Analysis ([MOA](https://moa.cms.waikato.ac.nz/)) framework, Release 2018.06, which is included in this repository following the GNU GENERAL PUBLIC LICENSE Version 3.
 
The main class is placed at scr/moa/experiment/RunPBSA.java. The evaluation method EvaluatePrequentialPBSA runs PBSA and evaluates its predictive performance in a prequential manner while taking verification latency into account.

The class for replicating the experiments is placed at scr/moa/experiment/RunExperimentsTSE.java. However, note that this class uses 2 loops that start 300 threads that run in parallel. This is not the best way to conduct the experiment, specially if you have a computer cluster available. Executing 300 threads at once will probably result in memory issues.

An example of command line to run the code is the following:

```{r}
EvaluatePrequentialPBSA -l (meta.PBSA.PBSA -i 15 -s 20 -t 0.99 -w 90 -p 100;0.4;10;12;1.5;3 -a 0.25;5;2) 
				-s (ArffFileStream -f (datasets/neutron.arff) -c 15) 
				-e (FadingFactorEachClassPerformanceEvaluator -a 0.99) -f 1 
				-d results/neutronRes.csv 
```			

**Where the hyperparameters fed to the PBSA method are:**

-i is the index (column) of the timestamp in the dataset. Indices start with 0. Therefore, if the timestamp is in the second column of the dataset file, its index is 1.

-s is the ensemble size used for the base learning algorithm, which in this implementation is [ORB](https://github.com/geocabral/spdisc-icse19).

-t is the time decay factor used for tracking the class size in [ORB](https://github.com/geocabral/spdisc-icse19).

-w is the waiting time in days used for obtaining the label of software changes.

-p are the hyperparameters of the base learner [ORB](https://github.com/geocabral/spdisc-icse19).

-a are the hyperparameters percentageDeviationFromTh;alfa;beta of the PBSA approach. Warning: alfa and beta must be entered as integers!

**And the parameters of the MOA framework are:**

-l the machine learning algorithm to be used.

-s (ArffFileStream -f <path to dataset> -c <class label index>) is the path to the dataset in arff format, with -c indicating the index of the class label in the dataset file.

-e (FadingFactorEachClassPerformanceEvaluator -a <fading factor>) is the performance evaluator to be used, with -a indicating the fading factor to be adopted. 

-d is the path to the output file where the results of the experiments will be saved.

**Datasets:**

A number of datasets used in experiments with PBSA can be found in the folder datasets. The file format is the WEKA ARFF, the attributes of the used datasets have the following types:

```{r}
@attribute fix numeric
@attribute ns numeric
@attribute nd numeric
@attribute nf numeric
@attribute entrophy numeric
@attribute la numeric
@attribute ld numeric
@attribute lt numeric
@attribute ndev numeric
@attribute age numeric
@attribute nuc numeric
@attribute exp numeric
@attribute rexp numeric
@attribute sexp numeric
@attribute contains_bug {False,True}
@attribute author_date_unix_timestamp numeric
@attribute commit_type numeric
```

From these attributes, the two last ones are intended to control the method's operation. The author_date_unix_timestamp is particularly important to reproduce the verification latency phenomenon. 

The commit_type attribute receives one of the four values (0 - CLEAN), (1 - BUG_NOT_DISCOVERED_W_DAYS), (2 - BUG_DISCOVERED_W_DAYS) and (3 - BUG_FOUND). One can determine these values by checking, in the original dataset, how many days a defect-inducing commit took to be fixed. Based on that, the processed dataset should be formatted as follows for use with PBSA:

* If a commit is clean, it should be listed in the file with commit_type = 0. The PBSA approach will then use this commit as a clean example for testing at the author unix timestamp and for training w days after the author unix timestamp.
* If a commit is defect-inducing and its label took t > w days to arrive, it should be listed twice in the file as follows:
	- It should be listed once with commit_type = 1. PBSA will then use this commit for training as a clean labeled example w days after the author unix timestamp.
	- It should be listed once again with commit_type = 3. PBSA will then use this commit for training as a defect-inducing labeled example t days after the author unix timestamp. 
* If a commit is defect-inducing and its label took t <= w days to arrive, it should be listed twice in the file as follows:
	- It should be listed once with commit_type = 2. This will alert PBSA of the fact that this commit will generate a training example before the end of the waiting time. 
	- It should be listed once again with commit_type = 3. PBSA will then use this commit for training as a defect-inducing labeled example t days after the author unix timestamp. 

IMPORTANT: the commits need to be listed in the dataset in ascending order of author unix timestamp.
