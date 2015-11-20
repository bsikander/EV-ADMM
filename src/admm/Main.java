package admm;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hama.HamaConfiguration;
import org.apache.hama.bsp.BSPJob;
import org.apache.hama.bsp.BSPJobClient;
import org.apache.hama.bsp.ClusterStatus;
import org.apache.hama.bsp.FileInputFormat;
import org.apache.hama.bsp.NullInputFormat;
import org.apache.hama.bsp.TextInputFormat;

public class Main {
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException, URISyntaxException {		
		HamaConfiguration conf = new HamaConfiguration();
		BSPJob job = new BSPJob(conf, Main.class);
		
		if(args.length < 3) {
			printUsage();
		}
	
		for(String s: args) {
			System.out.println(s);
		}
			
		job.setBspClass(EVADMMBsp.class);
		job.setJarByClass(EVADMMBsp.class);
		job.setJobName("EVADMM");
	
		job.set(Constants.EVADMM_MAX_ITERATIONS, "5");
		job.set(Constants.EVADMM_EV_COUNT, "3");
		job.set(Constants.EVADMM_OUTPUT_PATH, "/Users/raja/Documents/workspace/Hama-EVADMM/output/");
		job.set(Constants.EVADMM_AGGREGATOR_PATH, "/Users/raja/Documents/Thesis/ADMM_matlab/Aggregator/aggregatorD.mat");
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
		BSPJobClient jobClient = new BSPJobClient(conf);
		ClusterStatus cluster = jobClient.getClusterStatus(true);
		System.out.println("Max bsp task:" + cluster.getMaxTasks());
		job.setInputFormat(NullInputFormat.class);
		job.setOutputPath(new Path(job.get(Constants.EVADMM_OUTPUT_PATH)));
		System.out.println("Starting the job");
		job.waitForCompletion(true);
		System.out.print("end");
	}
	
	private static void printUsage() {
	    System.out.println("Usage: <input_aggregator> <input_ev> <output> "
	        + "[maximum iterations (default 4)] [ev count (default 4)] [bsp tasks (default 2)] [rho (default 0.01)]");
	    System.exit(-1);
	  }

}
