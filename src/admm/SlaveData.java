package admm;

/*
 * This class represents the data for a single EV that is loaded from file system. 
 */
public class SlaveData {
	private double[] d;
	private double[] A;
	private double[][] B;
	private double R;
	private double[] Smax;
	private double[] Smin;
	
	/*
	 * The default constructor which takes all the objects as parameters that were loaded from file system for a single EV.
	 */
	public SlaveData(double[] d, double[] A, double[][] B, double R, double[] Smax, double[] Smin)
	{
		this.d = d;
		this.A = A;
		this.B = B;
		this.R = R;
		this.Smax = Utils.scalerAdd(Smax, 0.0001);
		this.Smin = Utils.scalerAdd(Smin, -0.0001);
	}
	
	public double[] getD() {
		return this.d;
	}
	
	public double[] getA() {
		return this.A;
	}
	
	public double[][] getB() {
		return this.B;
	}
	
	public double getR() {
		return this.R;
	}
	
	public double[] getSmax() {
		return this.Smax;
	}
	
	public double[] getSmin() {
		return this.Smin;
	}
}
