package moa.classifiers.core.driftdetection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.text.StrTokenizer;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import com.yahoo.labs.samoa.instances.Instance;

import moa.streams.ArffFileStream;

public class FeaturesConceptDriftDetector implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	ArrayList<ArrayList<Double>> arraArrFeatValues = new ArrayList<>();
	ArrayList<ArrayList<Double>> arraArrMovAvgs = new ArrayList<>();
	ArrayList<ArrayList<Double>> arraArrMovAvgsStds = new ArrayList<>();

	static ArrayList<Double> arrAvgPredictions = null;

	static ArrayList<Double> predictionsMAHistory = new ArrayList<>();

	static int lastPredCleanClass = 0;

	public ArrayList<Integer> featuresChanged = new ArrayList<>();

	int times = 3;

	public HashMap<Integer, Integer> lastChanges = new HashMap<>();

	public static int numberInstancesAvgComputation = 100;
	public int movingAverageVectorSize = 100;

	public int instancesSeen = 2000;

	public boolean changed = false;

	BufferedWriter writer = null;
	public static BufferedWriter writer2 = null;

	public void input(Instance inst) {

		changed = false;

		inputFeat2(inst, 7, 0);
		inputFeat2(inst, 8, 1);
		inputFeat2(inst, 9, 2);
		inputFeat2(inst, 10, 3);
		inputFeat2(inst, 11, 4);
		inputFeat2(inst, 13, 5);

		if (featuresChanged == null) {
			featuresChanged = new ArrayList<>();
		}

		for (Integer i : lastChanges.keySet()) {
			// for(Integer j: lastChanges.keySet()){
			// if(i!=j){
			if (lastChanges.get(i) > 0) {
				featuresChanged.add(i);
				// lastChanges.put(i, -lastChanges.get(i));
				changed = true;
			}
			// }
			// }
		}

		// combining at least 2 features
		// for(Integer i: lastChanges.keySet()){
		// for(Integer j: lastChanges.keySet()){
		// if(i!=j){
		// if(lastChanges.get(i) > 0 && (Math.abs(lastChanges.get(i)) -
		// Math.abs(lastChanges.get(j))) < 100){
		// featuresChanged.add(i);
		// lastChanges.put(i, -lastChanges.get(i));
		// changed = true;
		// }
		// }
		// }
		// }

		// if(!featuresChanged.isEmpty() && changed){
		// System.out.println(instancesSeen+": "+featuresChanged);
		// featuresChanged = new ArrayList<>();
		// }

		instancesSeen++;
		// if(instancesSeen == 18877){
		// try {
		// writer.close();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
	}

	// the aim of this method is to detect big changes in the time series
	// standard deviation
	public void inputFeat(Instance inst, int feat, int pos) {

		// arraArrFeatValues contains all the arrays containing features values
		// for each feature investigated
		if (arraArrFeatValues.size() < pos + 1) {
			arraArrFeatValues.add(new ArrayList<Double>());
			arraArrMovAvgs.add(new ArrayList<Double>());
			arraArrMovAvgsStds.add(new ArrayList<Double>());
			lastChanges.put(feat, 0);
		}

		// arraArrFeatValues.get(pos) is the array containing the last
		// numberInstancesAvgComputation feature values for feature
		// feat. This array average is the input for the array
		// arraArrMovAvgs.get(pos)
		if (arraArrFeatValues.get(pos).size() < numberInstancesAvgComputation) {
			arraArrFeatValues.get(pos).add(inst.value(feat));
		} else {
			arraArrFeatValues.get(pos).remove(0);
			arraArrFeatValues.get(pos).add(inst.value(feat));
			double[] mean_stddev = std_dev(arraArrFeatValues.get(pos), numberInstancesAvgComputation);

			// arraArrMovAvgs.get(pos) contains the last movingAverageVectorSize
			// means of the last movingAverageVectorSize time windows (the time
			// windows are stored in arraArrFeatValues.get(pos))
			if (arraArrMovAvgs.get(pos).size() < movingAverageVectorSize) {
				arraArrMovAvgs.get(pos).add(mean_stddev[0]);
				arraArrMovAvgsStds.get(pos).add(mean_stddev[1]);
			} else {
				arraArrMovAvgs.get(pos).remove(0);
				arraArrMovAvgs.get(pos).add(mean_stddev[0]);

				arraArrMovAvgsStds.get(pos).remove(0);
				arraArrMovAvgsStds.get(pos).add(mean_stddev[1]);

				// retrieve information regarding the first 40 moving averages
				// and the last 40 moving averages. A gap of 20 moving averages
				// is not considered in order to make distributions far from
				// each other
				// double[] mean_stddev_init = std_dev(arraArrMovAvgs.get(pos),
				// 40);
				// double[] mean_stddev_end =
				// std_dev(arraArrMovAvgs.get(pos).subList(60, 100), 40);

				double[] mean_stddev_init = std_dev(arraArrMovAvgsStds.get(pos), 40);
				double[] mean_stddev_end = std_dev(arraArrMovAvgsStds.get(pos).subList(60, 100), 40);

				// times smaller
				// double t = (mean_stddev_init[0] + (2 * mean_stddev_init[1]))
				// / (mean_stddev_end[0] + (2 * mean_stddev_end[1]));

				// t is how many times the first 40 moving averages standard
				// deviation is higher than the last 40 ones
				double t = ((mean_stddev_init[0])) / ((mean_stddev_end[0]));

				double t2 = ((mean_stddev_end[0])) / ((mean_stddev_init[0]));

				// if t is higher than a pre-defined value (times) and this
				// feature wasn't affected by a drift in the last 1000 commits,
				// then a new alarm is triggered for this feature

				NumberFormat nf = NumberFormat.getInstance(new Locale("en", "US"));
				nf.setMaximumFractionDigits(2);

				try {
					if (feat == 8) {
						// writer.write(instancesSeen+"\t"+arraArrMovAvgs.get(pos).get(99)+"\t"+mean_stddev_init[0]+"\t"+
						// mean_stddev_end[0]+"\t"+ t+"\n");
						writer.write(
								instancesSeen + "\t" + nf.format(mean_stddev[0]) + "\t" + nf.format(mean_stddev_init[0])
										+ "\t" + nf.format(mean_stddev_end[0]) + "\t" + nf.format(t) + "\n");
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if ((t > 10 || t2 > 10) && (instancesSeen - Math.abs(lastChanges.get(feat))) > 1000) {// &&

					// System.out.println(mean_stddev_init[0]+"\t"+mean_stddev_init[1]);
					// System.out.println(mean_stddev_end[0]+"\t"+mean_stddev_end[1]);
					// System.out.println(t);
					// (instancesSeen
					// -
					// lastOvearallDetection)
					// <=
					// 100)
					// {
					lastChanges.put(feat, instancesSeen);
					// changed = true;
					// lastOvearallDetection = instancesSeen;
					// System.out.println("change ["+feat+"]:"+instancesSeen);
					// featuresChanged.add(new Integer(feat));
				}
			}

		}

	}

	// the aim of this method is to detect big changes in the time series
	// standard deviation
	public void inputFeat2(Instance inst, int feat, int pos) {

		// arraArrFeatValues contains all the arrays containing features values
		// for each feature investigated
		if (arraArrFeatValues.size() < pos + 1) {
			arraArrFeatValues.add(new ArrayList<Double>());
			arraArrMovAvgs.add(new ArrayList<Double>());
			arraArrMovAvgsStds.add(new ArrayList<Double>());
			lastChanges.put(feat, 0);
		}

		// arraArrFeatValues.get(pos) is the array containing the last
		// numberInstancesAvgComputation feature values for feature
		// feat. This array average is the input for the array
		// arraArrMovAvgs.get(pos)
		if (arraArrFeatValues.get(pos).size() < numberInstancesAvgComputation) {
			arraArrFeatValues.get(pos).add(inst.value(feat));
		} else {
			arraArrFeatValues.get(pos).remove(0);
			arraArrFeatValues.get(pos).add(inst.value(feat));
			double[] mean_stddev = std_dev(arraArrFeatValues.get(pos), numberInstancesAvgComputation);

			// arraArrMovAvgs.get(pos) contains the last movingAverageVectorSize
			// means of the last movingAverageVectorSize time windows (the time
			// windows are stored in arraArrFeatValues.get(pos))
			if (arraArrMovAvgs.get(pos).size() < movingAverageVectorSize) {
				arraArrMovAvgs.get(pos).add(mean_stddev[0]);
				arraArrMovAvgsStds.get(pos).add(mean_stddev[1]);
			} else {
				arraArrMovAvgs.get(pos).remove(0);
				arraArrMovAvgs.get(pos).add(mean_stddev[0]);

				arraArrMovAvgsStds.get(pos).remove(0);
				arraArrMovAvgsStds.get(pos).add(mean_stddev[1]);

				// retrieve information regarding the first 40 moving averages
				// and the last 40 moving averages. A gap of 20 moving averages
				// is not considered in order to make distributions far from
				// each other
				// double[] mean_stddev_init = std_dev(arraArrMovAvgs.get(pos),
				// 40);
				// double[] mean_stddev_end =
				// std_dev(arraArrMovAvgs.get(pos).subList(60, 100), 40);

				double[] mean_stddev_avg_init = std_dev(arraArrMovAvgs.get(pos), 30);
				double[] mean_stddev_avg_end = std_dev(arraArrMovAvgs.get(pos).subList(70, 100), 30);

				double[] mean_stddev_init = std_dev(arraArrMovAvgsStds.get(pos), 30);
				double[] mean_stddev_end = std_dev(arraArrMovAvgsStds.get(pos).subList(70, 100), 30);

				// t is how many times the first 40 moving averages standard
				// deviation is higher than the last 40 ones
				double t = ((mean_stddev_init[0])) / ((mean_stddev_end[0]));

				double t2 = ((mean_stddev_end[0])) / ((mean_stddev_init[0]));

				// if t is higher than a pre-defined value (times) and this
				// feature wasn't affected by a drift in the last 1000 commits,
				// then a new alarm is triggered for this feature

				NumberFormat nf = NumberFormat.getInstance(new Locale("en", "US"));
				nf.setMaximumFractionDigits(2);

				boolean isOverlap = isThereOverlap(mean_stddev_avg_init[0], mean_stddev_init[0], mean_stddev_avg_end[0],
						mean_stddev_end[0]);

				if (!isOverlap || (t > 10 || t2 > 10)) {
					if ((instancesSeen - Math.abs(lastChanges.get(feat))) > 1000) {
						// System.out.println(feat + "\t" + instancesSeen + "\t
						// batDistance");
						lastChanges.put(feat, instancesSeen);
					}

				}

			}

		}

	}

	public static boolean isThereOverlap(double pm, double ps, double qm, double qs) {

		double times = 1;

		boolean ret = false;

		double upperBoundP = pm + (times * ps);
		double lowerBoundP = pm - (times * ps);

		double upperBoundQ = qm + (times * qs);
		double lowerBoundQ = qm - (times * qs);

		if (upperBoundP >= lowerBoundQ && upperBoundP <= upperBoundQ) {
			ret = true;
		}

		if (lowerBoundP >= lowerBoundQ && lowerBoundP <= upperBoundQ) {
			ret = true;
		}

		if (upperBoundQ >= lowerBoundP && upperBoundQ <= upperBoundP) {
			ret = true;
		}

		if (lowerBoundQ >= lowerBoundP && lowerBoundQ <= upperBoundP) {
			ret = true;
		}

		return ret;
	}

	public static double batachDist(double pm, double ps, double qm, double qs) {

		double ret = 0.0;

		// ret = (1/8)*Math.pow((ps-qs), 2);
		//
		// ret *= 1/((Math.pow(pm, 2)+ Math.pow(qm, 2))/2);
		//
		// ret += 0.5 * Math.log(Math.abs(((Math.pow(pm, 2)+ Math.pow(qm,
		// 2))/2))/(pm*qm));

		ret = (1 / 4.) * Math
				.log((1 / 4.) * (((Math.pow(pm, 2) / Math.pow(qm, 2)) + (Math.pow(qm, 2) / Math.pow(pm, 2)) + 2)));

		ret += (1 / 4.) * (Math.pow(ps - qs, 2) / (Math.pow(qm, 2) + Math.pow(pm, 2)));

		return ret;

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

	public static void main(String args[]) {

		if (writer2 == null) {
			try {
				writer2 = new BufferedWriter(new FileWriter("outputsMABrackets.txt"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		FeaturesConceptDriftDetector fd = new FeaturesConceptDriftDetector();

		String path = "/Users/georgegomescabral/ProjetosSoftwares/Java/workspace/JournalSDP2018/arffs/delayAnticipateNew/";
		String ds = "camel";
		ArffFileStream stream = new ArffFileStream(path + ds + ".arff", 15);
		stream.prepareForUse();

		try {
			populatePredictions(ds);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// System.out.println(arrAvgPredictions.size());

		int ct = 0;
		while (stream.hasMoreInstances()) {

			Instance trainInst = stream.nextInstance().getData();

			if (trainInst.value(16) != 3) {

				try {
					// System.out.println(ct);
					writer2.write(predictionsMAHistory.get(ct) + "\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//
				// // System.out.println(ct+"\t"+meanMAPreds);
				// if (meanMAPreds >= 0.7) {
				// lastPredCleanClass = ct;
				// }
				fd.input(trainInst);

				if(ct == 2000){
					fd.featuresChanged = new ArrayList<>();
				}
				
				if (!fd.featuresChanged.isEmpty() && fd.changed && ct > 2000) {

					for (Integer i : fd.featuresChanged) {

						if (fd.lastChanges.containsKey(i)) {
							// if(Math.abs(ct -
							// (Math.abs(fd.lastChanges.get(i))-2000))>1000){
							// if(ct > 17000){
							System.out.println(i + "\t" + ct);
							// }

							fd.lastChanges.put(i,
									-fd.lastChanges.get(i));
							
							// }
						}

					}

					// for(Integer i: fd.lastChanges.keySet()){
					//
					// }

					fd.featuresChanged = new ArrayList<>();

				}

				// if (fd.changed && (ct - lastPredCleanClass) < 100) {
				// System.out.println("*" + lastPredCleanClass);
				// }

				ct++;
			}

		}

		System.out.println(ct);

		try {
			writer2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void populatePredictions(String ds) throws FileNotFoundException {

		// simulate the predictions data
		File file = new File("/Volumes/GEORGE2/exps/wflorbf2/" + ds + "(i15t0.99w90r10s20u4)-1.csv");
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

		ArrayList<Double> arrWindowsPredictions = new ArrayList<>();
		arrAvgPredictions = new ArrayList<>();
		for (int i = 0; i < arrPredictions.size(); i++) {

			if (arrWindowsPredictions.size() < 100) {
				arrWindowsPredictions.add(arrPredictions.get(i));
			} else {
				arrWindowsPredictions.remove(0);
				arrWindowsPredictions.add(arrPredictions.get(i));
			}

			predictionsMAHistory.add(arrWindowsPredictions.stream().mapToDouble(a -> a).average().getAsDouble());

			// if(predictionsMAHistory.size() < 20){
			// predictionsMAHistory.add(arrWindowsPredictions.stream().mapToDouble(a
			// -> a).average().getAsDouble());
			// }else{
			// predictionsMAHistory.remove(0);
			// predictionsMAHistory.add(arrWindowsPredictions.stream().mapToDouble(a
			// -> a).average().getAsDouble());
			// }

			// arrAvgPredictions.add(arrWindowsPredictions.stream().mapToDouble(a
			// -> a).average().getAsDouble());
		}

	}

}
