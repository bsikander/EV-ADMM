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
		
		//TODO: Get the parameters from arguments and set them otherwise set the default values
		job.set(Constants.EVADMM_MAX_ITERATIONS, "4");
		job.set(Constants.EVADMM_EV_COUNT, "4");
		job.set(Constants.EVADMM_OUTPUT_PATH, "/Users/raja/Documents/workspace/Hama-EVADMM/output/");
		job.set(Constants.EVADMM_AGGREGATOR_PATH, "/Users/raja/Documents/Thesis/ADMM_matlab/Aggregator/aggregator.mat");
		job.set(Constants.EVADMM_EV_PATH, "/Users/raja/Documents/Thesis/ADMM_matlab/EVs/home/");
		job.set(Constants.EVADMM_RHO, "0.01");
		job.set(Constants.EVADMM_BSP_TASK, "4");
		
		job.setBspClass(EVADMMBsp.class);
		job.setJarByClass(EVADMMBsp.class);
		job.setJobName("EVADMM");
		
		//job.setNumBspTask(4);
		//job.setOutputPath(new Path("/Users/raja/Documents/workspace/Hama-EVADMM/output/"));
		job.setNumBspTask (Integer.parseInt(job.get(Constants.EVADMM_BSP_TASK)) );
		job.setOutputPath(new Path(job.get(Constants.EVADMM_OUTPUT_PATH)));
		
		
		job.waitForCompletion(true);
		System.out.print("end");
	}

}
