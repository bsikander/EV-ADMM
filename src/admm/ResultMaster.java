package admm;

public class ResultMaster {
	String peerName;
	int iteration;
	int ev;
	double[] u;
	double[] xMean;
	double[] x_master_optimal;
	double costValue;
	double[] evAverage;
	double s_norm;
	double r_norm;
	double cost;
	
	public ResultMaster(String peerName,int iteration, int ev_number, double[] u, double[] xMean, double[] x_optimal, double costvalue, double[] evAverageValue, double s_norm, double r_norm, double cost)
	{
		this.peerName = peerName;
		this.iteration = iteration;
		this.ev = ev_number;
		this.u = u;
		this.xMean = xMean;
		this.x_master_optimal = x_optimal;
		this.costValue = costvalue;
		this.evAverage = evAverageValue;
		this.s_norm = s_norm;
		this.r_norm = r_norm;
		this.cost = cost;
	}
	
	public String printResult(int count)
	{
		double[] temp = evAverage;
		
		double x_evSum = 0;
		for(int k=0; k < temp.length;k++)
			x_evSum += temp[k];
		
		double sum = 0;
		for(double d: this.x_master_optimal)
			sum+=d;

		double sumxMean=0;
		for(double d: this.xMean)
			sumxMean+=d;
		
		double sumu=0;
		for(double d: this.u)
			sumu+=d;
		
		if(count>0) {
			String print = "M:=> " + this.peerName + " \t " + this.iteration + " \t\t " + this.ev + " \t " + this.costValue + " \t " + x_evSum + " \t " + sum  + " \t " + sumxMean + " \t " + sumu + "\t" + this.x_master_optimal[0] + "\t" + this.xMean[0] + "\t" + this.u[0] + "\t s-norm: " + this.s_norm + "\t r_norm: " + this.r_norm;
			System.out.println(print + " \t cost: " + this.cost);
			return print;
		}
		else {
			String print = "M:=> " + this.peerName + " \t " + this.iteration + " \t\t " + this.ev + " \t " + this.costValue + " \t " + x_evSum + " \t " + sum  + " \t " + sumxMean + " \t " + sumu + "\t" + this.x_master_optimal[0] + "\t" + this.xMean[0] + "\t" + this.u[0] + "\t s-norm: " + this.s_norm + "\t r_norm: " + this.r_norm;
			System.out.println(print + "\t cost: " + this.cost);
			return print;
		}
		
	}
}
