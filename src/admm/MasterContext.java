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


//import org.apache.commons.math3.linear.MatrixUtils;
//import org.apache.commons.math3.linear.RealMatrix;

import gurobi.*;

public class MasterContext {
	private int N_EV;
	//private String chargeStrategy = "home";
	private double rho;
	private int T;
	private double eps_pri;
	//private double eps_dual;
	private double[] xa_min;
	private double[] xa_max;
	private MasterData masterData;
	private double[] xMean;
	private double[] x_optimal;
	
	private double[] u;
	
	public MasterContext(String inputPath, int evCount, double rhoValue, Configuration conf)
	{
		masterData = Utils.LoadMasterDataFromMatFile(inputPath, conf);
		N_EV = evCount;
		rho = rhoValue;
		
		x_optimal = Utils.getArrayWithData(getT(),0);
		u = Utils.getArrayWithData(getT(),0);
		
		//Xa_min and Xa_max
		double[] m = Utils.getArrayWithData(getT(),1);
		m = Utils.scalerMultiply(m, -100e3);

		double[] m_max = Utils.getArrayWithData(getT(),1);
		m_max = Utils.scalerMultiply(m_max, 60);
		
		this.xa_min = m;
		this.xa_max = m_max;
		
		xMean = Utils.getArrayWithData(getT(),0);
	}
	
	public double optimize(double[] xold,int iteration) throws IloException, FileNotFoundException
	{	
		IloCplex cplex = new IloCplex();
		//OutputStream out = new FileOutputStream("logfile_master");
		//cplex.setOut(out);
		
		IloNumVar[] x_n = cplex.numVarArray(masterData.getPrice().length, -60, 100000);
		
		double[] priceRealMatrix = masterData.getPrice();
		priceRealMatrix = Utils.scalerMultiply(priceRealMatrix, -1);
		
		double[] data = subtractOldMeanU(xold);
		
		IloNumExpr[] exps = new IloNumExpr[data.length];
		
		for(int i =0; i< data.length; i++)
		{	
			//Original equation
			exps[i] = cplex.sum(cplex.prod(priceRealMatrix[i], x_n[i]) ,cplex.prod(rho/2, cplex.square(cplex.sum(x_n[i], cplex.constant(data[i])))));
		}
		
		IloNumExpr rightSide = cplex.sum(exps);
		cplex.addMinimize(rightSide);
		cplex.solve();		
		//cplex.exportModel("TestModel_beh" + iteration +".lp");		
		
		x_optimal = new double[x_n.length];
		
		for(int u=0; u< x_n.length; u++)
		{
			x_optimal[u] = cplex.getValues(x_n)[u];
		}
		this.setXOptimal(x_optimal);
		
		double result = cplex.getObjValue();
		cplex = null;
		x_n = null;
		exps = null;
		
		return result;
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
	
	public double getEps_pri()
	{
		this.eps_pri = Math.sqrt(getT()*getN());
		return this.eps_pri;
	}
	
	public double getEps_dual()
	{
		this.eps_pri = Math.sqrt(getT()*getN());
		return this.eps_pri;
	}
	
	public double[] getxa_min()
	{	
		return this.xa_min;
	}
	
	public double[] getxa_max()
	{
		return this.xa_max;
	}
	
	public void setRho(double value) {
		this.rho = value;
	}
}
