package fine.vmj.cv;

public class FrameTransformException extends RuntimeException {

	public FrameTransformException(String msg){
		super(msg);
	}
	
	public FrameTransformException(Throwable e){
		super(e);
	}
	
	public FrameTransformException(String msg , Throwable e){
		super(msg,e);
	}
	
}
