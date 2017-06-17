package fine.vmj.ml;

import java.util.Arrays;
import java.util.stream.IntStream;

public class AggregationFactory {

	public static Aggregation<Double[], Double> get(Aggregation.Type type, double... ds) {
		switch (type) {
		case SUM:
			return new Sum();
		case WEIGHTED_SUM:
			return new WeightedSum(ds);
		case BIASED_WEIGHTED_SUM:
			// first element is bias
			return new BiasedWeightedSum(IntStream.range(1, ds.length).mapToDouble(i -> ds[i]).toArray(), ds[0]);
		}

		return null;
	}

	public static class Sum implements Aggregation<Double[], Double> {

		@Override
		public Double aggregate(Double[] input) {
			return Arrays.stream(input).mapToDouble(d -> d).sum();
		}

	}

	public static class WeightedSum extends Sum {

		final double[] weights;

		public WeightedSum(double[] weights) {
			super();
			this.weights = weights;
		}

		@Override
		public Double aggregate(Double[] input) {
			if (input.length != weights.length) {
				throw new IllegalArgumentException(
						"Length of inputs[" + input.length + "] and weights[" + weights.length + "] must be equal.");
			}
			return IntStream.range(0, weights.length).mapToDouble(i -> {
				return input[i] * weights[i];
			}).sum();
		}

	}

	public static class BiasedWeightedSum extends WeightedSum {

		final double bias;

		public BiasedWeightedSum(double[] weights, double bias) {
			super(weights);
			this.bias = bias;
		}

		@Override
		public Double aggregate(Double[] input) {
			if (input.length != weights.length) {
				throw new IllegalArgumentException(
						"Length of inputs[" + input.length + "] and weights[" + weights.length + "] must be equal.");
			}
			return IntStream.range(0, weights.length).mapToDouble(i -> {
				return input[i] * weights[i];
			}).sum() + bias;
		}

	}

}
