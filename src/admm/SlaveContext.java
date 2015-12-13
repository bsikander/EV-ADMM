package admm;

import java.io.FileNotFoundException;
import java.io.IOException;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.CplexStatus;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hama.bsp.BSPPeer;

/*
 * This class loads the data for an EV and solves its model. An object of this class is created for each EV in each iteration.
 */
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
	private double[] xOptimalDifference;
	
	/*
	 * The default constructor which loads the EV from file system.
	 */
	public SlaveContext(SlaveData slaveData, double[] xMean, double[] u, int currentEVNo, double rhoValue, boolean isFirstIteration, BSPPeer<LongWritable, Text,IntWritable, Text, Text> peer, double delta, double[] oldXOptimal) throws IOException
	{	
		//firstIteration = isFirstIteration;
		//conf = peer.getConfiguration();
		
		//slaveData = Utils.LoadSlaveDataFromMatFile(fileName, peer);
		//this.slaveData = new SlaveData(evString);
		this.slaveData = slaveData;
		
		if(!isFirstIteration)
			this.x = oldXOptimal;
		else
			this.x = new double[u.length];
		
		x_optimal = new double[this.x.length];
		rho = rhoValue;
		
		this.alpha = ((0.05/3600) * (15*60)) / delta;
		
		this.xi_max = Utils.scalerMultiply(this.slaveData.getD(), 4);
		this.xi_min = Utils.scalerMultiply(this.slaveData.getD(), 0);
		
		this.xMean = xMean;
		this.u = u;
		this.currentEVNo = currentEVNo;
	}
	
	/*
	 * This function solves the model using IBM CPLEX and returns the cost. The key for a successful long run is that you do not
	 * create the CPLEX instances again and again because the objects are not cleaned up by GC. So, always use clearModel to
	 * clear out the old model and create a new one and reuse the same cplex object again and again.
	 */
	public double optimize(IloCplex cplex) throws IloException, FileNotFoundException
	{	
		cplex.clearModel();
		cplex.setOut(null);
		
		IloNumVar[] x_i = new IloNumVar[this.x.length];
		
		for(int i = 0; i < this.x.length ; i++) {
			x_i[i] = cplex.numVar(xi_min[i], xi_max[i]);
		}
		
	    double gammaAlpha = this.gamma * this.alpha;
		double[] data = subtractOldMeanU(this.x);
		
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
			//x_optimal = new double[x_i.length];
			
//			for(int u=0; u< x_i.length; u++)
//			{
//				x_optimal[u] = cplex.getValues(x_i)[u];
//			}
			x_optimal = cplex.getValues(x_i);
			
			//Calculate x_i^k - x_i^k-1
			xOptimalDifference = Utils.calculateVectorSubtraction(x_optimal, this.x);
			
			//Write the x_optimal to mat file
			//Updated: Don't write to harddrive, it is very expensive.
			//Utils.SlaveXToMatFile(evFileName, x_optimal, conf);
			
			double result = cplex.getObjValue();
			
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
			
			x_i = null;
			exps = null;
			AXExpEq = null;
			
			return 0;
		}
	}
	
	/*
	 * This function calculates -xold + xMean + u
	 */
	private double[] subtractOldMeanU(double[] xold)
	{
		xold = Utils.scalerMultiply(xold, -1);
		return Utils.vectorAdd(Utils.vectorAdd(xold, this.xMean),this.u);
	}
	
	/*
	 * Getter to access the current EV being processed.
	 */
//	public int getCurrentEVNo()
//	{
//		return this.currentEVNo;
//	}
	
	/*
	 * Getter for accessing xOptimal.
	 */
	public double[] getXOptimalSlave()
	{
		return this.x_optimal;
	}
	
	/*
	 * Getter to access the alpha.
	 */
	public double getAlpha()
	{
		return this.alpha;
	}
	
	/*
	 * Getter to access the difference of old optimal value and current optimal value (x_old* - x*)
	 */
	public double[] getXOptimalDifference() {
		return this.xOptimalDifference;
	}
	
	/*
	 * Getter to access the xi_max.
	 */
	public double[] getXimax()
	{
		return this.xi_max;
	}
	
	/*
	 * Getter to access the xi_min.
	 */
	public double[] getXimin()
	{
		return this.xi_min;
	}
	
	/*
	 * Setter to set u.
	 */
	public void setU(double[] u)
	{
		this.u = u;
	}
	
	/*
	 * Setter to set xMean.
	 */
	public void setXMean(double[] xmean)
	{
		this.xMean = xmean;
	}
	
	/*
	 * Getter for u.
	 */
	public double[] getU()
	{
		return this.u;
	}
	
	/*
	 * Getter for xMean.
	 */
	public double[] getXMean()
	{
		return this.xMean;
	}
	
	/*
	 * Getter for x.
	 */
	public double[] getX()
	{
		return this.x;
	}
	
}
