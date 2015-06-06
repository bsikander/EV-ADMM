package admm;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;

import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;

public class Utils {
	
	public static MasterData LoadMasterDataFromMatFile(String filePath)
	{
		try
		{
		
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
			sumArr[i] = arr1[i] + arr2[i];
		}
		
		return sumArr;
	}
	
	public static double[][] setColumnInMatrix(double[][] original, double[] column, int columnId)
	{
		for(int i=0; i < column.length; i++)
		{
			original[i][columnId] = column[i];
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
			average[i] = rowSum/(double)matrix[0].length;
			
			rowSum = 0;
		}
		
		return average;
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
