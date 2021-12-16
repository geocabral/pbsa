package teste;

import org.apache.commons.math3.distribution.BetaDistribution;

import moa.options.ClassOption;
import moa.tasks.EvaluatePrequential;
import moa.tasks.MainTask;
import moa.tasks.TaskThread;

public class Run {

	static MainTask currentTask = new EvaluatePrequential();
	
	public static void main(String[] args){
//		try {
//			
//			String task = "EvaluatePrequential .....";
//			currentTask = (MainTask) ClassOption.cliStringToObject(task, MainTask.class, null);
//
//			TaskThread thread = new TaskThread((moa.tasks.Task) currentTask);
//
//			thread.start();
//
//
//		} catch (Exception ex) {
//			ex.printStackTrace();
//		}
		
		BetaDistribution b = new BetaDistribution(2, 5);
		
		for(int i = 0; i < 50; i++){
			System.out.println(new Double((1-b.sample())*918).intValue());
		}
		
		
		
	}
	
	
}
