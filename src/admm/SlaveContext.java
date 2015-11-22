package admm;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.CplexStatus;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hama.bsp.BSPPeer;

public class SlaveContext {
	double[] x_salve;
	private double gamma = 0;
	private double alpha;
	private double rho;
	private double[] xi_max;
	private double[] xi_min;
	double[] xMean;
	private double[] u;
	private SlaveData slaveData;
	private double[] x_optimal;
	private double[] x;
	int currentEVNo;
	private String evFileName;
	private boolean firstIteration = true;
	private Configuration conf;
	private double[] xOptimalDifference;
	
	public SlaveContext(String fileName, double[] xMean, double[] u, int currentEVNo, double rhoValue, boolean isFirstIteration, BSPPeer<NullWritable, NullWritable,IntWritable, Text, Text> peer, double delta) throws IOException
	{	
		firstIteration = isFirstIteration;
		conf = peer.getConfiguration();
		
		slaveData = Utils.LoadSlaveDataFromMatFile(fileName, firstIteration, peer);
		this.x = slaveData.getXOptimal(); //Read the last optimal value directly from the .mat file
		x_optimal = new double[this.x.length];
		
		if(!isFirstIteration)
			xOptimalDifference = slaveData.getXOptimal(); //Take the old value of optimal value from the mat file
		else
			xOptimalDifference = new double[slaveData.getXOptimal().length];
		
		rho = rhoValue;
		evFileName = fileName; 
		
		this.alpha = ((0.05/3600) * (15*60)) / delta;
		//System.out.println("DELTA --> " + delta + "  ALPHA --> " + this.alpha);
		
		this.xi_max = Utils.scalerMultiply(this.slaveData.getD(), 4);
		this.xi_min = Utils.scalerMultiply(this.slaveData.getD(), 0);
//		this.xi_max = Utils.scalerMultiply(this.slaveData.getD(), 20);
//		this.xi_min = Utils.scalerMultiply(this.slaveData.getD(), -20);
		
		this.xMean = xMean;
		this.u = u;
		this.currentEVNo = currentEVNo;
	}
	
	public double optimize(IloCplex cplex) throws IloException, FileNotFoundException
	{
		//IloCplex cplex = new IloCplex();
		//OutputStream out = new FileOutputStream("logfile_slave");
		//cplex.setOut(out);
		//Ilo
		cplex.clearModel();
		cplex.setOut(null);
		
		IloNumVar[] x_i = new IloNumVar[x.length];
		
		for(int i = 0; i < x.length ; i++) {
			x_i[i] = cplex.numVar(xi_min[i], xi_max[i]);
		}
		
	    double gammaAlpha = this.gamma * this.alpha;
		double[] data = subtractOldMeanU(x);
		
		IloNumExpr[] exps = new IloNumExpr[data.length];
		
		for(int i =0; i< data.length; i++)
		{	
			exps[i] = cplex.sum(cplex.prod(gammaAlpha, cplex.square(x_i[i])) ,cplex.prod(rho/2, cplex.square(cplex.sum(x_i[i], cplex.constant(data[i])))));
		}
		
		IloNumExpr rightSide = cplex.sum(exps);
		cplex.addMinimize(rightSide);
		
		IloNumExpr[] AXExpEq = new IloNumExpr[data.length];
		
		for(int j = 0; j < data.length ; j++ )
		{	
			//cplex.addEq(cplex.prod(x_i[j], this.slaveData.getA()[j]), this.slaveData.getR());
			AXExpEq[j] = cplex.prod(x_i[j], this.slaveData.getA()[j]);
		}
		cplex.addEq(cplex.sum(AXExpEq), this.slaveData.getR());
		
		//S_min <= B_i*x_i <= S_max
		
		//After talking with Jose, he mentioned that this part is not required
//		for(int h=0; h < this.slaveData.getB().length; h++)
//		{
//			IloNumExpr[] BXExpLe = new IloNumExpr[this.slaveData.getB()[0].length];
//			IloNumExpr[] BXExpGe = new IloNumExpr[this.slaveData.getB()[0].length];
//			
//			
//			for(int f=0; f < this.slaveData.getB()[0].length; f++)
//			{	
//				BXExpLe[f] = cplex.prod(x_i[f],this.slaveData.getB()[h][f]);
//				BXExpGe[f] = cplex.prod(x_i[f],this.slaveData.getB()[h][f]);
//			}
//			
//			cplex.addLe(cplex.sum(BXExpLe), this.slaveData.getSmax()[h]);
//			cplex.addGe(cplex.sum(BXExpGe), this.slaveData.getSmin()[h]);
//		}
		
//		if(firstIteration)
//			cplex.exportModel("EV_" + currentEVNo + ".lp");
		
		if ( cplex.solve() ) {
			//cplex.solve();
			
			x_optimal = new double[x_i.length];
			
			for(int u=0; u< x_i.length; u++)
			{
				x_optimal[u] = cplex.getValues(x_i)[u];
			}
			
			//System.out.println("@@@@@@@Slave Opti Start");
			//Utils.PrintArray(x_optimal);
			
			//Calculate x_i^k - x_i^k-1
			xOptimalDifference = Utils.calculateVectorSubtraction(x_optimal, xOptimalDifference);
			
			//Write the x_optimal to mat file
			Utils.SlaveXToMatFile(evFileName, x_optimal, conf);
			
			//TODO: Print -Remove
	//		System.out.println("$$$$$$$$$$////////// Cost Value ////// + " + cplex.getObjValue());
			double result = cplex.getObjValue();
			
			//cplex = null;
			//cplex.end();
			x_i = null;
			exps = null;
			AXExpEq = null;
			
			return result;
		}
		else
		{
			System.out.println(">>> CPLEX SOLVE FAILED !!!");
			//cplex.exportModel("EV_" + currentEVNo + ".lp");
			Utils.PrintArray(this.x);
			Utils.PrintArray(this.xMean);
			Utils.PrintArray(this.u);
			System.out.println("RHO -> " + this.rho + " >> Alpha->" + this.alpha + " >>gamma ->" + this.gamma);
			
			CplexStatus s = cplex.getCplexStatus();
			//cplex.exportModel("27MatFileModel.lp");
			System.out.println("Status -> " + s.toString());
			//System.out.println("CPlex -> " + cplex.getModel().toString());
			
//			IloRange[] con = new IloRange[cplex.getNrows()];
//			System.out.println("Size -> " + cplex.getNrows());
//			
//			Iterator it = cplex.iterator();
//			int i=0;
//			while (it.hasNext()) {
//			  Object thing = it.next();
////			  if (thing instanceof IloNumVar) {
////			    System.out.print("Variable ");
////			  }
//			   if (thing instanceof IloRange) {
//				   con[i] = (IloRange) thing;
//			  }
//			  else if (thing instanceof IloObjective) {
//			    System.out.print("Objective ");
//			  }
//			  System.out.println("named " +
//			                     ((IloAddable) thing).getName());
			//}
//			cplex.getinf
//			
//			
//			
//			cplex.feasOpt(con, arg1, arg2)
			//cplex.getInfeasibilities(cplex.feasOpt(arg0, arg1))
			//System.out.println("IIS -> " + cplex.getIIS().toString());
//			cplex.getModel();
//			
//			cplex.getInfeasibilities()
			
//			cplex.getin
//			cplex.feasOpt(cplex., arg1)
			
			//cplex = null;
			//cplex.end();
			x_i = null;
			exps = null;
			AXExpEq = null;
			
			return 0;
		}
	}
	
	
	private double[] subtractOldMeanU(double[] xold)
	{
		xold = Utils.scalerMultiply(xold, -1);
		return Utils.vectorAdd(Utils.vectorAdd(xold, this.xMean),this.u);
		
		//K= xold - xmean - u;
//		this.xMean = Utils.scalerMultiply(this.xMean, -1);
//		this.u = Utils.scalerMultiply(this.u, -1);
//		return Utils.vectorAdd(Utils.vectorAdd(xold, this.xMean),this.u);
	}
	
	public int getCurrentEVNo()
	{
		return this.currentEVNo;
	}
	
	public double[] getXOptimalSlave()
	{
		return this.x_optimal;
	}
	
	public double getAlpha()
	{
		return this.alpha;
	}
	
	public double[] getXOptimalDifference() {
		return this.xOptimalDifference;
	}
	
	
	public double[] getXimax()
	{
		return this.xi_max;
	}
	
	public double[] getXimin()
	{
		return this.xi_min;
	}
	
	public void setU(double[] u)
	{
		this.u = u;
	}
	
	public void setXMean(double[] xmean)
	{
		this.xMean = xmean;
	}
	
	public double[] getU()
	{
		return this.u;
	}
	
	public double[] getXMean()
	{
		return this.xMean;
	}
	
	public double[] getX()
	{
		return this.x;
	}
	
}
