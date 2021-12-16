package moa.classifiers.core.driftdetection;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.commons.lang3.text.StrTokenizer;

import com.yahoo.labs.samoa.instances.Instance;

import moa.streams.ArffFileStream;

public class FeatureGroupBasedSDPDriftDetector{

	public int instancesSeen;
	
	public int timeWindowConfirmChange = 100;

	public HashMap<String, FeatureBasedSDPDriftDetector> hashFeatureDetectors;
	public HashMap<String, Integer> hashFeatureIdx;
	
	public String[] groupDiffusion = {"ns","nd","nf","entrophy"};
	public int[] groupDiffusionIdxs = {1,2,3,4};
	
	public String[] groupSize = {"la","ld","lt"};
	public int[] groupSizeIdxs = {5,6,7};
	
	public String[] groupHistory = {"ndev","age","nuc"};
	public int[] groupHistoryIdxs = {8,9,10};
	
	public String[] groupExperience = {"exp","rexp","sexp"};
	public int[] groupExperienceIdxs = {11,12,13};
	
	public ArrayList<Instance> trainingExamplesQueue = new ArrayList<>();
	
	public ArrayList<Integer> changedFeatures = new ArrayList<>();
	
	public void prepareForUseImpl(){
		
		hashFeatureDetectors = new HashMap<>();
		
		for(int i = 0; i < groupDiffusion.length; i++){
			hashFeatureDetectors.put(groupDiffusion[i], new FeatureBasedSDPDriftDetector());
		}
		
		for(int i = 0; i < groupSize.length; i++){
			hashFeatureDetectors.put(groupSize[i], new FeatureBasedSDPDriftDetector());
		}
		
		for(int i = 0; i < groupHistory.length; i++){
			hashFeatureDetectors.put(groupHistory[i], new FeatureBasedSDPDriftDetector());
		}
		
		for(int i = 0; i < groupExperience.length; i++){
			hashFeatureDetectors.put(groupExperience[i], new FeatureBasedSDPDriftDetector());
		}
		
		hashFeatureIdx = new HashMap<>();
		hashFeatureIdx.put("ns", 1);
		hashFeatureIdx.put("nd", 2);
		hashFeatureIdx.put("nf", 3);
		hashFeatureIdx.put("entrophy", 4);
		hashFeatureIdx.put("la", 5);
		hashFeatureIdx.put("ld", 6);
		hashFeatureIdx.put("lt", 7);
		hashFeatureIdx.put("ndev", 8);
		hashFeatureIdx.put("age", 9);
		hashFeatureIdx.put("nuc", 10);
		hashFeatureIdx.put("exp", 11);
		hashFeatureIdx.put("rexp", 12);
		hashFeatureIdx.put("sexp", 13);
		
		
	}
	
	public void feedDetectors(Instance inst){
		Iterator<String> it = hashFeatureDetectors.keySet().iterator();
		
		while(it.hasNext()){
			String attName = it.next();
			hashFeatureDetectors.get(attName).input(inst.value(hashFeatureIdx.get(attName)));
		}
		
	}
	
	public void enqueueInstance(Instance inst){
		
		if(trainingExamplesQueue.size() >= 50){
			trainingExamplesQueue.remove(0);
			trainingExamplesQueue.add(inst.copy());
		}else{
			trainingExamplesQueue.add(inst.copy());
		}
		
	}
	
	public void checkChange(){
		
//		verifyChangeGroupDiffusion();
		
//		verifyChangeGroupSize();
//		
		//check change for groupExperience
		verifyChangeGroupExperience();
		
		verifyChangeGroupHistory();
		
		
	}
	
	private void verifyChangeGroupDiffusion() {
		int[] lastChanges = new int[groupDiffusion.length];
		boolean changed = false;
		for(int i = 0; i < groupDiffusion.length; i++){
			lastChanges[i] = hashFeatureDetectors.get(groupDiffusion[i]).lastChangeTimeStep;
			if(lastChanges[i] == instancesSeen){
				changed = true;
			}
		}
		
		int numberChangesInWindow  = 0;
		for(int i = 0; i < lastChanges.length; i++){
			if(instancesSeen - lastChanges[i] <= timeWindowConfirmChange){
				numberChangesInWindow++;
			}
		}
		
		
		if(changed && numberChangesInWindow >= 2){
			int[] lastChangeRelativeToOtherFeature = new int[groupDiffusion.length];
			for(int i = 0; i < groupDiffusion.length; i++){
				lastChangeRelativeToOtherFeature[i] = 10000000;
				for(int j = 0; j < groupDiffusion.length; j++){
					if(i!=j){
						if(lastChangeRelativeToOtherFeature[i] > Math.abs(lastChanges[i] - lastChanges[j]) && lastChanges[i] > 0){
							lastChangeRelativeToOtherFeature[i] = Math.abs(lastChanges[i] - lastChanges[j]);  
						}
					}
				}	
			}
			
			String output = "";
			for(int i = 0; i < lastChangeRelativeToOtherFeature.length; i++){
				if(lastChangeRelativeToOtherFeature[i] <= timeWindowConfirmChange && (instancesSeen - hashFeatureDetectors.get(groupDiffusion[i]).lastConfirmedChangeTimeStep) > 1000){
					hashFeatureDetectors.get(groupDiffusion[i]).lastConfirmedChangeTimeStep = instancesSeen;
					output+= groupDiffusion[i]+" "+instancesSeen+"\t";
					changedFeatures.add(hashFeatureIdx.get(groupDiffusion[i]));
				}
				
			}
//			if(!output.equals("")){
//				System.out.println(output);
//			}
		}
	}
	
	private void verifyChangeGroupSize() {
		int[] lastChanges = new int[groupSize.length];
		boolean changed = false;
		for(int i = 0; i < groupSize.length; i++){
			lastChanges[i] = hashFeatureDetectors.get(groupSize[i]).lastChangeTimeStep;
			if(lastChanges[i] == instancesSeen){
				changed = true;
			}
		}
		
		int numberChangesInWindow  = 0;
		for(int i = 0; i < lastChanges.length; i++){
			if(instancesSeen - lastChanges[i] <= timeWindowConfirmChange){
				numberChangesInWindow++;
			}
		}
		
		
		if(changed && numberChangesInWindow >= 2){
			int[] lastChangeRelativeToOtherFeature = new int[groupSize.length];
			for(int i = 0; i < groupSize.length; i++){
				lastChangeRelativeToOtherFeature[i] = 10000000;
				for(int j = 0; j < groupSize.length; j++){
					if(i!=j){
						if(lastChangeRelativeToOtherFeature[i] > Math.abs(lastChanges[i] - lastChanges[j]) && lastChanges[i] > 0){
							lastChangeRelativeToOtherFeature[i] = Math.abs(lastChanges[i] - lastChanges[j]);  
						}
					}
				}	
			}
			
			String output = "";
			for(int i = 0; i < lastChangeRelativeToOtherFeature.length; i++){
				if(lastChangeRelativeToOtherFeature[i] <= timeWindowConfirmChange && (instancesSeen - hashFeatureDetectors.get(groupSize[i]).lastConfirmedChangeTimeStep) > 1000){
					hashFeatureDetectors.get(groupSize[i]).lastConfirmedChangeTimeStep = instancesSeen;
					output+= groupSize[i]+" "+instancesSeen+"\t";
					changedFeatures.add(hashFeatureIdx.get(groupSize[i]));
				}
				
			}
//			if(!output.equals("")){
//				System.out.println(output);
//			}
		}
	}
	
	

	private void verifyChangeGroupExperience() {
		int[] lastChanges = new int[groupExperience.length];
		boolean changed = false;
		for(int i = 0; i < groupExperience.length; i++){
			lastChanges[i] = hashFeatureDetectors.get(groupExperience[i]).lastChangeTimeStep;
			if(lastChanges[i] == instancesSeen){
				changed = true;
			}
		}
		
		int numberChangesInWindow  = 0;
		for(int i = 0; i < lastChanges.length; i++){
			if(instancesSeen - lastChanges[i] <= timeWindowConfirmChange){
				numberChangesInWindow++;
			}
		}
		
		
		if(changed && numberChangesInWindow >= 2){
			int[] lastChangeRelativeToOtherFeature = new int[groupExperience.length];
			for(int i = 0; i < groupExperience.length; i++){
				lastChangeRelativeToOtherFeature[i] = 10000000;
				for(int j = 0; j < groupExperience.length; j++){
					if(i!=j){
						if(lastChangeRelativeToOtherFeature[i] > Math.abs(lastChanges[i] - lastChanges[j]) && lastChanges[i] > 0){
							lastChangeRelativeToOtherFeature[i] = Math.abs(lastChanges[i] - lastChanges[j]);  
						}
					}
				}	
			}
			
			String output = "";
			for(int i = 0; i < lastChangeRelativeToOtherFeature.length; i++){
				if(lastChangeRelativeToOtherFeature[i] <= timeWindowConfirmChange && (instancesSeen - hashFeatureDetectors.get(groupExperience[i]).lastConfirmedChangeTimeStep) > 1000){
					hashFeatureDetectors.get(groupExperience[i]).lastConfirmedChangeTimeStep = instancesSeen;
					output+= groupExperience[i]+" "+instancesSeen+"\t";
					changedFeatures.add(hashFeatureIdx.get(groupExperience[i]));

				}
				
			}
//			if(!output.equals("")){
//				System.out.println(output);
//			}
		}
	}

	private void verifyChangeGroupHistory() {
		int[] lastChanges = new int[groupHistory.length];
		boolean changed = false;
		for(int i = 0; i < groupHistory.length; i++){
			lastChanges[i] = hashFeatureDetectors.get(groupHistory[i]).lastChangeTimeStep;
			if(lastChanges[i] == instancesSeen){
				changed = true;
			}
		}
		int numberChangesInWindow  = 0;
		for(int i = 0; i < lastChanges.length; i++){
			if(instancesSeen - lastChanges[i] <= timeWindowConfirmChange){
				numberChangesInWindow++;
			}
		}
		
		
		if(changed && numberChangesInWindow >= 2){
			int[] lastChangeRelativeToOtherFeature = new int[groupHistory.length];
			for(int i = 0; i < groupHistory.length; i++){
				lastChangeRelativeToOtherFeature[i] = 10000000;
				for(int j = 0; j < groupHistory.length; j++){
					if(i!=j){
						if(lastChangeRelativeToOtherFeature[i] > Math.abs(lastChanges[i] - lastChanges[j]) && lastChanges[i] > 0){
							lastChangeRelativeToOtherFeature[i] = Math.abs(lastChanges[i] - lastChanges[j]);  
						}
					}
				}	
			}
			
			String output = "";
			for(int i = 0; i < lastChangeRelativeToOtherFeature.length; i++){
				if(lastChangeRelativeToOtherFeature[i] <= timeWindowConfirmChange && (instancesSeen - hashFeatureDetectors.get(groupHistory[i]).lastConfirmedChangeTimeStep) > 1000){
					hashFeatureDetectors.get(groupHistory[i]).lastConfirmedChangeTimeStep = instancesSeen;
					output+= groupHistory[i]+" "+instancesSeen+"\t";
					changedFeatures.add(hashFeatureIdx.get(groupHistory[i]));
				}
				
			}
//			if(!output.equals("")){
//				System.out.println(output);
//			}
		}
	}
	
	public ArrayList<Integer> getChangedFeatures(){
		return changedFeatures;
	}
	
	public void setChangedFeatures(ArrayList<Integer> a){
		changedFeatures = a;
	}
	
	public static void main(String[] args){
		ArffFileStream stream = new ArffFileStream(
				"/Users/georgegomescabral/ProjetosSoftwares/Java/workspace/JournalSDP2018/arffs/delayAnticipateNew/camel.arff",
				15);
		stream.prepareForUse();
		
		FeatureGroupBasedSDPDriftDetector fd = new FeatureGroupBasedSDPDriftDetector();
		fd.prepareForUseImpl();
		
		while (stream.hasMoreInstances()) {
			Instance trainInst = stream.nextInstance().getData();
			
			if(trainInst.value(16) != 3){
				fd.enqueueInstance(trainInst);
				fd.feedDetectors(trainInst.copy());
				fd.checkChange();
				
				if(!fd.getChangedFeatures().isEmpty()){
					String s = fd.instancesSeen+": ";
					for(Integer i: fd.getChangedFeatures()){
						s += i+"\t";
					}
					System.out.println(s);
				}
				
				fd.setChangedFeatures(new  ArrayList<Integer>());
				
				fd.instancesSeen++;	
			}
			
			
		}
		
		
		PredictionBasedSDPDriftDetector p = new PredictionBasedSDPDriftDetector();
		p.prepareForUse();
		

		// simulate the predictions data
		File file = new File(
				"/Users/georgegomescabral/ProjetosSoftwares/Java/workspace/MOA_Deploy/results/camel.csv");
		Scanner input = null;
		try {
			input = new Scanner(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int ct = 0;
		while (input.hasNextLine()) {
			String str = input.nextLine();
			if (ct > 0) {
				@SuppressWarnings("deprecation")
				StrTokenizer strTok = new StrTokenizer(str, ',');
				
				for (int i = 0; i < 17; i++) {
					strTok.nextToken();
				}
				double pred = new Double(strTok.nextToken());
				p.input(pred);
			}
			ct++;
		}



		
	}
	

}

