package fine.vmj.ml.monad;

import java.util.stream.IntStream;

import fine.vmj.ml.Activation;
import fine.vmj.ml.Aggregation;
import fine.vmj.ml.Evaluation;
import fine.vmj.ml.Node;

public abstract class NLayer extends Node<Double[],Double[],Double[]> implements INLayer<Double[],Double> {

	private INeuron<Double[],Double>[] neurons;

	public NLayer(Aggregation<Double[], Double[]> aggregation, Activation<Double[]> activation,
			Evaluation<Double[]> evaluation) {
		super(aggregation, activation, evaluation);
		// TODO Auto-generated constructor stub
	}

	
//	public NLayer(INeuron<Double[],Double> neuron , int size){
//		this.neurons = new INeuron[size];
//		IntStream.range(0,size).forEach(n -> neurons[n] = neuron.clone() );
//	}
	
	@Override
	public int getSize(){
		return neurons.length;
	}
}
