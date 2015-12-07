package admm;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.fs.Path;
import org.apache.hama.HamaConfiguration;
import org.apache.hama.bsp.BSPJob;
import org.apache.hama.bsp.BSPJobClient;
import org.apache.hama.bsp.ClusterStatus;
import org.apache.hama.bsp.FileInputFormat;
import org.apache.hama.bsp.NullInputFormat;
import org.apache.hama.bsp.TextInputFormat;

/*
 * The main entry point of the algorithm. This function takes multiple command line arguments to change the behavior of the algorithm.
 * It also starts the BSP class.
 */
public class Main {
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException, URISyntaxException {		
		
		try
		{
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
	
		job.set(Constants.EVADMM_MAX_ITERATIONS, "400");
		job.set(Constants.EVADMM_EV_COUNT, "1000");
		job.set(Constants.EVADMM_OUTPUT_PATH, "/Users/raja/Documents/workspace/Hama-EVADMM/output/");
		job.set(Constants.EVADMM_AGGREGATOR_PATH, "/Users/raja/Documents/Thesis/ADMM_matlab/Aggregator/aggregator.mat");
		job.set(Constants.EVADMM_EV_PATH, "/Users/raja/Documents/Thesis/ADMM_matlab/Valley_Filling_1.1/Jose/EVs/home/");
		job.set(Constants.EVADMM_RHO, "0.01");
		job.set(Constants.EVADMM_BSP_TASK, "3");
		
		//String inputPath = "/Users/raja/Documents/workspace/Hama-EVADMM/empty.txt,/Users/raja/Documents/workspace/Hama-EVADMM/EVs_new1.txt";
		String inputPath = "/Users/raja/Documents/workspace/Hama-EVADMM/empty.txt,/Users/raja/Documents/workspace/Hama-EVADMM/EVs_1000.txt";
		FileInputFormat.addInputPaths(job, inputPath);
		job.setInputFormat(TextInputFormat.class);
		
		job.set(Constants.EVADMM_INPUT_PATH, inputPath);
		
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
		//job.setInputFormat(NullInputFormat.class);
		job.setOutputPath(new Path(job.get(Constants.EVADMM_OUTPUT_PATH)));
		System.out.println("Starting the job");
		
		long lStartTime = System.nanoTime();
		
		job.waitForCompletion(true);
		
		long lEndTime = System.nanoTime();
		long difference = lEndTime - lStartTime;
		long milliseconds = difference/1000000;
		System.out.println("Elapsed milliseconds: " + milliseconds);
		System.out.println("Elapsed seconds: " + milliseconds/1000);
		System.out.println("Elapsed time: " + String.format("%02d:%02d:%02d", 
			    TimeUnit.MILLISECONDS.toHours(milliseconds),
			    TimeUnit.MILLISECONDS.toMinutes(milliseconds) - 
			    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds)),
			    TimeUnit.MILLISECONDS.toSeconds(milliseconds) - 
			    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds))));
		
		System.out.print("end");
		}
		catch(OutOfMemoryError e)
		{
			System.out.println("Out of Memory Exception -> " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/*
	 * In case user is not using the command line parameters properly. This function prints the correct usage information.
	 */
	private static void printUsage() {
	    System.out.println("Usage: <input_aggregator> <input_ev> <output> "
	        + "[maximum iterations (default 4)] [ev count (default 4)] [bsp tasks (default 2)] [rho (default 0.01)]");
	    System.exit(-1);
	  }

}
