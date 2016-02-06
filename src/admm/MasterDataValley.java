package admm;

/*
 * This class stores the data loaded by Aggregator
 */
public class MasterDataValley {
	private double[] D;
	private double[] price;
	private double delta;

	/*
	 * The default constructor of the class. This function also sclaes the value
	 * of price.
	 */
	public MasterDataValley(double[] D, double[] price) {
		this.D = D;
		this.price = scalePrice(price);

		System.out.println("Demand Array Start");
		Utils.PrintArray(this.price);
		Utils.PrintArray(D);
		System.out.println("Demand Array ENDnd");

		// this.delta = Utils.calculateMean(this.price) /
		// Utils.calculateMean(D); //Equation to calculate alpha is different in
		// valley
		// System.out.println("Price Mean --> " +
		// Utils.calculateMean(this.price));
		// System.out.println("D Mean --> " + Utils.calculateMean(D));
	}

	/*
	 * Getter for Demand
	 */
	public double[] getD() {
		return this.D;
	}

	/*
	 * Setter for demand.
	 */
	public void setD(double[] d) {
		this.D = d;
		this.delta = Utils.calculateMean(this.price)
				/ Utils.calculateMean(this.D); // Equation to calculate alpha is
												// different in valley
		System.out.println("Price Mean --> " + Utils.calculateMean(this.price));
		System.out.println("D Mean --> " + Utils.calculateMean(D));
	}

	/*
	 * Getter for delta.
	 */
	public double getDelta() {
		return this.delta;
	}

	/*
	 * Getter for price.
	 */
	public double[] getPrice() {
		return this.price;
	}

	/*
	 * This function scales the price.
	 */
	private double[] scalePrice(double[] price) {
		double[] scaledPrice = new double[price.length * 4];
		int index = 0;
		for (double d : price) {
			scaledPrice[index] = (d / (3600 * 1000)) * (15 * 60);
			scaledPrice[index + 1] = (d / (3600 * 1000)) * (15 * 60);
			scaledPrice[index + 2] = (d / (3600 * 1000)) * (15 * 60);
			scaledPrice[index + 3] = (d / (3600 * 1000)) * (15 * 60);

			index += 4;
		}

		return scaledPrice;
	}
}
