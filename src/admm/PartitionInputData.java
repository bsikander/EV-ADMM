package admm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class PartitionInputData {
	
	public static List<String> partitionData(Path fileToPartition, int totalSlaveGroomTasks, int totalEVs, Configuration conf, String hdfsPartitionFilePath)
	{
		List<File> tempFiles = new ArrayList<File>(totalSlaveGroomTasks);
        List<BufferedWriter> writers = new ArrayList<BufferedWriter>(totalSlaveGroomTasks);
        
        List<String> partitionedFilePath = new ArrayList<String>(totalSlaveGroomTasks);
		
		try
		{
			// 1- Create a temp file for each groom
			// 2- Add the path of that temp file to a list
			// 3- Add a buffer writer for that file to a seperate list
			for(int i = 0; i < totalSlaveGroomTasks; i++) {
				//System.out.println("part-" +  i + "-");
				
				File tempFile = File.createTempFile("part-" +  i + "-", ".txt");
				tempFiles.add(tempFile);
				
				BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
				writers.add(writer);
			}
			
            FileSystem fs = FileSystem.get(conf);
            
            if (!fs.exists(fileToPartition))
            	throw new RuntimeException();
            
            FSDataInputStream data = fs.open(fileToPartition);
            BufferedReader br = new BufferedReader(new InputStreamReader(data));
            
            String line = null;
            int currentGroomSlaveTask = 0;
            
            // Divide all the lines in the main file to all the temp files
            // Or until the totalEVs have been reached
            while ((line = br.readLine()) != null && totalEVs != 0) { 
                BufferedWriter bw = writers.get( currentGroomSlaveTask % totalSlaveGroomTasks );
                bw.write(line);
                bw.newLine();
                
                totalEVs--;
                currentGroomSlaveTask++;
            }
            
            // Cleanup everything and save the temps files back to HDFS
            for(int i = 0; i < totalSlaveGroomTasks; i++) 
            {
            	writers.get(i).close();
            	fs.copyFromLocalFile(new Path(tempFiles.get(i).getAbsolutePath()), new Path(hdfsPartitionFilePath)); //Copy the temp file to HDFS
            	
            	partitionedFilePath.add(hdfsPartitionFilePath + tempFiles.get(i).getName());
            	tempFiles.get(i).delete();
            }
            data.close();
            
            
            //TO DELETE A FILE FROM HDFS
            //fs.delete(p, true);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			
		}
		
		return partitionedFilePath;
	}
}
