package admm;

import java.io.FileNotFoundException;

import org.apache.hadoop.conf.Configuration;

import ilog.concert.IloException;

/*
 * This class is responsible for loading the data and solving the master model.
 */
public class MasterContextValley {
	/*
	 *  Total EVs
	 */
	private int N_EV;
	
	/*
	 *  Current RHO value
	 */
	private double rho;
	
	/*
	 *  Time Interval
	 */
	private int T;
	
	/*
	 *  After loading the Aggregator data, everything is populated in this object. 
	 */
	private MasterDataValley masterData;
	
	/*
	 * Stores the current xMean value.
	 */
	private double[] xMean;
	
	/*
	 * Stores the current x* value of master
	 */
	private double[] x_optimal;
	
	/*
	 * Stores the current u
	 */
	private double[] u;
	
	/*
	 * This is the default constructor of this class and is responsible to load the Aggregator file specified by
	 * user through command line parameters and to scale the value of D based on the total EVs.
	 */
	public MasterContextValley(String inputPath, int evCount, double rhoValue, Configuration conf)
	{
		masterData = Utils.LoadMasterDataValleyFillingFromMatFile(inputPath, conf);
		N_EV = evCount;
		rho = rhoValue;
		
		x_optimal = Utils.getArrayWithData(getT(),0);
		u = Utils.getArrayWithData(getT(),0);
		
		xMean = Utils.getArrayWithData(getT(),0);
		
		//D= D*N/1e5;  % Need to scale demand
		masterData.setD( scaleD(masterData.getD(), N_EV + 1) );
	}
	
	/*
	 * This function solves the Aggregator model, set the optimal value in class variable and returns the cost.
	 */
	public double optimize(double[] xold, int iteration) throws IloException, FileNotFoundException
	{	
		//K= xold - xmean - u ;
		//x= rho/(rho+2)* K + 2/(rho+2) * D;
		
		double rhotemp = rho/(rho+2);
		double[] k = subtractOldMeanU(xold);
		
		double[] rhoMultiplyK = Utils.scalerMultiply(k, rhotemp);
		
		double[] DMatrix = masterData.getD();
		double[] rhoMultiplyD = Utils.scalerMultiply(DMatrix, (2/(rho+2)) );
		
		//this.setXOptimal( Utils.calculateVectorSubtraction(rhoMultiplyK, rhoMultiplyD ));
		this.setXOptimal( Utils.vectorAdd(rhoMultiplyK, rhoMultiplyD ));
		
//		/* TODO: REMOVE THIS*/
//		double sum3 = 0;
//		for(double d: this.getXOptimal())
//			sum3+=d;
//		
//		System.out.println("X MASTER SUM -> " + sum3);
		
		
		//cost= norm(D-x)^2;
		double cost = Utils.calculateNorm(Utils.vectorAdd(masterData.getD(), Utils.scalerMultiply(this.getXOptimal(), -1)));

		return cost*cost;
	}
	
	/*
	 * This function scales the Demand parameter of Aggregator based on the total EVs.
	 */
	private double[] scaleD(double[] D, double N)
	{
		double[] DNArray = Utils.scalerMultiply(D, N);
		double[] temp = new double[DNArray.length];
		
		int index = 0;
		for(double d : DNArray)
		{
			temp[index] = Utils.roundDouble( d/100000 , 4); // d/1e5
			index++;
		}
		
		return temp;
	}
	
	/*
	 * This function returns the value of xold - xMean - u.
	 */
	private double[] subtractOldMeanU(double[] xold)
	{	
		double[] temp = Utils.vectorAdd(xold, Utils.scalerMultiply(this.xMean, -1));
		double[] output = Utils.vectorAdd(temp,Utils.scalerMultiply(this.u, -1));
		
		return output;
	}
	
	/*
	 * Setter for xOptimal
	 */
	public void setXOptimal(double[] value)
	{
		this.x_optimal = value;
	}
	
	/*
	 * Setter for xMean value
	 */
	public void setXMean(double[] value)
	{
		this.xMean = value;
	}
	
	/*
	 * Getter for xMean.
	 */
	public double[] getxMean()
	{
		return this.xMean;
	}
	
	/*
	 * Getter for xOptimal
	 */
	public double[] getXOptimal()
	{
		return this.x_optimal;
	}
	
	/*
	 * Returns the masterData object which is created after loading the Aggregator mat file.
	 */
	public MasterDataValley getMasterData()
	{
		return this.masterData;
	}
	
	/*
	 * Gets total time interval
	 */
	public int getT()
	{
		this.T = (24*3600)/(15*60);
		return this.T;
	}
	
	/*
	 * Getter for u
	 */
	public double[] getu()
	{
		return this.u;
	}
	
	/*
	 * Setter for u
	 */
	public void setU(double[] u)
	{
		this.u = u;
	}
	
	/*
	 * Returns total EVs + 1
	 */
	public int getN()
	{
		return N_EV+1;
	}
	
	/*
	 * Setter for RHO value.
	 */
	public void setRho(double value) {
		this.rho = value;
	}
}
