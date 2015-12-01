package admm;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.hadoop.conf.Configuration;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloQuadNumExpr;
import ilog.cplex.IloCplex;

import gurobi.*;

public class MasterContextValley {
	private int N_EV;

	private double rho;
	private int T;
	private MasterDataValley masterData;
	private double[] xMean;
	private double[] x_optimal;
	
	private double[] u;
	
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
		
		//Xa_min and Xa_max
//		double[] m = Utils.getArrayWithData(getT(),1);
//		m = Utils.scalerMultiply(m, -100e3);
//
//		double[] m_max = Utils.getArrayWithData(getT(),1);
//		m_max = Utils.scalerMultiply(m_max, 60);
//		
//		this.xa_min = m;
//		this.xa_max = m_max;
		
		
	}
	
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
	
	public double optimize(double[] xold, int iteration) throws IloException, FileNotFoundException
	{	
		//K= xold - xmean - u ;
		//x= rho/(rho+2)* K + 2/(rho+2) * D;
		
		double rhotemp = rho/(rho+2);
		double[] k = subtractOldMeanUAnalytic(xold);
		
		double[] rhoMultiplyK = Utils.scalerMultiply(k, rhotemp);
		
		double[] DMatrix = masterData.getD();
		double[] rhoMultiplyD = Utils.scalerMultiply(DMatrix, (2/(rho+2)) );
		
		//this.setXOptimal( Utils.calculateVectorSubtraction(rhoMultiplyK, rhoMultiplyD ));
		this.setXOptimal( Utils.vectorAdd(rhoMultiplyK, rhoMultiplyD ));
		
		//cost= norm(D-x)^2;
		double cost = Utils.calculateNorm(Utils.vectorAdd(masterData.getD(), Utils.scalerMultiply(this.getXOptimal(), -1)));

		return cost*cost;
	}
	
	public double optimize1(double[] xold,int iteration) throws IloException, FileNotFoundException
	{	
		IloCplex cplex = new IloCplex();
		OutputStream out = new FileOutputStream("logfile_masterValley");
		cplex.setOut(out);
		
		//IloNumVar[] x_n = cplex.numVarArray(masterData.getPrice().length, -60, 100000);
		IloNumVar[] x_n = cplex.numVarArray(getT(), Double.MIN_VALUE, Double.MAX_VALUE);
		
		//double[] priceRealMatrix = masterData.getPrice();
		//priceRealMatrix = Utils.scalerMultiply(priceRealMatrix, -1);
		double[] DMatrix = masterData.getD();
		//DMatrix = Utils.scalerMultiply(DMatrix, -1);
		
		
		double[] data = subtractOldMeanU(xold);
		
		IloNumExpr[] exps = new IloNumExpr[data.length];
		
		for(int i =0; i< data.length; i++)
		{	
			//Original equation
			
			exps[i] = cplex.sum(cplex.square( cplex.sum(DMatrix[i], cplex.prod(x_n[i],-1)) ) ,
					cplex.prod(rho/2, cplex.square(cplex.sum(x_n[i], cplex.constant(data[i])))));
//			exps[i] = cplex.sum(cplex.square( cplex.sum(DMatrix[i], x_n[i]) ) ,
//								cplex.prod(rho/2, cplex.square(cplex.sum(x_n[i], cplex.constant(data[i])))));
		}
		
		IloNumExpr rightSide = cplex.sum(exps);
		cplex.addMinimize(rightSide);
		cplex.solve();		
		cplex.exportModel("Model_valley" + iteration +".lp");		
		
		x_optimal = new double[x_n.length];
		
		for(int u=0; u< x_n.length; u++)
		{
			x_optimal[u] = cplex.getValues(x_n)[u];
		}
		this.setXOptimal(x_optimal);
		
		//Utils.PrintArray(x_optimal);
		
		return cplex.getObjValue();
	}
	
	private double[] subtractOldMeanUAnalytic(double[] xold)
	{	
		//xold - xmean + u
		//xold = Utils.scalerMultiply(xold, -1);
		double[] temp = Utils.vectorAdd(xold, Utils.scalerMultiply(this.xMean, -1));
		//double[] output = Utils.vectorAdd(temp,this.u, -1);
		double[] output = Utils.vectorAdd(temp,Utils.scalerMultiply(this.u, -1));
		
		return output;
	}
	
	private double[] subtractOldMeanU(double[] xold)
	{	
		xold = Utils.scalerMultiply(xold, -1);
		double[] temp = Utils.vectorAdd(xold, this.xMean);
		double[] output = Utils.vectorAdd(temp, this.u);
		
		return output;
	}
	
	public void setXOptimal(double[] value)
	{
		this.x_optimal = value;
	}
	
	public void setXMean(double[] value)
	{
		this.xMean = value;
	}
	
	public double[] getxMean()
	{
		return this.xMean;
	}
	
	public double[] getXOptimal()
	{
		return this.x_optimal;
	}
	
	public MasterDataValley getMasterData()
	{
		return this.masterData;
	}
	
	public int getT()
	{
		this.T = (24*3600)/(15*60);
		return this.T;
	}
	
	public double[] getu()
	{
		return this.u;
	}
	
	public void setU(double[] u)
	{
		this.u = u;
	}
	
	public int getN()
	{
		return N_EV+1;
	}
	
//	public double getEps_pri()
//	{
//		this.eps_pri = Math.sqrt(getT()*getN());
//		return this.eps_pri;
//	}
//	
//	public double getEps_dual()
//	{
//		this.eps_pri = Math.sqrt(getT()*getN());
//		return this.eps_pri;
//	}
//	
//	public double[] getxa_min()
//	{	
//		return this.xa_min;
//	}
//	
//	public double[] getxa_max()
//	{
//		return this.xa_max;
//	}
	
	public void setRho(double value) {
		this.rho = value;
	}
}
