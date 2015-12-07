package admm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hama.bsp.BSPPeer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.jmatio.io.MatFileReader;
import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;

import ilog.concert.*;
import ilog.cplex.*;
public class TestCPLEX {
public static void main(String[] args) throws IloException, IOException {

	writeSlaveFileWithoutFramework();
	System.out.println("END");;
//	int i = 0;
//	while(i < 10000000)
//	{
//		double[] tes1 = new double[100];
//		String test = "abc" + i;
//		float g = (float) (Math.random() * 100);
//		System.out.println(test + "00> " + (g));
//		i++;
//	}
//	System.out.println("testste");
//	long maxBytes = Runtime.getRuntime().maxMemory();
//	System.out.println("Max memory: " + maxBytes / 1024 / 1024 + "M");
	
	
	//	double[][] matrixData = { {1d,2d,3d}, {4d,5d,6d}};
//	RealMatrix a = MatrixUtils.createRealMatrix(matrixData);
//	System.out.println(a.getNorm());
//	System.out.println(a.getFrobeniusNorm());
//	SingularValueDecomposition b = new SingularValueDecomposition(a);
//	System.out.println(b.getNorm());
	
	//parseTxtFileToGenerateMatFiles();
	//writeMasterFile();
//	writeSlaveFile();
//	System.out.println("Done");

	//	writeDummyFile();
	
//	Map<String, double[]> data = new HashMap<String, double[]>();
//	data.put("test", new double[] {1,2,3});
//	data.put("test1", new double[] {4,5,6});
//	
//	final ObjectMapper OBJECT_MAPPER = new ObjectMapper();	
//	String value = OBJECT_MAPPER.writeValueAsString(data);
//	
//	System.out.println("String: > " + value);
//	
//	
//	 //Map<String, double[]> data1 = OBJECT_MAPPER.readValue(value, data.getClass());
//	Map<String, double[]> data1 = OBJECT_MAPPER.readValue(value, new TypeReference<Map<String, double[]>>(){});
//	 //ArrayList<Double> d = (java.util.ArrayList<Double>)data1.get("test1");
//	 Utils.PrintArray(data1.get("test1"));
//	 
//	 double[] dd = data1.get("test1");
//	 Utils.PrintArray(dd);
	 //System.out.println("Data :> " + data.get("test"));
}



private static void writeDummyFile() throws FileNotFoundException, UnsupportedEncodingException {
	String data = "";
	
	data = "12\n[]\n345";
	writeFile(data, "Dmmy.txt");
			}

private static void writeSlaveFile() throws IOException {
	SlaveData sdata;
	String data = "";
	//xi_max,xi_min,A,R_value,gamma,alpha,rho,smax,smin,B
	for(int i =1; i <= 10; i++) {
		 sdata = LoadSlaveDataFromMatFile("/Users/raja/Documents/ADMM_matlab/EVs/home/" + i + ".mat");
		 
		 String x_max = "";
		 String x_min = "";
		 int ccc = 0;
		 for(double d: sdata.getD())
		 {
			 x_max += d * 4 + ",";
			 x_min += d * -4 + ",";
			 //Utils.scalerMultiply(sdata.getD(), -4);
			 ccc++;
		 }
		 data += "96|[";
		 data += x_max.substring(0,x_max.length() - 1);
		 data += "]|[";
		 data += x_min.substring(0,x_min.length() - 1);
		 data += "]|[";
				 
		 
		 //data += "[4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0,4.0]";
		 //data += "|[-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-0.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0,-4.0]";
//		 for(double d: sdata.getD()) {
//			 data += String.valueOf(d) + ",";
//		 }
//		 data = data.substring(0,data.length() - 1);
//		 data += "]|[";
		 
		 //data += "|[";
		 
		 for(double d: sdata.getA()) {
			 data += String.valueOf(d) + ",";
		 }
		 data = data.substring(0,data.length() - 1);
		 data += "]|" + sdata.getR() + "|1|0.0125|0.01" + "|["; //]|R_value|gamma_value|alpha_value|rho|[
		 
		 
		 for(double d: sdata.getSmax()) {
			 data += String.valueOf(d) + ",";
		 }
		 data = data.substring(0,data.length() - 1);
		 data += "]|[";
		 
		 for(double d: sdata.getSmin()) {
			 data += String.valueOf(d) + ",";
		 }
		 data = data.substring(0,data.length() - 1);
		 data += "]|";
		 
		 for(double[] arr: sdata.getB()) {
			 data += "[";
			 for(double d : arr) {
				 data += String.valueOf(d) + ",";
			 }
			 data = data.substring(0,data.length() - 1);
			 data += "]";
		 }
		 
		 data +="\n";
		 
	}
	writeFile(data,"EVs.txt");
}

private static void writeSlaveFileWithoutFramework() throws IOException {
	SlaveData sdata;
	String data = "";
	//xi_max,xi_min,A,R_value,gamma,alpha,rho,smax,smin,B
	for(int i =1; i <= 3; i++) {
		 sdata = LoadSlaveDataFromMatFile("/Users/raja/Documents/Thesis/ADMM_matlab/Valley_Filling_1.1/Jose/EVs/home/" + i + ".mat");
		 
		 String D = "";
		 
		 for(double d: sdata.getD())
		 {
			 D += d + ",";
			//x_max += d * 4 + ",";
		 }
		 data += (i - 1);
		 data += "|[";
		 data += D.substring(0,D.length() - 1);
		 data += "]|[";
		 
		 for(double d: sdata.getA()) {
			 data += String.valueOf(d) + ",";
		 }
		 data = data.substring(0,data.length() - 1);
		 data += "]|" + sdata.getR() + "|[" ; //]|R_value|gamma_value|alpha_value|rho|[
		 
		 
		 for(double d: sdata.getSmax()) {
			 data += String.valueOf(d) + ",";
		 }
		 data = data.substring(0,data.length() - 1);
		 data += "]|[";
		 
		 for(double d: sdata.getSmin()) {
			 data += String.valueOf(d) + ",";
		 }
		 data = data.substring(0,data.length() - 1);
		 data += "]|";
		 
		 for(double[] arr: sdata.getB()) {
			 data += "[";
			 for(double d : arr) {
				 data += String.valueOf(d) + ",";
			 }
			 data = data.substring(0,data.length() - 1);
			 data += "]";
		 }
		 
		 data +="\n";
		 
	}
	writeFile(data,"EVs_new1.txt");
}


private static void writeMasterFile() throws FileNotFoundException, UnsupportedEncodingException
{
	MasterData mData = LoadMasterDataFromMatFile("/Users/raja/Documents/ADMM_matlab/Aggregator/aggregator.mat");
	//"timeSlot,price,rho"
	String data = "96|[";
	
	for(double d : mData.getPrice()) {
		data += String.valueOf(d) + ",";
	}
	data = data.substring(0,data.length() - 1);
	data += "]";
	//data += "]|[";
	
//	for(double d : mData.getRe()) {
//		data += String.valueOf(d) + ",";
//	}
//	
//	data = data.substring(0,data.length() - 1);
//	data += "]|[";
//	
//	for(double d : mData.getD()) {
//		data += String.valueOf(d) + ",";
//	}
//	
//	data = data.substring(0,data.length() - 1);
//	data += "]";

	
	//NOT INCLUDED IN UNCOMMENT
//	double[] m = Utils.getArrayWithData(96,1);
//	m = Utils.scalerMultiply(m, -100e3);
//	
//	for(double d: m) {
//		data += d + ",";
//	}
//	data = data.substring(0,data.length() - 1);
//	data += "]|[";
//	
//	double[] m_max = Utils.getArrayWithData(96,1);
//	m_max = Utils.scalerMultiply(m_max, 60);
//	
//	for(double d: m) {
//		data += d + ",";
//	}
//	data = data.substring(0,data.length() - 1);
//	data += "]";
	//NOT INCLUDED IN UNCOMMENT - END
//	data += "|[-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0,-100000.0]";
//	data += "|[60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0,60.0]";
	data += "|0.01";
	writeFile(data, "aggregator.txt");
	
	System.out.println("Finished");
}

private static void writeFile(String data, String fileName) throws FileNotFoundException, UnsupportedEncodingException {
	PrintWriter writer = new PrintWriter(fileName, "UTF-8");
	writer.println(data);
	
	writer.close();
}


public static MasterData LoadMasterDataFromMatFile(String filePath)
{
	try
	{	
		MatFileReader matfilereader = new MatFileReader(filePath);
		//MatFileWriter writer = new MatFileWriter();
		 
		//MatFileReader matfilereader = new MatFileReader(filePath);
		double[][] reArray = ((MLDouble)matfilereader.getMLArray("re")).getArray(); //Conversion
		double[][] DArray = ((MLDouble)matfilereader.getMLArray("D")).getArray(); //Conversion
		double[][] priceArray = ((MLDouble)matfilereader.getMLArray("price")).getArray();
		
		MasterData context = new MasterData(
											Utils.getSingleArrayFromDouble(reArray),
											Utils.getSingleArrayFromDouble( DArray ), 
											priceArray[0]
											);
		
		return context;
	}
	catch(Exception e)
	{
		System.out.println("Exception in LoadMasterData function in Utils" + e.getMessage() + " == filePath: " + filePath);
		return null;
	}
}


public static SlaveData LoadSlaveDataFromMatFile(String filePath) throws IOException
{
	try
	{		
		MatFileReader matfilereader = new MatFileReader(filePath);
		
		//MatFileReader matfilereader = new MatFileReader(filePath);
		
		double[][] dArray = ((MLDouble)matfilereader.getMLArray("d")).getArray();
		double[][] AArray = ((MLDouble)matfilereader.getMLArray("A")).getArray();
		double[][] BArray = ((MLDouble)matfilereader.getMLArray("B")).getArray();
		double[][] RArray = ((MLDouble)matfilereader.getMLArray("R")).getArray();
		double[][] SmaxArray = ((MLDouble)matfilereader.getMLArray("S_max")).getArray(); //Conversion
		double[][] SminArray = ((MLDouble)matfilereader.getMLArray("S_min")).getArray(); //Conversion
		
//		double[][] x_optimal = new double[dArray[0].length][1];
//		if(matfilereader.getMLArray("x_optimal") == null || isFirstIteration == true) {
//			for(int i=0; i< dArray[0].length;i++) {
//				x_optimal[i][0] = 0;
//			}
//		}
//		else {
//			x_optimal = ((MLDouble)matfilereader.getMLArray("x_optimal")).getArray(); //Conversion
//			
//			peer.write(new IntWritable(1), new Text("X_OPTIMAL FOUND"));
//			Utils.PrintArray(Utils.getSingleArrayFromDouble(x_optimal));
//		}
//		
//		peer.write(new IntWritable(1), new Text(Utils.convertDoubleArrayToString(Utils.getSingleArrayFromDouble(x_optimal))));
		
		SlaveData context = new SlaveData(
										dArray[0], 
										AArray[0], 
										BArray, 
										RArray[0][0],
										Utils.getSingleArrayFromDouble(SmaxArray),
										Utils.getSingleArrayFromDouble(SminArray)
										//Utils.getSingleArrayFromDouble(x_optimal)
										);
		
		return context;
	}
	catch(Exception e)
	{
		System.out.println("Exception in LoadSlaveDataFromMatFile function in Utils" + e.getMessage());
		
		return null;
	}
}

public static void parseTxtFileToGenerateMatFiles() throws IOException {
	BufferedReader br = new BufferedReader(new FileReader("/Users/raja/Documents/workspace/ADFHama/data/EVs.txt"));
	 
	String line = null;
	int count = 1;
	while ((line = br.readLine()) != null) {
		WriteMatFile(line, count);
		count++;
	}
 
	br.close();
	
}

	public static void WriteMatFile(String input, int id) throws IOException
	{
		MatFileWriter matfileWriter = new MatFileWriter();
		//MatFileReader matfileReader = new MatFileReader(filePath);
		List<MLArray> list = parse(input);
		
		
		matfileWriter.write("/Users/raja/Documents/workspace/ADFHama/data/mat_files/" + id + ".mat", list); //Write to temp file
		
	}
	
	private static List<MLArray> parse(String input) {
		String[] splitData = input.split("\\|");
		
		List<MLArray> list = new ArrayList<MLArray>();
		
		list.add(getJMatArray( getArray(splitData[0]), "x_max1"));
		list.add( getJMatArray( getArray(splitData[1]), "x_min1"));
		list.add( getJMatArray(getArray(splitData[2]), "A1"));
		list.add( getJMatArray( Double.parseDouble(splitData[3]), "R1"));
		list.add( getJMatArray(Double.parseDouble(splitData[4]), "gamma1"));
		list.add( getJMatArray(Double.parseDouble(splitData[5]), "alpha1"));
		list.add( getJMatArray( Double.parseDouble(splitData[6]),"rho1"));
		list.add( getJMatArray(getArray(splitData[7]),"S_max1"));
		list.add( getJMatArray(getArray(splitData[8]), "S_min1"));
		list.add(new MLDouble("B1", getDoubleArray(splitData[9])));
		
		return list;
	}
	
	private static MLArray getJMatArray(double x, String name) {
		double[][] xDoubleArray = new double[1][1];
		xDoubleArray[0][0] = x;	
		
		MLArray xMLArray = new MLDouble(name,xDoubleArray);
		return xMLArray;
	}
	
	private static MLArray getJMatArray(double[] x, String name) {
		double[][] xDoubleArray = new double[x.length][1];
		for(int i =0; i< x.length; i++) {
			xDoubleArray[i][0] = x[i];
		}	
		
		MLArray xMLArray = new MLDouble(name,xDoubleArray);
		return xMLArray;
	}
	
	private static double[] getArray(String input) {
		double[] arr;
		//System.out.println("INPUT> :" + input);
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
	
	private static double[][] getDoubleArray(String input) {
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
	

}
////	double[] x1 = new double[30];
////	for(int i =0; i< 30;i++)
////	{
////		x1[i] = i;
////	}
////	Utils.SlaveXToMatFile("/Users/raja/Documents/workspace/Hama-EVADMM/1.mat",x1);
//	
////	SlaveData daaaata = Utils.LoadSlaveDataFromMatFile("/Users/raja/Documents/workspace/Hama-EVADMM/2.mat",true);
////	Utils.PrintArray(daaaata.getXOptimal());
////	System.out.println("Total Size: " + daaaata.getXOptimal().length);
//	
//	//SlaveData test = Utils.LoadSlaveDataFromMatFile("/Users/raja/Documents/workspace/Hama-EVADMM/test.mat");
//	
//	
//	//Data of master iteration 1. This will generate the output for iteration 2
//	//String u = "0.6595032375558516 0.6595032376333922 0.6595032377141438 0.6595032377983914 0.6355532378864608 0.635553237978727 0.6355532380756254 0.6355532381776642 0.6283532382854438 0.6283532383996792 0.6283532385212336 0.6283532386511622 0.6231532387907769 0.623153238941741 0.6231532391062163 0.6231532392871003 0.6133032394884346 0.6133032397161606 0.6133032399796325 0.6133032402949418 0.6056032406929428 0.6056032412415334 0.6056032421176089 0.6056032438310096 0.4220956216568828 0.42209562518550625 0.35534879940199887 0.35534880165267796 0.37464880618793595 0.3066884082460504 0.2352 0.2352 0.25579999999999997 0.25579999999999997 0.25579999999999997 0.25579999999999997 0.2854 0.2854 0.2854 0.2854 0.29050000000000004 0.29050000000000004 0.29050000000000004 0.29050000000000004 0.2982 0.2982 0.2982 0.2982 0.30015 0.30015 0.30015 0.30015 0.26555 0.26555 0.26555 0.26555 0.25464999999999993 0.25464999999999993 0.25464999999999993 0.25464999999999993 0.25444999999999995 0.25444999999999995 0.25444999999999995 0.25444999999999995 0.27535 0.34683839702827857 0.41358521671187615 0.41358521671187615 0.4934852167118762 0.4934852167118762 0.5614456149910436 0.5614456150081911 0.5804956150256666 0.5804956150434843 0.58049561506166 0.5804956150887685 0.5485456151164392 0.5485456151446936 0.5485456151735594 0.5485456152030656 0.48699561523324186 0.4869956152641194 0.4869956152957308 0.4869956153281111 0.45964561536129767 0.6495032370310883 0.6495032370660129 0.649503237101877 0.6605032371387334 0.6605032371766397 0.6605032372156594 0.6605032372558617 0.652903237297323 0.652903237340127 0.6529032374094289 0.6529032374812711";
//	//String x = "1.31725	1.31725	1.31725	1.31725	1.1975	1.1975	1.1975	1.1975	1.1615	1.1615	1.1615	1.1615	1.1355	1.1355	1.1355	1.1355	1.08625	1.08625	1.08625	1.08625	1.04775	1.04775	1.04775	1.04775	1.0795	1.0795	1.0795	1.0795	1.176	1.176	1.176	1.176	1.279	1.279	1.279	1.279	1.427	1.427	1.427	1.427	1.4525000000000001	1.4525000000000001	1.4525000000000001	1.4525000000000001	1.491	1.491	1.491	1.491	1.5007499999999998	1.5007499999999998	1.5007499999999998	1.5007499999999998	1.32775	1.32775	1.32775	1.32775	1.2732499999999998	1.2732499999999998	1.2732499999999998	1.2732499999999998	1.2722499999999999	1.2722499999999999	1.2722499999999999	1.2722499999999999	1.37675	1.37675	1.37675	1.37675	1.77625	1.77625	1.77625	1.77625	1.8715	1.8715	1.8715	1.8715	1.71175	1.71175	1.71175	1.71175	1.404	1.404	1.404	1.404	1.26725	1.26725	1.26725	1.26725	1.32225	1.32225	1.32225	1.32225	1.28425	1.28425	1.28425	1.28425";
//	//String xMean="0.6595032375558516 0.6595032376333922 0.6595032377141438 0.6595032377983914 0.6355532378864608 0.635553237978727 0.6355532380756254 0.6355532381776642 0.6283532382854438 0.6283532383996792 0.6283532385212336 0.6283532386511622 0.6231532387907769 0.623153238941741 0.6231532391062163 0.6231532392871003 0.6133032394884346 0.6133032397161606 0.6133032399796325 0.6133032402949418 0.6056032406929428 0.6056032412415334 0.6056032421176089 0.6056032438310096 0.4220956216568828 0.42209562518550625 0.35534879940199887 0.35534880165267796 0.37464880618793595 0.3066884082460504 0.2352 0.2352 0.25579999999999997 0.25579999999999997 0.25579999999999997 0.25579999999999997 0.2854 0.2854 0.2854 0.2854 0.29050000000000004 0.29050000000000004 0.29050000000000004 0.29050000000000004 0.2982 0.2982 0.2982 0.2982 0.30015 0.30015 0.30015 0.30015 0.26555 0.26555 0.26555 0.26555 0.25464999999999993 0.25464999999999993 0.25464999999999993 0.25464999999999993 0.25444999999999995 0.25444999999999995 0.25444999999999995 0.25444999999999995 0.27535 0.34683839702827857 0.41358521671187615 0.41358521671187615 0.4934852167118762 0.4934852167118762 0.5614456149910436 0.5614456150081911 0.5804956150256666 0.5804956150434843 0.58049561506166 0.5804956150887685 0.5485456151164392 0.5485456151446936 0.5485456151735594 0.5485456152030656 0.48699561523324186 0.4869956152641194 0.4869956152957308 0.4869956153281111 0.45964561536129767 0.6495032370310883 0.6495032370660129 0.649503237101877 0.6605032371387334 0.6605032371766397 0.6605032372156594 0.6605032372558617 0.652903237297323 0.652903237340127 0.6529032374094289 0.6529032374812711";
//	
//	//PROBLEM HERE
//	//%Data of master iteration 3. This will generate the output for iteration 4
//	String xMean="0.5066893470996584 0.5066893470971756 0.5066893470944465 0.5066893470914337 0.481273020471175 0.4812730204674837 0.481273020463483 0.4812730204591208 0.4736322041045641 0.4736322040993001 0.47363220409349865 0.47363220408704887 0.46811383671671225 0.4681138367085282 0.46811383669919043 0.4681138366883645 0.4576607754217622 0.4576607754063845 0.4576607753873839 0.4576607753626972 0.44948934673397006 0.44948934667878354 0.4494893465757239 0.4494893463566537 0.4644229513323377 0.46442295034984865 0.4514909933830909 0.4514909927766736 0.4777074815124355 0.45503328854433783 0.4139520000000001 0.4139520000000001 0.45020800000000005 0.45020800000000005 0.45020800000000005 0.45020800000000005 0.5023039999999999 0.5023039999999999 0.5023039999999999 0.5023039999999999 0.5112800000000001 0.5112800000000001 0.5112800000000001 0.5112800000000001 0.5248320000000002 0.5248320000000002 0.5248320000000002 0.5248320000000002 0.528264 0.528264 0.528264 0.528264 0.4673679999999999 0.4673679999999999 0.4673679999999999 0.4673679999999999 0.4481839999999999 0.4481839999999999 0.4481839999999999 0.4481839999999999 0.447832 0.447832 0.447832 0.447832 0.48461599999999994 0.5171101058454293 0.533096587109883 0.533096587109883 0.6416301382991636 0.6416301382991636 0.6311879323321382 0.631187932334643 0.6539857283400423 0.6539857283424935 0.6539857283449001 0.6539857283454997 0.6157500547424789 0.6157500547429304 0.6157500547433326 0.615750054743677 0.5420910341559331 0.5420910341562097 0.5420910341565192 0.5420910341568644 0.5093603402210682 0.49607710217722467 0.49607710217661605 0.4960771021759509 0.5077505716047926 0.5077505716039504 0.5077505716030253 0.5077505716020113 0.4996852654492784 0.4996852654480889 0.499685265446138 0.499685265444033";
//	String u="1.795859098876118 1.795859098970463 1.7958590990684564 1.795859099170403 1.7191213440296071 1.719121344140671 1.71912134425712 1.7191213443795217 1.696051956713387 1.6960519568498649 1.696051956994799 1.6960519571493533 1.6793907327970579 1.6793907329756121 1.6793907331695193 1.6793907333819287 1.6478305294839575 1.6478305297487643 1.6478305300534357 1.6478305304153813 1.6231591022557672 1.6231591028722896 1.6231591038531372 1.6231591057626238 1.4102603927036785 1.4102603950347914 1.286745701462116 1.2867457030514178 1.3587307677505704 1.2109111507749728 1.0254720000000002 1.0254720000000002 1.115288 1.115288 1.115288 1.115288 1.244344 1.244344 1.244344 1.244344 1.2665800000000003 1.2665800000000003 1.2665800000000003 1.2665800000000003 1.3001520000000002 1.3001520000000002 1.3001520000000002 1.3001520000000002 1.308654 1.308654 1.308654 1.308654 1.157798 1.157798 1.157798 1.157798 1.1102739999999998 1.1102739999999998 1.1102739999999998 1.1102739999999998 1.109402 1.109402 1.109402 1.109402 1.200526 1.3727893830154385 1.507959419920288 1.507959419920288 1.8059701140349236 1.8059701140349236 1.8915582236631034 1.8915582236892723 1.9573545911770804 1.9573545912041446 1.9573545912316663 1.9573545912669401 1.8470032033646882 1.847003203401333 1.8470032034387402 1.8470032034769441 1.6344170399883657 1.6344170400284683 1.6344170400696565 1.6344170401119915 1.5399534890443207 1.763818281863819 1.7638182819048551 1.7638182819468926 1.7990631800187922 1.7990631800629164 1.7990631801081867 1.7990631801546737 1.7747121597458562 1.7747121597950715 1.7747121598799356 1.7747121599677405";
//	String x="0.7139072588912293	0.7139072586200328	0.7139072583378366	0.7139072580436695	0.481250114996675	0.4812501146748985	0.4812501143371005	0.481250113981534	0.4113072564969112	0.4113072560991915	0.411307255676166	0.41130725522422895	0.36079296904853175	0.36079296852409115	0.3607929679531262	0.36079296732577165	0.26510725238717525	0.26510725159908	0.2651072506882636	0.26510724959968973	0.19030724826346254	0.1903072463714543	0.1903072433275645	0.1903072373570503	0.9247294956004355	0.924729485444608	1.2126417844399506	1.2126417777978336	1.391304621335794	1.7095558672926798	2.0697600000000005	2.0697600000000005	2.25104	2.25104	2.25104	2.25104	2.5115199999999995	2.5115199999999995	2.5115199999999995	2.5115199999999995	2.5564000000000004	2.5564000000000004	2.5564000000000004	2.5564000000000004	2.6241600000000007	2.6241600000000007	2.6241600000000007	2.6241600000000007	2.64132	2.64132	2.64132	2.64132	2.3368399999999996	2.3368399999999996	2.3368399999999996	2.3368399999999996	2.2409199999999996	2.2409199999999996	2.2409199999999996	2.2409199999999996	2.23916	2.23916	2.23916	2.23916	2.4230799999999997	2.0720530486317026	1.7669391176673137	1.7669391176673137	2.506584831816604	2.506584831816604	2.2465638023470262	2.2465638022825507	2.427266659300257	2.427266659233213	2.427266659164807	2.4272666590683505	2.1241980876391433	2.1241980875385016	2.124198087435626	2.1241980873304005	1.5403523731018924	1.540352372991363	1.5403523728779946	1.5403523727616348	1.280918086992198	0.616764403595723	0.6167644034775093	0.6167644033562396	0.723621546033268	0.7236215459054279	0.7236215457740179	0.7236215456388131	0.6497929741095216	0.6497929739659075	0.6497929737229762	0.6497929734713137";
//	
//	//%Data of master iteration 2. This will generate the output for iteration 3
//	//String xMean ="0.629666514220608 0.6296665142398952 0.6296665142598663 0.6296665142805782 0.6022950856719713 0.6022950856944602 0.6022950857180117 0.6022950857427365 0.5940665143233789 0.5940665143508855 0.5940665143800666 0.5940665144111422 0.5881236572895687 0.5881236573253429 0.5881236573641124 0.5881236574064639 0.5768665145737606 0.5768665146262191 0.5768665146864194 0.5768665147577424 0.5680665148288544 0.5680665149519727 0.5680665151598044 0.5680665155749604 0.523741819714458 0.5237418194994365 0.47990590867702626 0.47990590862206606 0.506374480050199 0.4491894539845845 0.37632 0.37632 0.40928 0.40928 0.40928 0.40928 0.45664 0.45664 0.45664 0.45664 0.46480000000000005 0.46480000000000005 0.46480000000000005 0.46480000000000005 0.47712000000000004 0.47712000000000004 0.47712000000000004 0.47712000000000004 0.48024000000000006 0.48024000000000006 0.48024000000000006 0.48024000000000006 0.42488000000000004 0.42488000000000004 0.42488000000000004 0.42488000000000004 0.40743999999999997 0.40743999999999997 0.40743999999999997 0.40743999999999997 0.40712000000000004 0.40712000000000004 0.40712000000000004 0.40712000000000004 0.44055999999999995 0.5088408801417307 0.5612776160985289 0.5612776160985289 0.6708547590238838 0.6708547590238838 0.6989246763399217 0.6989246763464381 0.7228732478113715 0.7228732478181668 0.7228732478251061 0.7228732478326718 0.6827075335057702 0.682707533513709 0.6827075335218483 0.6827075335302014 0.6053303905991909 0.6053303906081393 0.6053303906174066 0.6053303906270161 0.5709475334619547 0.618237942655506 0.618237942662226 0.6182379426690648 0.630809371275266 0.6308093712823262 0.6308093712895021 0.6308093712968008 0.6221236569992545 0.6221236570068556 0.6221236570243686 0.6221236570424364";
//	//String u="1.2891697517764595 1.2891697518732874 1.28916975197401 1.2891697520789696 1.237848323558432 1.2378483236731872 1.237848323793637 1.2378483239204008 1.2224197526088227 1.2224197527505647 1.2224197529013003 1.2224197530623044 1.2112768960803457 1.211276896267084 1.2112768964703289 1.211276896693564 1.1901697540621952 1.1901697543423797 1.190169754666052 1.1901697550526842 1.1736697555217972 1.173669756193506 1.1736697572774133 1.17366975940597 0.9458374413713408 0.9458374446849428 0.8352547080790251 0.8352547102747441 0.881023286238135 0.7558778622306349 0.61152 0.61152 0.6650799999999999 0.6650799999999999 0.6650799999999999 0.6650799999999999 0.74204 0.74204 0.74204 0.74204 0.7553000000000001 0.7553000000000001 0.7553000000000001 0.7553000000000001 0.77532 0.77532 0.77532 0.77532 0.78039 0.78039 0.78039 0.78039 0.6904300000000001 0.6904300000000001 0.6904300000000001 0.6904300000000001 0.6620899999999998 0.6620899999999998 0.6620899999999998 0.6620899999999998 0.66157 0.66157 0.66157 0.66157 0.7159099999999999 0.8556792771700092 0.974862832810405 0.974862832810405 1.16433997573576 1.16433997573576 1.2603702913309653 1.2603702913546293 1.3033688628370381 1.303368862861651 1.3033688628867661 1.3033688629214404 1.2312531486222094 1.2312531486584026 1.2312531486954077 1.231253148733267 1.0923260058324327 1.0923260058722586 1.0923260059131374 1.0923260059551272 1.0305931488232525 1.2677411796865943 1.267741179728239 1.2677411797709417 1.2913126084139994 1.291312608458966 1.2913126085051614 1.2913126085526625 1.2750268942965777 1.2750268943469827 1.2750268944337975 1.2750268945237075";
//	//String x="1.3154935248882969	1.3154935247332153	1.3154935245717128	1.3154935244032173	1.1238935242270784	1.1238935240425458	1.1238935238487493	1.1238935236446714	1.0662935234291129	1.0662935232006416	1.066293522957533	1.0662935226976755	1.0246935224184461	1.0246935221165179	1.0246935217875675	1.0246935214257995	0.945893521023131	0.9458935205676786	0.9458935200407348	0.9458935194101163	0.8842935186141142	0.8842935175169332	0.8842935157647823	0.8842935123379808	1.3148087566862345	1.3148087496289873	1.448302401196002	1.4483023966946438	1.6027023876241282	1.7386231835078991	1.8816	1.8816	2.0463999999999998	2.0463999999999998	2.0463999999999998	2.0463999999999998	2.2832	2.2832	2.2832	2.2832	2.3240000000000003	2.3240000000000003	2.3240000000000003	2.3240000000000003	2.3856	2.3856	2.3856	2.3856	2.4012000000000002	2.4012000000000002	2.4012000000000002	2.4012000000000002	2.1244	2.1244	2.1244	2.1244	2.0372	2.0372	2.0372	2.0372	2.0356	2.0356	2.0356	2.0356	2.2028	2.0598232059434425	1.9263295665762477	1.9263295665762477	2.5655295665762483	2.5655295665762483	2.4296087700179134	2.429608769983618	2.5820087699486667	2.582008769913031	2.5820087698766794	2.5820087698224627	2.3264087697671223	2.3264087697106133	2.3264087696528817	2.326408769593869	1.834008769533516	1.834008769471761	1.8340087694085383	1.834008769343778	1.6152087692774049	1.2354935259378235	1.2354935258679742	1.235493525796246	1.3234935257225335	1.3234935256467202	1.3234935255686813	1.3234935254882765	1.2626935254053537	1.2626935253197458	1.2626935251811422	1.2626935250374578";
//
//	MasterContext context = new MasterContext("/Users/raja/Documents/Thesis/ADMM_matlab/Aggregator/aggregator.mat",4,0.01);
//	context.setU(split(u));
//	//context.setX(split(x));
//	context.setXMean(split(xMean));
//	
//	 double cost = context.optimize(split(x), 0);
//	 System.out.println(cost);
//	 
//	 //System.out.println("Output Using Yalmip CPLEX model");
//	 //optimize();
//	 
//	
//	
////	double[][] matrix = new double[5][6];
////	int m = 1;
////	for(int i=0; i< 5; i++)
////		for(int j =0; j< 6; j++)
////			matrix[i][j] = m++;
////	
////	double[] result = Utils.calculateMean(matrix);
////	
////	Utils.PrintArray(result);
//	
////	double[] v1 = new double[5];
////	double[] v2 = new double[5];
////	for(int i=1;i<6; i++)
////	{
////		v1[i-1] = i;
////		v2[i-1] = i+5;
////	}
////	
////	
////	Utils.PrintArray(Utils.vectorAdd(v1, v2));
//	
//}
//
//private static void optimize() throws IloException {
//	IloCplex cplex = new IloCplex();
//	
//	IloNumVar[] x_n = cplex.numVarArray(96, Double.MIN_VALUE, Double.MAX_VALUE);
//
//	cplex.importModel("TestModel0.lp");
//	//cplex.importModel("mymodelCplex.mod");
//	cplex.solve();
////	cplex.output();
////	cplex.getStatus();
//	System.out.println( cplex.getObjValue());
//	
//	Iterator matrixEnum = cplex.LPMatrixIterator();
//    IloLPMatrix lp = (IloLPMatrix)matrixEnum.next();
//    
//    double[] x = cplex.getValues(lp);
//    double sum = 0;
//    for (int j = 0; j < x.length; ++j) {
//    	System.out.print(x[j] + "\t");
//    	sum += x[j];
//    }
//    System.out.println();
//    System.out.println(sum);
//    
//	//cplex.getValues(x_n);
////	double[] x_optimal = new double[96];
////	
////	System.out.println("======= MASTER: OPTIMZATION ARRAY =====");
////	for(int u=0; u< 96; u++)
////	{
////		
////		x_optimal[u] = cplex.getValues(x_n)[u];
////		System.out.print(Utils.round(cplex.getValues(x_n)[u],4) + "\t");
////	}
////	
////	System.out.println("=====");
//}
//
//
//private static double[] split(String input)
//{
//	String[] splittedInput = input.split("\\s+");
//	double[] output = new double[96];
//	int i = 0;
//	
//	for(String s: splittedInput) {
//		output[i] = Double.parseDouble(s);
//		i++;
//	}
//	return output;
//}
//
//// try {
////		 IloCplex cplex = new IloCplex();
////		 double[] lb = {0.0, 0.0, 0.0};
////		 double[] ub = {40.0, Double.MAX_VALUE, Double.MAX_VALUE};
////		 IloNumVar[] x = cplex.numVarArray(3, lb, ub);
////		 double[] objvals = {1.0, 2.0, 3.0};
////		
////		 cplex.addMaximize(cplex.scalProd(x, objvals)); 
////		 
////		 cplex.addLe(cplex.sum(cplex.prod(-1.0, x[0]),
////		 cplex.prod( 1.0, x[1]),
////		 cplex.prod( 1.0, x[2])), 20.0);
////		 cplex.addLe(cplex.sum(cplex.prod( 1.0, x[0]),
////		 cplex.prod(-3.0, x[1]),
////		 cplex.prod( 1.0, x[2])), 30.0);
////		 cplex.exportModel("model2.lp");
////		 if ( cplex.solve() ) {
////			 cplex.output().println("Solution status = " + cplex.getStatus());
////			 cplex.output().println("Solution value = " + cplex.getObjValue());
////		 
////		 double[] val = cplex.getValues(x);
////		 int ncols = cplex.getNcols();
////		 
////		 for (int j = 0; j < ncols; ++j)
////			 cplex.output().println("Column: " + j + " Value = " + val[j]);
////		 }
////		 cplex.end();
////	}
////	catch (IloException e) {
////		System.err.println("Concert exception '" + e + "' caught");
////	}
////	}
//} 
//
