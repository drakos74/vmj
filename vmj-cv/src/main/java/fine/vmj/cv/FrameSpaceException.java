package fine.vmj.cv;

public class FrameSpaceException extends RuntimeException {

	public FrameSpaceException(String msg) {
		super(msg);
	}
	
	public FrameSpaceException(Throwable e){
		super(e);
	}
	
	public FrameSpaceException(String msg , Throwable e){
		super(msg,e);
	}
	
}
