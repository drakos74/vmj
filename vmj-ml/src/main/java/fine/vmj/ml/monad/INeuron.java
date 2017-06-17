package fine.vmj.ml.monad;

public interface INeuron<I,O>{

	void setWeights(double[] weights);
	void setWeight(int i , double weight);
	void setBias(double bias);
	
	double[] getWeights();
	double getWeight(int i);
	double getBias();
	
	INeuron<I,O> clone();
	
	O feedforward(I input);
	
}
