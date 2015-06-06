package admm;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hama.HamaConfiguration;
import org.apache.hama.bsp.BSPJob;
import org.apache.hama.bsp.FileInputFormat;
import org.apache.hama.bsp.TextInputFormat;

public class Main {
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
//		MasterContext mContext = Utils.LoadMasterDataFromMatFile("/Users/raja/Documents/Thesis/ADMM_matlab/Aggregator/aggregator.mat");
//		SlaveContext sContext = Utils.LoadSlaveDataFromMatFile("/Users/raja/Documents/Thesis/ADMM_matlab/EVs/home/1.mat");
//		MasterContext m = new MasterContext();
//		double[] val = m.getxa_min();
		
		HamaConfiguration conf = new HamaConfiguration();
		BSPJob job = new BSPJob(conf);
		
		job.setBspClass(EVADMMBsp.class);
		job.setJarByClass(EVADMMBsp.class);
		job.setJobName("EVADMM");
		job.setNumBspTask(4);
		job.setOutputPath(new Path("/Users/raja/Documents/workspace/Hama-EVADMM/output/"));
		
		job.waitForCompletion(true);
		System.out.print("end");
	}

}
