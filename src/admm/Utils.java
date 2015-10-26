package admm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hama.bsp.BSPPeer;
import org.codehaus.jackson.map.ObjectMapper;
import org.mortbay.log.Log;

import com.google.common.io.ByteStreams;
import com.jmatio.io.MatFileReader;
import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;

public class Utils {
	
	public static MasterData LoadMasterDataFromMatFile(String filePath, Configuration conf)
	{
		File tempMatFile = null;
		try
		{
			//File tempMatFile = getFileFromHDFS(conf, filePath);
			tempMatFile = getFileFromHDFS(conf, filePath);
			MatFileReader matfilereader = new MatFileReader(tempMatFile);
			
			double[][] reArray = ((MLDouble)matfilereader.getMLArray("re")).getArray(); //Conversion
			double[][] DArray = ((MLDouble)matfilereader.getMLArray("D")).getArray(); //Conversion
			double[][] priceArray = ((MLDouble)matfilereader.getMLArray("price")).getArray();
			
			MasterData context = new MasterData(
												getSingleArrayFromDouble(reArray),
												getSingleArrayFromDouble( DArray ), 
												priceArray[0]
												);
			
			return context;
		}
		catch(Exception e)
		{
			System.out.println("Exception in LoadMasterData function in Utils" + e.getMessage() + " == filePath: " + filePath);
			return null;
		}
		finally {
			if(tempMatFile != null) {
				tempMatFile.delete();
			}
		}
	}
	
	public static MasterDataValley LoadMasterDataValleyFillingFromMatFile(String filePath, Configuration conf)
	{
		File tempMatFile = null;
		try
		{
			//File tempMatFile = getFileFromHDFS(conf, filePath);
			tempMatFile = getFileFromHDFS(conf, filePath);
			MatFileReader matfilereader = new MatFileReader(tempMatFile);
			
			//double[][] reArray = ((MLDouble)matfilereader.getMLArray("re")).getArray(); //Conversion
			double[][] DArray = ((MLDouble)matfilereader.getMLArray("D")).getArray(); //Conversion
			double[][] priceArray = ((MLDouble)matfilereader.getMLArray("price")).getArray();
			
			MasterDataValley context = new MasterDataValley(
												getSingleArrayFromDouble( DArray ),
												priceArray[0]
												);
			
			return context;
		}
		catch(Exception e)
		{
			System.out.println("Exception in LoadMasterDataValley function in Utils " + e.getMessage() + " == filePath: " + filePath);
			return null;
		}
		finally {
			if(tempMatFile != null) {
				tempMatFile.delete();
			}
		}
	}
	
	private static FileSystem getFSObject(Configuration conf) throws IOException, URISyntaxException
	{
		FileSystem fs = FileSystem.get(conf);
		return fs;
	}
	
	private static File getFileFromHDFS(Configuration conf, String filePath) throws IOException, URISyntaxException {
		FileSystem fs = getFSObject(conf);
		FSDataInputStream in = fs.open(new Path(filePath));
		File tempMatFile = stream2file(in, filePath.split("/")[filePath.split("/").length - 1]);
		
		//fs.close(); //Added on 27th August
		in.close();
		
		return tempMatFile;
	}
	
	public static SlaveData LoadSlaveDataFromMatFile(String filePath, boolean isFirstIteration, BSPPeer<NullWritable, NullWritable,IntWritable, Text, Text> peer) throws IOException
	{
		File tempMatFile = null;
		try
		{	
			//System.out.println("FILE PATH: ->" + filePath);
			//File tempMatFile = getFileFromHDFS(peer.getConfiguration(), filePath);
			tempMatFile = getFileFromHDFS(peer.getConfiguration(), filePath);
	
			//peer.write(new IntWritable(1), new Text("Inside LOAD SLAVE DATA FROM MAT FILE"));
			MatFileReader matfilereader = new MatFileReader(tempMatFile);
			
			double[][] dArray = ((MLDouble)matfilereader.getMLArray("d")).getArray();
			double[][] AArray = ((MLDouble)matfilereader.getMLArray("A")).getArray();
			double[][] BArray = ((MLDouble)matfilereader.getMLArray("B")).getArray();
			double[][] RArray = ((MLDouble)matfilereader.getMLArray("R")).getArray();
			double[][] SmaxArray = ((MLDouble)matfilereader.getMLArray("S_max")).getArray(); //Conversion
			double[][] SminArray = ((MLDouble)matfilereader.getMLArray("S_min")).getArray(); //Conversion
			
			double[][] x_optimal = new double[dArray[0].length][1];
			if(matfilereader.getMLArray("x_optimal") == null || isFirstIteration == true) {
				peer.write(new IntWritable(1), new Text("X_OTIMAL NOT FOUND .. WRITING ZERO"));
				for(int i=0; i< dArray[0].length;i++) {
					x_optimal[i][0] = 0;
				}
			}
			else {
				x_optimal = ((MLDouble)matfilereader.getMLArray("x_optimal")).getArray(); //Conversion
				
				peer.write(new IntWritable(1), new Text("X_OPTIMAL FOUND"));
				//Utils.PrintArray(getSingleArrayFromDouble(x_optimal));
			}
			
			peer.write(new IntWritable(1), new Text(Utils.convertDoubleArrayToString(getSingleArrayFromDouble(x_optimal))));
			
			SlaveData context = new SlaveData(
											dArray[0], 
											AArray[0], 
											BArray, 
											RArray[0][0],
											getSingleArrayFromDouble(SmaxArray),
											getSingleArrayFromDouble(SminArray),
											getSingleArrayFromDouble(x_optimal)
											);
			
			return context;
		}
		catch(Exception e)
		{
			System.out.println("Exception in LoadSlaveDataFromMatFile function in Utils -> FILE: " + filePath + " -- EXCEPTION->" + e.getMessage());
			peer.write(new IntWritable(1), new Text(e.getMessage()));
			return null;
		}
		finally {
			if(tempMatFile != null) {
				tempMatFile.delete();
			}
		}
	}
	
	public static void SlaveXToMatFile(String filePath, double[] x, Configuration conf)
	{
		File tempMatFile = null;
		try
		{
			MatFileWriter matfileWriter = new MatFileWriter();
			//File tempMatFile = getFileFromHDFS(conf, filePath);
			tempMatFile = getFileFromHDFS(conf, filePath);
			MatFileReader matfileReader = new MatFileReader(tempMatFile);
			
			List<MLArray> list = new ArrayList<MLArray>();
			list.add((matfileReader.getMLArray("d")));
			list.add(matfileReader.getMLArray("A"));
			list.add(matfileReader.getMLArray("B"));
			list.add(matfileReader.getMLArray("R"));
			list.add(matfileReader.getMLArray("S_max")); //Conversion
			list.add(matfileReader.getMLArray("S_min")); //Conversion
			
			double[][] xDoubleArray = new double[x.length][1];
			for(int i =0; i< x.length; i++) {
				xDoubleArray[i][0] = x[i];
			}	
			
			MLArray xMLArray = new MLDouble("x_optimal",xDoubleArray);
			list.add(xMLArray);
			
			
			final File tempFile = File.createTempFile("saveTemp", ".tmp"); //Create a temp file
			tempFile.deleteOnExit();
			String tempPath = tempFile.getAbsolutePath(); //Get absolute path of temp file
			
			matfileWriter.write(tempPath, list); //Write to temp file
			
			FileSystem fs = getFSObject(conf);
			fs.copyFromLocalFile(new Path(tempPath), new Path(filePath)); //Copy the temp file to HDFS
			
		}
		catch(Exception e)
		{
			System.out.println("Exception in SlaveXToMatFile function in Utils -> FilePath -> " + filePath + " Exception" + e.getMessage());
		}
		finally {
			if(tempMatFile != null) {
				tempMatFile.delete();
			}
		}
	}
	
	public static File stream2file (InputStream in, String fileName) throws IOException {
        final File tempFile = File.createTempFile(fileName, ".tmp");
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            //IOUtils.copy(in, out);
        	ByteStreams.copy(in, out);
        }
        catch(Exception e) {
        	System.out.println("EXCEPTION stream2file -> " + e.getMessage());
        }
        return tempFile;
    }
	
	public  static String convertDoubleArrayToString(double[] arr) {
		String result = "";
		for(int i=0; i< arr.length; i++){
			result += arr[i];
		}
		return result;
	}
	
	public static double[] getSingleArrayFromDouble(double[][] dArray)
	{
		double[] sArray = new double[dArray.length];
		int i = 0;
		for(double[] d: dArray)
		{
			//sArray[i] = d[0];
			sArray[i] = Utils.round(d[0]);
			i++;
		}
		
		return sArray;
	}
	
	public static double[] getZeroArray(int size)
	{
		double[] zeroArray = new double[size];
		for(int i=0; i< size; i++)
		{
			zeroArray[i] = 0;
		}
		
		return zeroArray;
	}
	
	public static double[] getArrayWithData(int size, int data)
	{
		double[] zeroArray = new double[size];
		for(int i=0; i< size; i++)
		{
			zeroArray[i] = data;
		}
		
		return zeroArray;
	}
	
	public static double[][] getZeroDoubleArray(int row, int col)
	{
		double[][] zeroArray = new double[row][col];
		for(int i=0; i< row; i++)
		{
			for(int j=0; j< col; j++)
			{
				zeroArray[i][j] = 0.0;
			}
		}
		return zeroArray;
	}

	public static double[] scalerMultiply(double[] arr, double multiplier)
	{	
		double[] newArr = new double[arr.length];
		for(int i=0; i < arr.length; i++)
		{
			newArr[i] = arr[i]*multiplier;
		}
		
		return newArr;
	}
	
	public static double[] scalerAdd(double[] arr, double add)
	{
		double[] newArr = new double[arr.length];
		for(int i=0; i < arr.length; i++)
		{
			newArr[i] = arr[i]+add;
		}
		
		return newArr;
	}
	
	public static double[] vectorAdd(double[] arr1, double[] arr2)
	{
		double[] sumArr = new double[arr1.length];
		
		for(int i=0; i < arr1.length; i++)
		{
			sumArr[i] = round(arr1[i] + arr2[i]);
		}
		
		return sumArr;
	}
	
	public static double[][] setColumnInMatrix(double[][] original, double[] column, int columnId)
	{	
		for(int i=0; i < column.length; i++)
		{
			original[i][columnId] = round(column[i]);
		}
		
		return original;
	}
	
	public static double[] getColumn(double[][] original, int columnId)
	{
		double[] column = new double[original.length];
		for(int i=0; i < column.length; i++)
		{
			column[i] = original[i][columnId];
		}
		
		return column;
	}
	
	public static double[] calculateMean(double[] xMasterOptimal, double[] xSlaveAverageOptimal, int evCount)
	{	
		double [] average = Utils.vectorAdd(xMasterOptimal, xSlaveAverageOptimal);
		
		for(int i =0; i< xMasterOptimal.length; i++)
			average[i] = round(average[i]/(double)evCount);
		return average;
	}
	
	public static double calculateMean(double[] vector)
	{
		double mean = 0;
		
		for(int i =0; i< vector.length; i++)
			mean = mean + vector[i];
		
		return round(mean/vector.length);
		
	}
	
	public static double round(double value) {
//		int places = Constants.ROUND_PLACES;
//	    if (places < 0) throw new IllegalArgumentException();
//
//	    BigDecimal bd = new BigDecimal(value);
//	    bd = bd.setScale(places, RoundingMode.HALF_UP);
//	    return bd.doubleValue();
		return value;
	}
	
	public static double[] calculateSumOfEVOptimalValue(double[][] matrix)
	{
		double[] average = new double[matrix.length];
		double rowSum = 0;
		for(int i =0; i< matrix.length; i++)
		{
			for(int j=0; j< matrix[0].length - 1;j++)
			{
				rowSum += matrix[i][j];
			}
			average[i] = rowSum;
			
			rowSum = 0;
		}
		
		return average;
	}

	public static double[] calculateSum(double[][] matrix)
	{
		double[] sumArray = new double[matrix.length];
		double rowSum = 0;
		
		for(int i =0; i< matrix.length; i++)
		{
			for(int j=0; j< matrix[0].length;j++)
			{
				rowSum += matrix[i][j];
			}
			sumArray[i] = rowSum;
			
			rowSum = 0;
		}
		
		return sumArray;
		
	}
	
	public static double[][] calculateMatrixSubtraction(double[][] mat1, double[][] mat2) {
		double[][] subtractedMatrix = new double[mat1.length][mat1[0].length];
		
		for(int i=0; i< mat1.length; i++)
		{
			for(int j=0; j< mat1[0].length; j++)
			{
				subtractedMatrix[i][j] = mat1[i][j] - mat2[i][j];
			}
		}
		
		return subtractedMatrix;
	}
	
	public static double[] calculateVectorSubtraction(double[] vec1, double[] vec2) {
		double[] subtractedMatrix = new double[vec1.length];
		
		for(int i=0; i< vec1.length; i++)
			subtractedMatrix[i] = vec1[i] - vec2[i];
		
		return subtractedMatrix;
	}

	public static double[] addMatrixAndVector(double[][] mat, double[] vec)
	{
		int count = 0;
		double[] result = new double[mat.length*mat[0].length];
		for(int i=0; i< mat[0].length ; i++)
		{
			for(int j=0; j < mat.length; j++)
			{
				result[count] = mat[j][i] + vec[j];
				count++;
			}
		}
		
		return result;
	}
	
	public static double calculateNorm(double[] vec)
	{
		double result = 0.0;
		for(int i =0; i< vec.length;i++) {
			result += (vec[i]*vec[i]);
		}
		
		return Math.sqrt(result);
	}
	
	public static double[] scaleVector(double[] vec, int N)
	{
		double[] scaledVec = new double[vec.length*N];
		int count=0;
		for(int i=0 ; i< N; i++)
		{
			for(int j=0; j< vec.length; j++)
			{
				scaledVec[count] = vec[j];
				count++;
			}
		}
		return scaledVec;
	}
	
	public static void PrintArray(double[] input)
	{		
		System.out.println("=====Priting Array=====");
		for(int i =0; i < input.length; i++)
		{		
			System.out.print(input[i] + " ");
		}
		System.out.println("=====END=====");
	}
	
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
    public static String networkMasterToJson(NetworkObjectMaster context) throws IOException {
        return OBJECT_MAPPER.writeValueAsString(context);
    }

    public static String networkSlaveToJson(NetworkObjectSlave context) throws IOException {
        return OBJECT_MAPPER.writeValueAsString(context);
    }
    
    public static NetworkObjectMaster jsonToNetworkMaster(String json) throws IOException {
        return OBJECT_MAPPER.readValue(json, NetworkObjectMaster.class);
    }

    public static NetworkObjectSlave jsonToNetworkSlave(String json) throws IOException {
        return OBJECT_MAPPER.readValue(json, NetworkObjectSlave.class);
    }
}
