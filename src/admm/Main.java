package admm;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hama.HamaConfiguration;
import org.apache.hama.bsp.BSPJob;
import org.apache.hama.bsp.FileInputFormat;
import org.apache.hama.bsp.TextInputFormat;

public class Main {
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {		
		HamaConfiguration conf = new HamaConfiguration();
		BSPJob job = new BSPJob(conf);
		
		if(args.length < 3) {
			printUsage();
		}
	
		for(String s: args) {
			System.out.println(s);
		}
		
		job.setBspClass(EVADMMBsp.class);
		job.setJarByClass(EVADMMBsp.class);
		job.setJobName("EVADMM");
		
		job.set(Constants.EVADMM_MAX_ITERATIONS, "4");
		job.set(Constants.EVADMM_EV_COUNT, "4");
		job.set(Constants.EVADMM_OUTPUT_PATH, "/Users/raja/Documents/workspace/Hama-EVADMM/output/");
		job.set(Constants.EVADMM_AGGREGATOR_PATH, "/Users/raja/Documents/Thesis/ADMM_matlab/Aggregator/aggregator.mat");
		job.set(Constants.EVADMM_EV_PATH, "/Users/raja/Documents/Thesis/ADMM_matlab/EVs/home/");
		job.set(Constants.EVADMM_RHO, "0.01");
		job.set(Constants.EVADMM_BSP_TASK, "2");
		
		if(args.length >=7)
			job.set(Constants.EVADMM_RHO, args[6]);
		if(args.length >=6)
			job.set(Constants.EVADMM_BSP_TASK, args[5]);
		if(args.length >=5)
			job.set(Constants.EVADMM_EV_COUNT, args[4]);
		if(args.length >=4)
			job.set(Constants.EVADMM_MAX_ITERATIONS, args[3]);
		if(args.length >=3)
			job.set(Constants.EVADMM_OUTPUT_PATH, args[2]);
		if(args.length >=2)
			job.set(Constants.EVADMM_EV_PATH, args[1]);
		if(args.length >=1)
			job.set(Constants.EVADMM_AGGREGATOR_PATH, args[0]);
		
		job.setNumBspTask (Integer.parseInt(job.get(Constants.EVADMM_BSP_TASK)) );
		job.setOutputPath(new Path(job.get(Constants.EVADMM_OUTPUT_PATH)));
		
		job.waitForCompletion(true);
		System.out.print("end");
	}
	
	private static void printUsage() {
	    System.out.println("Usage: <input_aggregator> <input_ev> <output> "
	        + "[maximum iterations (default 4)] [ev count (default 4)] [bsp tasks (default 2)] [rho (default 0.01)]");
	    System.exit(-1);
	  }

}
