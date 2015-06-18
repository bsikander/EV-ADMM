package admm;

public class Result {
	private String peerName;
	private int Iteration;
	private int EV_number;
	private double[] x_old;
	private double[] xMean;
	private double[] u;
	private double[] x_optimal;
	private double cost_value;
	
	public Result(String peerName,int iteration, int ev_number, double[] x_old, double[] xMean, double[] u, double[] x_optimal, double costvalue)
	{
		this.peerName = peerName;
		this.Iteration = iteration;
		this.EV_number = ev_number;
		this.x_old = x_old;
		this.xMean = xMean;
		this.u = u;
		this.x_optimal = x_optimal;
		this.cost_value = costvalue;
	}
	
	public String printResult()
	{
		double sum = 0;
		for(double d: this.x_optimal)
			sum+=d;
		
		double sumxold=0;
		for(double d: this.x_old)
			sumxold+=d;
		
		double sumxMean=0;
		for(double d: this.xMean)
			sumxMean+=d;
		
		double sumu=0;
		for(double d: this.u)
			sumu+=d;
		
		String print = this.peerName + " \t " + this.Iteration + " \t\t " + this.EV_number + " \t " + this.cost_value + " \t " + sum + " \t " + sumxold + " \t " + sumxMean + " \t " + sumu;
		System.out.println(print);
		return print;
		
	}
	
}
