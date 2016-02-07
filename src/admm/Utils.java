package admm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hama.bsp.BSPPeer;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.io.ByteStreams;
import com.jmatio.io.MatFileReader;
import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;

/*
 * This class holds all the static utility functions.
 */
public class Utils {
	
	private Utils() {}
	
	/*
	 * This function loads the aggregator mat file from file system and populates the MasterData object.
	 */
	public static MasterData LoadMasterDataFromMatFile(String filePath, Configuration conf)
	{
		File tempMatFile = null;
		try
		{	
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
	
	/*
	 * This function loads the aggregator mat file from file system and populates the MasterData object.
	 */
	public static MasterDataValley LoadMasterDataValleyFillingFromMatFile(String filePath, Configuration conf)
	{
		File tempMatFile = null;
		try
		{	
			tempMatFile = getFileFromHDFS(conf, filePath);
			MatFileReader matfilereader = new MatFileReader(tempMatFile);
			
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
	
	/*
	 * This function returns the file system object.
	 */
	private static FileSystem getFSObject(Configuration conf) throws IOException, URISyntaxException
	{
		FileSystem fs = FileSystem.get(conf);
		return fs;
	}
	
	/*
	 * This function reads and returns the file it reads from HDFS.
	 */
	public static File getFileFromHDFS(Configuration conf, String filePath) throws IOException, URISyntaxException {
		FileSystem fs = getFSObject(conf);
		FSDataInputStream in = fs.open(new Path(filePath));
		File tempMatFile = stream2file(in, filePath.split("/")[filePath.split("/").length - 1]);
		
		//fs.close(); //Added on 27th August
		in.close();
		
		return tempMatFile;
	}
	
	/*
	 * This function loads the EVs from mat file into SlaveData object.
	 */
	public static SlaveData LoadSlaveDataFromMatFile(String filePath, BSPPeer<LongWritable, Text,IntWritable, Text, Text> peer) throws IOException
	{
		File tempMatFile = null;
		try
		{	
			tempMatFile = getFileFromHDFS(peer.getConfiguration(), filePath);
			
			MatFileReader matfilereader = new MatFileReader(tempMatFile);
			
			double[][] dArray = ((MLDouble)matfilereader.getMLArray("d")).getArray();
			double[][] AArray = ((MLDouble)matfilereader.getMLArray("A")).getArray();
			double[][] BArray = ((MLDouble)matfilereader.getMLArray("B")).getArray();
			double[][] RArray = ((MLDouble)matfilereader.getMLArray("R")).getArray();
			double[][] SmaxArray = ((MLDouble)matfilereader.getMLArray("S_max")).getArray(); //Conversion
			double[][] SminArray = ((MLDouble)matfilereader.getMLArray("S_min")).getArray(); //Conversion
			
//			double[][] x_optimal = new double[dArray[0].length][1];
//			if(matfilereader.getMLArray("x_optimal") == null || isFirstIteration == true) {
//				peer.write(new IntWritable(1), new Text("X_OTIMAL NOT FOUND .. WRITING ZERO"));
//				for(int i=0; i< dArray[0].length;i++) {
//					x_optimal[i][0] = 0;
//				}
//			}
//			else {
//				x_optimal = ((MLDouble)matfilereader.getMLArray("x_optimal")).getArray(); //Conversion
//				
//				peer.write(new IntWritable(1), new Text("X_OPTIMAL FOUND"));
//				//Utils.PrintArray(getSingleArrayFromDouble(x_optimal));
//			}
//			
//			peer.write(new IntWritable(1), new Text(Utils.convertDoubleArrayToString(getSingleArrayFromDouble(x_optimal))));
			
			SlaveData context = new SlaveData(
											dArray[0], 
											AArray[0], 
											//BArray, 
											RArray[0][0]//,
											//getSingleArrayFromDouble(SmaxArray),
											//getSingleArrayFromDouble(SminArray)
											);
			
			matfilereader = null;
			
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
	
	/*
	 * This function stores the optimal X value back to mat file. This function is not used anymore because writing data to
	 * takes too much time.
	 */
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
			//tempFile.delete()
			String tempPath = tempFile.getAbsolutePath(); //Get absolute path of temp file
			
			matfileWriter.write(tempPath, list); //Write to temp file
			
			FileSystem fs = getFSObject(conf);
			fs.copyFromLocalFile(new Path(tempPath), new Path(filePath)); //Copy the temp file to HDFS
			
			tempFile.delete();
			matfileWriter = null;
			
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
	
	public static double[] getArray(String input) {
		double[] arr;
		
		input = input.substring(1,input.length() - 1); //remove [ ] symbols
		String[] values = input.split(",");
		arr = new double[values.length];
		
		int index = 0;
		for(String s: values) {
			arr[index] = Double.parseDouble(s);
			index ++;
		}
		return arr;
	}
	
	public static double[][] getDoubleArray(String input) {
		double[][] arr;
		
		String[] values = input.split("]");
		
		int index =0;
		arr = new double[values.length][];
		
		for(String s: values) {
			s += "]";
			arr[index] = getArray(s);
			index++;
		}
		return arr;
	}
	
	/*
	 * This function reads a file from HDFS into a java File object.
	 */
	public static File stream2file(InputStream in, String fileName) throws IOException {
        final File tempFile = File.createTempFile(fileName, ".tmp");
        tempFile.deleteOnExit();
        
        FileOutputStream out = new FileOutputStream(tempFile);
        try {
        	ByteStreams.copy(in, out);
        }
        catch(Exception e) {
        	System.out.println("EXCEPTION stream2file -> " + e.getMessage());
        }
        finally
        {
        	out.close();
        	out = null;
        }
        return tempFile;
    }

	/*
	 * For debugging purposes, this function converts double array to string.
	 */
	public  static String convertDoubleArrayToString(double[] arr) {
		String result = "";
		for(int i=0; i< arr.length; i++){
			result += arr[i];
		}
		return result;
	}
	
	/*
	 * While loading the EVs, the mat reader returns a double array e.g 96 * 1. This function converts that array
	 * to a single array.
	 */
	public static double[] getSingleArrayFromDouble(double[][] dArray)
	{
		double[] sArray = new double[dArray.length];
		int i = 0;
		for(double[] d: dArray)
		{	
			sArray[i] = Utils.round(d[0]);
			i++;
		}
		
		return sArray;
	}
	
	/*
	 * This function returns an array of a specified size with all 0's as the default value.
	 */
	public static double[] getZeroArray(int size)
	{
		double[] zeroArray = new double[size];
		for(int i=0; i< size; i++)
		{
			zeroArray[i] = 0;
		}
		
		return zeroArray;
	}
	
	/*
	 * This function returns a double array of a specific size and data specified by user. 
	 */
	public static double[] getArrayWithData(int size, int data)
	{
		double[] zeroArray = new double[size];
		for(int i=0; i< size; i++)
		{
			zeroArray[i] = data;
		}
		
		return zeroArray;
	}
	
	/*
	 * This function returns a double array of a specific size with all zeros.
	 */
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

	/*
	 * This function multiplies all the index of array with a specific value specified by user.
	 */
	public static double[] scalerMultiply(double[] arr, double multiplier)
	{	
		double[] newArr = new double[arr.length];
		for(int i=0; i < arr.length; i++)
		{
			newArr[i] = arr[i]*multiplier;
		}
		
		return newArr;
	}
	
	/*
	 * This function adds a value specified by user to each element of array.
	 */
	public static double[] scalerAdd(double[] arr, double add)
	{
		double[] newArr = new double[arr.length];
		for(int i=0; i < arr.length; i++)
		{
			newArr[i] = arr[i]+add;
		}
		
		return newArr;
	}
	
	/*
	 * This function adds two double arrays and returns the added array.
	 */
	public static double[] vectorAdd(double[] arr1, double[] arr2)
	{
		double[] sumArr = new double[arr1.length];
		
		for(int i=0; i < arr1.length; i++)
		{
			sumArr[i] = round(arr1[i] + arr2[i]);
		}
		
		return sumArr;
	}
	
	/*
	 * This function replaces the whole column in a double array with an array array specified by user.
	 */
	public static double[][] setColumnInMatrix(double[][] original, double[] column, int columnId)
	{	
		for(int i=0; i < column.length; i++)
		{
			original[i][columnId] = round(column[i]);
		}
		
		return original;
	}
	
	/*
	 * This function returns a column from the double array.
	 */
	public static double[] getColumn(double[][] original, int columnId)
	{
		double[] column = new double[original.length];
		for(int i=0; i < column.length; i++)
		{
			column[i] = original[i][columnId];
		}
		
		return column;
	}
	
	/*
	 * This function calculates the mean of a vector.
	 */
	public static double[] calculateMean(double[] xMasterOptimal, double[] xSlaveAverageOptimal, int evCount)
	{	
		double [] average = Utils.vectorAdd(xMasterOptimal, xSlaveAverageOptimal);
		
		for(int i =0; i< xMasterOptimal.length; i++)
			average[i] = round(average[i]/(double)evCount);
		
//		System.out.println("X-AVERAGE - Start");
//		Utils.PrintArray(average);
//		System.out.println("X-AVERAGE - End");
		return average;
	}
	
	/*
	 * This function calculates the mean of a vector.
	 */
	public static double calculateMean(double[] vector)
	{
		double mean = 0;
		
		for(int i =0; i< vector.length; i++)
			mean = mean + vector[i];
		
		return round(mean/vector.length);
		
	}
	
	/*
	 * This function rounds the double value to a specified decimal places.
	 */
	public static double round(double value) {
//		int places = Constants.ROUND_PLACES;
//	    if (places < 0) throw new IllegalArgumentException();
//
//	    BigDecimal bd = new BigDecimal(value);
//	    bd = bd.setScale(places, RoundingMode.HALF_UP);
//	    return bd.doubleValue();
		return value;
	}
	
	/*
	 * This function rounds the double value to a specified decimal value.
	 */
	public static double roundDouble(double num, int decimalPlaces)
	{
//		double multiplier = 10.0 * (decimalPlaces-1);
//		return Math.round (num * multiplier) / multiplier;
//		int places = Constants.ROUND_PLACES;
//	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(num);
	    bd = bd.setScale(decimalPlaces, RoundingMode.HALF_UP);
	    return bd.doubleValue();
		//return value;
	}
	
	/*
	 * This function sums all the EVs across columns and return it.
	 */
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
//TODO: CHECK THIS.
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
	
	public static double[] calculateVectorSum(double[] vec1, double[] vec2) {
		double[] sumMatrix = new double[vec1.length];
		
		for(int i=0; i< vec1.length; i++)
			sumMatrix[i] = vec1[i] + vec2[i];
		
		return sumMatrix;
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
	
	public static double[] calculateEVSum(double[][] evData)
	{
		int count = 0;
		double[] result = new double[evData.length];
		for(int i=0; i< evData.length ; i++)
		{
			double sum = 0;
			for(int j=0; j < evData[0].length - 1; j++) //Donot include master optimization
			{
				sum = sum + evData[i][j];
			}
			//result[count] = Utils.roundDouble(sum, 4);
			result[count] = sum;
			count++;
		}
		
		return result;
	}
	
	public static double calculateNorm(double[] vec)
	{
		RealVector v = MatrixUtils.createRealVector(vec);
		//System.out.println(">>>> Norm: " + v.getNorm() + " -- L1 Norm: " + v.getL1Norm() + " -- Inf Norm: " + v.getLInfNorm());
		//return v.getL1Norm();
		return v.getNorm();
		
//		double result = 0.0;
//		for(int i =0; i< vec.length;i++) {
//			result += (vec[i]*vec[i]);
//		}
//		
//		return Math.sqrt(result);
	}
	
	public static double calculateNorm(double[][] mat)
	{
		RealMatrix realMatrix = MatrixUtils.createRealMatrix(mat);
		SingularValueDecomposition svd = new SingularValueDecomposition(realMatrix);
		return svd.getNorm();
	}
	
	public static double calculateVariance(double[] data)
	{
		Variance v = new Variance();
		
		return v.evaluate(data);
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
			//System.out.print(input[i] + " ");
			System.out.print(String.format("%.13f",input[i]) + " ");
			
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
    
    public static double getMean(double[] data)
    {
        double sum = 0.0;
        for(double a : data)
            sum += a;
        return sum/data.length;
    }

    public static double getVariance(double[] data)
    {
        double mean = getMean(data);
        double temp = 0;
        for(double a :data)
            temp += (mean-a)*(mean-a);
        return temp/data.length;
    }
}
