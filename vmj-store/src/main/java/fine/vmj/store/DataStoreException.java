package fine.vmj.store;

public class DataStoreException extends Exception {

	private static final long serialVersionUID = 1L;

	public DataStoreException(String reason){
		super(reason);
	}
	
	public DataStoreException(String reason , Exception ex){
		super(reason,ex);
	}
	
}
