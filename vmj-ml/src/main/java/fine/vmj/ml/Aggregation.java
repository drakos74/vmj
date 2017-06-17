package fine.vmj.ml;

@FunctionalInterface public interface Aggregation<I,O> {

	O aggregate(I input);
	
	enum Type{
		SUM,WEIGHTED_SUM,BIASED_WEIGHTED_SUM
	}
	
}
