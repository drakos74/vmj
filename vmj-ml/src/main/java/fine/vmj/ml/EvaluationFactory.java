package fine.vmj.ml;

public class EvaluationFactory {

	public static Evaluation<Double> get(Evaluation.Type type, double... ds) {
		switch (type) {
		case SQUR:
			return new Diff();
		}
		return (Double x, Double y) -> x;
	}

	public static class Diff implements Evaluation<Double> {

		@Override
		public Double evaluate(Double input, Double output) {
			return Math.abs(input - output);
		}

	}

}
