package fine.vmj.store.data;


public class ByteFrame extends Frame{

	private static final long serialVersionUID = 1L;
	
	final byte[] mat;

	public String name;

	public ByteFrame(byte[] mat, Rect rect, int index , int type) {
		super(rect,index,type);
		this.mat = mat;
	}
	
	public byte[] getBytes(){
		return mat;
	}

}
