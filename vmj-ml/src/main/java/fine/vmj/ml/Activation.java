package fine.vmj.ml;

@FunctionalInterface public interface Activation<A> {

	A activate(A input);
	
	enum Type{
		PERCEPTRON,SIGMOID
	}
	
}
