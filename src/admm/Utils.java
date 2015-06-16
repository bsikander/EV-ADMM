package admm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.codehaus.jackson.map.ObjectMapper;

import com.jmatio.io.MatFileReader;
import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;

public class Utils {
	
	public static int ROUND_PLACES = 4;
	
	public static MasterData LoadMasterDataFromMatFile(String filePath)
	{
		try
		{
			MatFileWriter writer = new MatFileWriter();
			 
			MatFileReader matfilereader = new MatFileReader(filePath);
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
			System.out.println("Exception in LoadMasterData function in Utils");
			return null;
		}
	}
	
	public static SlaveData LoadSlaveDataFromMatFile(String filePath)
	{
		try
		{
			MatFileReader matfilereader = new MatFileReader(filePath);
			double[][] dArray = ((MLDouble)matfilereader.getMLArray("d")).getArray();
			double[][] AArray = ((MLDouble)matfilereader.getMLArray("A")).getArray();
			double[][] BArray = ((MLDouble)matfilereader.getMLArray("B")).getArray();
			double[][] RArray = ((MLDouble)matfilereader.getMLArray("R")).getArray();
			double[][] SmaxArray = ((MLDouble)matfilereader.getMLArray("S_max")).getArray(); //Conversion
			double[][] SminArray = ((MLDouble)matfilereader.getMLArray("S_min")).getArray(); //Conversion
			
			SlaveData context = new SlaveData(
											dArray[0], 
											AArray[0], 
											BArray, 
											RArray[0][0],
											getSingleArrayFromDouble(SmaxArray),
											getSingleArrayFromDouble(SminArray)
											);
			
			return context;
		}
		catch(Exception e)
		{
			System.out.println("Exception in LoadSlaveDataFromMatFile function in Utils" + e.getMessage());
			return null;
		}
	}
	
	public static void SlaveXToMatFile(String filePath)
	{
		
	}
	
	private static double[] getSingleArrayFromDouble(double[][] dArray)
	{
		double[] sArray = new double[dArray.length];
		int i = 0;
		for(double[] d: dArray)
		{
			sArray[i] = d[0];
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
			sumArr[i] = round( arr1[i] + arr2[i], ROUND_PLACES);
		}
		
		return sumArr;
	}
	
	public static double[][] setColumnInMatrix(double[][] original, double[] column, int columnId)
	{	
		for(int i=0; i < column.length; i++)
		{
			original[i][columnId] = round(column[i], 4);
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
	
	public static double[] calculateMean(double[][] matrix)
	{
		double[] average = new double[matrix.length];
		double rowSum = 0;
		for(int i =0; i< matrix.length; i++)
		{
			for(int j=0; j< matrix[0].length;j++)
			{
				rowSum += matrix[i][j];
			}
			average[i] = round(rowSum/(double)matrix[0].length , ROUND_PLACES);
			
			rowSum = 0;
		}
		
		return average;
	}
	
	public static double round(double value, int places) {
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
		
		return Math.pow(result, 0.5);
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
