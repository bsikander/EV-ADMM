package admm;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

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
	private static final int N_EV = 100;
	private String chargeStrategy = "home";
	private static final double rho = 0.01;
	private int T;
	private double eps_pri;
	private double eps_dual;
	private double[] xa_min;
	private double[] xa_max;
	private MasterData masterData;
	private double[] xMean;
	private double[] x_optimal;
	
	private double[][] x_master;
	private double[] u;
	
	public MasterContext()
	{
		masterData = Utils.LoadMasterDataFromMatFile("/Users/raja/Documents/Thesis/ADMM_matlab/Aggregator/aggregator.mat");
		
		//x_master = MatrixUtils.createRealMatrix(Utils.getZeroDoubleArray(getT(),getN()));
		x_master = Utils.getZeroDoubleArray(getT(),getN());
		//u = MatrixUtils.createColumnRealMatrix(Utils.getArrayWithData(getT(),0));
		u = Utils.getArrayWithData(getT(),0);
		
		
		//Xa_min and Xa_max
		//RealMatrix m = MatrixUtils.createColumnRealMatrix(Utils.getArrayWithData(getT(),1));
		//m = m.scalarMultiply(-100e3);
		double[] m = Utils.getArrayWithData(getT(),1);
		m = Utils.scalerMultiply(m, -100e3);
		
//		RealMatrix m_max = MatrixUtils.createColumnRealMatrix(Utils.getArrayWithData(getT(),1));
//		m_max = m_max.scalarMultiply(60);
		double[] m_max = Utils.getArrayWithData(getT(),1);
		m_max = Utils.scalerMultiply(m_max, 60);
		
//		this.xa_min = m.getColumn(0);
//		this.xa_max = m_max.getColumn(0);
		this.xa_min = m;
		this.xa_max = m_max;
		
//		for(int i =0; i< xa_max.length;i++)
//		{
//			System.out.println(xa_max[i]);
//		}
		
		//xMean = MatrixUtils.createColumnRealMatrix(Utils.getArrayWithData(getT(),0));
		xMean = Utils.getArrayWithData(getT(),0);
	}
	
	public double optimize(double[] xold) throws IloException, FileNotFoundException
	{		
		
		IloCplex cplex = new IloCplex();
		OutputStream out = new FileOutputStream("logfile");
		cplex.setOut(out);
		//cplex.setOut(env.getNullStream());
		IloNumVar[] x_n = cplex.numVarArray(masterData.getPrice().length, Double.MIN_VALUE, Double.MAX_VALUE);
		//IloLinearNumExpr objective = cplex.linearNumExpr();
		
		
//	    RealMatrix priceRealMatrix = MatrixUtils.createColumnRealMatrix(masterData.getPrice());
//	    priceRealMatrix = priceRealMatrix.scalarMultiply(-15);
		double[] priceRealMatrix = masterData.getPrice();
		//priceRealMatrix = Utils.scalerMultiply(priceRealMatrix, -15);
		priceRealMatrix = Utils.scalerMultiply(priceRealMatrix, -1);
		
//	    double[][] lprice = priceRealMatrix.getData();
//	    
		//System.out.println("Printing Price");
	    //for(int j = 0; j < priceRealMatrix.length ; j++ )
	    //{
	    	//System.out.println(priceRealMatrix[j]);
	    //}
	    
	    
		//objective.addTerms(priceRealMatrix.getData()[0],x_n);
		//cplex.prod(priceRealMatrix.getData()[0],x_n);
		//cplex.scalProd(priceRealMatrix.getData()[0],x_n);
	    
		
		double[] data = subtractOldMeanU(xold);
		
		//double[][] data = mat.getData();
		System.out.println("Data length" + data.length);
		IloNumExpr[] exps = new IloNumExpr[data.length];
		
		for(int i =0; i< data.length; i++)
		{	
			//System.out.println("Index: " + i);
			//(x_n - data_i)^2
			//exps[i] = cplex.square(cplex.sum(x_n[i], cplex.constant(-data[i])));
			//exps[i] = cplex.prod(rho/2, cplex.square(cplex.sum(x_n[i], cplex.constant(-data[i]))));
			exps[i] = cplex.sum(cplex.prod(priceRealMatrix[i], x_n[i]) ,cplex.prod(rho/2, cplex.square(cplex.sum(x_n[i], cplex.constant(-data[i])))));
			//IloNumExpr exp = cplex.constant(-data[i][0]);
			
			//exps[i] = cplex.sum(cplex.prod(lprice[i][0], x_n[i]) ,cplex.prod(rho/2, cplex.square(x_n[i] )));
			
		}
		
		IloNumExpr rightSide = cplex.sum(exps);
		//IloLinearNumExpr a = cplex.linearNumExpr();
		//cplex.addMinimize(objective);
		cplex.addMinimize(rightSide);
		
		
		for(int j = 0; j < data.length ; j++ )
		{
			cplex.addLe(x_n[j], -xa_min[j]);
			cplex.addGe(x_n[j], -xa_max[j]);
		}
		
		
		cplex.exportModel("model3.lp");
		//cplex.addMinimize(objective);
		cplex.solve();
		
		
		System.out.println("MASTER:: Optimal Value: " + cplex.getObjValue());
		//System.out.println(cplex.getStatus());
		
		x_optimal = new double[x_n.length];
		
		System.out.println("======= MASTER: OPTIMZATION ARRAY =====");
		for(int u=0; u< x_n.length; u++)
		{
			x_optimal[u] = cplex.getValues(x_n)[u];
			System.out.print(cplex.getValues(x_n)[u] + "\t");
		}
		
		System.out.println("=====");
		
		
		//TODO: Do error handling here. Check status of optimization
		//this.getx().setColumn(this.getN()-1, x_optimal);
		this.setX(Utils.setColumnInMatrix(this.getx(), x_optimal, this.getN() - 1));
		
		return cplex.getObjValue();
		
		
		
		
		
//		  GRBEnv    env   = new GRBEnv("mip1.log");
//	      GRBModel  model = new GRBModel(env);
//	      
//	      RealMatrix mat = subtractOldMeanU(xold);
//	      
//	      
//	      
//	      RealMatrix priceRealMatrix = MatrixUtils.createColumnRealMatrix(masterData.getPrice());
//	      priceRealMatrix.scalarMultiply(-15);
//	      
//	      
//	      GRBVar[] x_n = new GRBVar[masterData.getPrice().length];
//	      
//	      
//	      for(int i =0; i < masterData.getPrice().length; i++)
//	      {
//	    	  x_n[i] = model.addVar(0, GRB.INFINITY,0, GRB.CONTINUOUS, "x "+ Integer.toString(i));
//	      }
//	      
//	      //Loop this 96
//	      for(int i =0; i < masterData.getPrice().length; i++)
//	      {
//	    	   x_n[i] * mat.getData()[0][i];
//	      }
//	      
//	      model.update();
//	      
//	      GRBLinExpr obj = new GRBLinExpr();
//	      obj.addTerms(priceRealMatrix.getData()[0], x_n);
//	      obj.addTerms();
//	      
//	      
//	      model.setObjective();
	      
	}
	
	private double[] subtractOldMeanU(double[] xold)
	{
		return Utils.vectorAdd(Utils.vectorAdd(xold, this.xMean), this.u);
		//return xold.add(this.xMean).add(this.u);
	}
	
	public void setX(double[][] value)
	{
		this.x_master = value;
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
	
	public double[][] getx()
	{
		return this.x_master;
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
}
