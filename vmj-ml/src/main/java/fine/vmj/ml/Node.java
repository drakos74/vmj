package fine.vmj.ml;

public abstract class Node<I,O,Y> implements Aggregation<I,O> , Activation<O> , Evaluation<Y> {

	protected Aggregation<I,O> aggregation;
	protected Activation<O> activation;
	protected Evaluation<Y> evaluation;
	
	public Node(Aggregation<I,O> aggregation, Activation<O> activation , Evaluation<Y> evaluation){
		this.aggregation = aggregation;
		this.activation = activation;
		this.evaluation = evaluation;
	}
	
	@Override
	public O aggregate(I input){
		return aggregation.aggregate(input);
	}
	
	@Override
	public O activate(O input){
		return activation.activate(input);
	}
	
	@Override 
	public Y evaluate(Y input , Y output){
		return evaluation.evaluate(input, output);
	}
	
}
