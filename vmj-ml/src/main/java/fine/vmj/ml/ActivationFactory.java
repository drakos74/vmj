package fine.vmj.ml;

public class ActivationFactory {

	public static Activation<Double> get(Activation.Type type, double... ds) {
		switch (type) {
		case PERCEPTRON:
			return new Perceptron(ds[0]);
		}
		return (Double x) -> x;
	}

	public static class Perceptron implements Activation<Double> {

		final double threshold;

		Perceptron(double threshold) {
			this.threshold = threshold;
		}

		@Override
		public Double activate(Double input) {
			return input > threshold ? 1.0 : 0.0;
		}

	}

}
