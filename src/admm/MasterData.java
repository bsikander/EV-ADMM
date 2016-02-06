package admm;

public class MasterData {
	private double[] re;
	private double[] D;
	private double[] price;

	public MasterData(double[] re, double[] D, double[] price) {
		this.re = re;
		this.D = D;
		this.price = scalePrice(price);
	}

	public double[] getRe() {
		return this.re;
	}

	public double[] getD() {
		return this.D;
	}

	public double[] getPrice() {
		return this.price;
	}

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
