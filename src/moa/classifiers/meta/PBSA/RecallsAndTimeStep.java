package moa.classifiers.meta.PBSA;

public class RecallsAndTimeStep{
	
	private long timeStep;
	private double recall1;
	private double recall0;
	//oversampling enhancement factor
	private double oef;
	
	public RecallsAndTimeStep(long _t, double r0, double r1){
		timeStep = _t;
		recall0 = r0;
		recall1 = r1;
		setOef();
	}

	public long getTimeStep() {
		return timeStep;
	}

	public double getRecall1() {
		return recall1;
	}

	public double getRecall0() {
		return recall0;
	}

	public void setRecalls(double r0, double r1){
		recall0 = r0;
		recall1 = r1;
	}
	
	public double getOef() {
		setOef();
		return oef;
	}

	//set the OEF
	public void setOef() {
		double diffRecalls = recall0 - recall1;
		
		if(diffRecalls > 0.01){
			
			
			if(diffRecalls  < 0.7){
				oef = (diffRecalls * 5) + 1;	
			}else{
				oef = 5;
			}
			
//			if(diffRecalls > 0.65){
//				oef = 5.5;
//			}
//			
//			if(diffRecalls < 0.6){
//				oef = 4.8;
//			}
//			
//			if(diffRecalls < 0.5){
//				oef = 4.0;
//			}
//			
//			if(diffRecalls < 0.4){
//				oef = 3.3;
//			}
//			
//			if(diffRecalls < 0.3){
//				oef = 2.7;
//			}
//			
//			if(diffRecalls < 0.2){
//				oef = 2.2;
//			}
//			
//			if(diffRecalls < 0.1){
//				oef = 1.8;
//			}
			
		}else{
			oef = 1;
		}
		
	}
	
	
}


