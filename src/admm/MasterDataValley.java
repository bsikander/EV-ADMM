package admm;

public class MasterDataValley {
	private double[] D;
	private double[] price;
	private double delta;
	
	public MasterDataValley(double[] D, double[] price) {
		this.D = D;
		this.price = scalePrice(price);
		
		System.out.println("D START");
		Utils.PrintArray(this.price);
		Utils.PrintArray(D);
		System.out.println("D ENDDDDD");
		
		this.delta = Utils.calculateMean(this.price) / Utils.calculateMean(D);	//Equation to calculate alpha is different in valley
	}
	
	public double[] getD() {
		return this.D;
	}
	
	public double getDelta()
	{
		return this.delta;
	}
	
	public double[] getPrice() {
		return this.price;
	}
	
	private double[] scalePrice(double[] price)
	{
		double[] scaledPrice = new double[price.length*4];
		int index = 0;
		for(double d : price)
		{	
			scaledPrice[index] = (d/(3600*1000)) * (15*60);
			scaledPrice[index+1] = (d/(3600*1000)) * (15*60);
			scaledPrice[index+2] = (d/(3600*1000)) * (15*60);
			scaledPrice[index+3] = (d/(3600*1000)) * (15*60);
			
			index += 4;
		}
		
		return scaledPrice;
	}
}
