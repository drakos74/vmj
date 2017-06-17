package fine.vmj.ml;

@FunctionalInterface public interface Evaluation<A> {
	
	A evaluate(A input,A output);
	
	enum Type{
		SQUR,STND
	}
	
}
