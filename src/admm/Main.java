package admm;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hama.HamaConfiguration;
import org.apache.hama.bsp.BSPJob;
import org.apache.hama.bsp.FileInputFormat;
import org.apache.hama.bsp.TextInputFormat;

public class Main {
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException, URISyntaxException {		
		HamaConfiguration conf = new HamaConfiguration();
		BSPJob job = new BSPJob(conf);
		
//		//TESTING HDFS ACCESS DIRECTLY FROM CODE - IT WORKS
//		
//		//System.out.println(conf..toString());
//		FileSystem fs = FileSystem.get(new URI("hdfs://localhost:54310/"), conf);
//		Path workingPath = fs.getWorkingDirectory();
//		System.out.println(workingPath.toString());
//		fs.printStatistics();
//		FileStatus test = fs.getFileStatus(new Path("test/1.mat"));
//		System.out.println(test.getPath().toString());
//		//fs.get(new URI("hdfs://localhost:54310/user/raja/test/1.mat"), conf);
//		
//		//TESTING HDFS ACCESS DIRECTLY FROM CODE - IT WORKS - END
		
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
