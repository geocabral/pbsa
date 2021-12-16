/*
 *    OzaBag.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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
package moa.classifiers.meta;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.trees.HoeffdingTree;
import moa.classifiers.trees.HoeffdingTree.Node;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.MiscUtils;
import moa.options.ClassOption;
import moa.streams.ArffFileStream;

/**
 * Incremental on-line bagging of Oza and Russell.
 *
 * <p>
 * Oza and Russell developed online versions of bagging and boosting for Data
 * Streams. They show how the process of sampling bootstrap replicates from
 * training data can be simulated in a data stream context. They observe that
 * the probability that any individual example will be chosen for a replicate
 * tends to a Poisson(1) distribution.
 * </p>
 *
 * <p>
 * [OR] N. Oza and S. Russell. Online bagging and boosting. In Artiﬁcial
 * Intelligence and Statistics 2001, pages 105–112. Morgan Kaufmann, 2001.
 * </p>
 *
 * <p>
 * Parameters:
 * </p>
 * <ul>
 * <li>-l : Classiﬁer to train</li>
 * <li>-s : The number of models in the bag</li>
 * </ul>
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class OzaBagRegisterTreeFixedIR extends AbstractClassifier implements MultiClassClassifier {

	Writer writerRoot;
	Writer writerSubRoot;
	String currentdataset = "";
	double lambda = 1;
	
	// number each attribute was chosen as a root normalized by the ensemble
	// size
	public static int[] rootAtt = new int[14];

	// number each attribute was chosen as a root of a subtree normalized by the
	// ensemble size
	public static int[] subRootAtt = new int[14];
	
	int ctInstances = 0;
	int ctInstancesNeg = 0;
	int ctInstancesPos = 0;
	

	@Override
	public String getPurposeString() {
		return "Incremental on-line bagging of Oza and Russell.";
	}

	private static final long serialVersionUID = 1L;

	public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l', "Classifier to train.", Classifier.class,
			"trees.HoeffdingTree");

	public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's', "The number of models in the bag.", 10, 1,
			Integer.MAX_VALUE);

	protected Classifier[] ensemble;

	@Override
	public void resetLearningImpl() {
		this.ensemble = new Classifier[this.ensembleSizeOption.getValue()];
		Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
		baseLearner.resetLearning();
		for (int i = 0; i < this.ensemble.length; i++) {
			this.ensemble[i] = baseLearner.copy();
		}
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {

		ctInstances += 1;

		
		
		if(inst.classValue() == 0){
			ctInstancesNeg++;
		}else{
			ctInstancesPos++;
		}
		
		
		if(ctInstances <= 500){
			lambda = (ctInstancesNeg/new Double(ctInstances)) / (ctInstancesPos/new Double(ctInstances));
		}
		
//		if(ctInstances == 500){
//			lambda = ((ctInstancesNeg/(500d))/(ctInstancesPos/(500d)));
//		}

		
		
		
		for (int i = 0; i < this.ensemble.length; i++) {
			int k = MiscUtils.poisson(lambda, this.classifierRandom);
			
			if(inst.classValue() == 0){
				k = 1;
			}
			
			
			if (k > 0) {
				Instance weightedInst = (Instance) inst.copy();
				weightedInst.setWeight(inst.weight() * k);
				this.ensemble[i].trainOnInstance(weightedInst);
			}
		}
	}

	@Override
	public double[] getVotesForInstance(Instance inst) {
		DoubleVector combinedVote = new DoubleVector();

		//writer for register in a file the number of roots and subroots for each feature
		currentdataset = "JGroups";
		try {
			
			if(writerRoot == null){
				writerRoot = new FileWriter("RQ4/roots"+currentdataset+"FixedIR.txt");	
			}
			
			if(writerSubRoot == null){
				writerSubRoot = new FileWriter("RQ4/subRoots"+currentdataset+"FixedIR.txt");	
			}
			
			for(int i = 0; i < 14; i++){
				writerRoot.write(rootAtt[i]+"\t");
				writerSubRoot.write(subRootAtt[i]+"\t");
			}
			writerRoot.write("\n");
			writerSubRoot.write("\n");
            
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//reset all the information about the number of roots and subroots (regarding features)
		for(int i = 0; i < 14; i++) {
			rootAtt[i] = 0;
			subRootAtt[i] = 0;
		}

		for (int i = 0; i < this.ensemble.length; i++) {

			//computeAttTreeUsage(this.ensemble[i]);

			

			DoubleVector vote = new DoubleVector(this.ensemble[i].getVotesForInstance(inst));
			if (vote.sumOfValues() > 0.0) {
				vote.normalize();
				combinedVote.addValues(vote);
			}
		}
		
//		for(int i = 0; i < 14; i++) {
//			System.out.print("("+i+", "+rootAtt[i]+")\t");
//		}
//		System.out.print(currentK+"\n");
		
		
		return combinedVote.getArrayRef();
	}

//	private void computeAttTreeUsage(Classifier classifier) {
//
//		try {
//			HoeffdingTree ht = (HoeffdingTree) classifier;
//			
//			rootAtt[((HoeffdingTree.SplitNode) ht.treeRoot).splitTest.getAttsTestDependsOn()[0]] += 1;
//
//			AutoExpandVector<Node> children = ((HoeffdingTree.SplitNode) ht.treeRoot).children;
//			for (Node child : children) {
//				if (child != null) {
//
//					int numAttsSub = 0;
//
//					try {
//						numAttsSub = ((HoeffdingTree.SplitNode) child).splitTest.getAttsTestDependsOn().length;
//					} catch (Exception e) {
//					}
//
//					for (int j = 0; j < numAttsSub; j++) {
//
//						subRootAtt[((HoeffdingTree.SplitNode) child).splitTest.getAttsTestDependsOn()[j]] += 1;
//						
//					}
//				}
//			}
//
//		} catch (Exception npe) {
//		}
//
//	}
//
//	// @ggc2
//	// plot data
//	public void classifiesArtificialDataToPlotCurve() {
//
//		int timeStepChecking = 4800;
//
//		double minAtt1 = 2000;
//		double minAtt2 = 2000;
//		double maxAtt1 = -2000;
//		double maxAtt2 = -2000;
//
//		String pathWindows = "C:/ProjetosSoftware/fse2018/";
//		String pathMac = "/Users/georgegomescabral/ProjetosSoftwares/Java/workspace/fse2018/";
//
//		boolean isWindows = false;
//		String path = "";
//
//		if (isWindows) {
//			path = pathWindows;
//		} else {
//			path = pathMac;
//		}
//
//		String dataset = "brackets.arff";
//
//		ArffFileStream stream = new ArffFileStream(path + "arffs/" + dataset, 15);
//		stream.prepareForUse();
//
//		int ct = 0;
//		while (stream.hasMoreInstances() && ct < timeStepChecking) {
//
//			ct++;
//		}
//
//		// return combinedVote.getArrayRef();
//	}

	@Override
	public boolean isRandomizable() {
		return true;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		// TODO Auto-generated method stub
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return new Measurement[] { new Measurement("ensemble size", this.ensemble != null ? this.ensemble.length : 0) };
	}

	@Override
	public Classifier[] getSubClassifiers() {
		return this.ensemble.clone();
	}
}
