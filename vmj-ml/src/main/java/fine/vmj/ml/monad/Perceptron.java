package fine.vmj.ml.monad;

import fine.vmj.ml.Activation;
import fine.vmj.ml.ActivationFactory;
import fine.vmj.ml.Aggregation;
import fine.vmj.ml.AggregationFactory;
import fine.vmj.ml.Evaluation;
import fine.vmj.ml.EvaluationFactory;
import fine.vmj.ml.Node;

public abstract class Perceptron extends Node<Double[],Double,Double> implements INeuron<Double[],Double> {

	double bias;
	final double[] weights;
	
	public Perceptron(int inp){
		super(AggregationFactory.get(Aggregation.Type.WEIGHTED_SUM),ActivationFactory.get(Activation.Type.PERCEPTRON),EvaluationFactory.get(Evaluation.Type.SQUR));
		this.weights = new double[inp];
	}
	
	@Override
	public Double feedforward(Double[] input){
		Double u = aggregation.aggregate(input);
		Double v = activation.activate(u);
		return v;
	}
	
	// GETTERS
	@Override
	public double[] getWeights(){
		return weights;
	}
	@Override
	public double getWeight(int i){
		if(weights.length >= i){
			throw new IllegalArgumentException("Cannot update weights at index "+i+". weights.length is "+weights.length);
		}
		return weights[i];
	}
	@Override
	public double getBias(){
		return bias;
	}
	
	//SETTERS
	@Override
	public void setWeights(double[] weights){
		if(weights.length != this.weights.length){
			throw new IllegalArgumentException("Cannot update weights["+this.weights.length+"] with different length weights["+weights.length+"]");
		}
	}
	@Override
	public void setWeight(int i , double weight){
		if(weights.length >= i){
			throw new IllegalArgumentException("Cannot update weights at index "+i+". weights.length is "+weights.length);
		}
		weights[i] = weight;
	}
	@Override
	public void setBias(double bias){
		this.bias = bias;
	}
	
	// ACTIONS
	@Override
	public abstract Perceptron clone();
	
}
