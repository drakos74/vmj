package fine.vmj.store.data;


public class PixelFrame extends Frame {

	private static final long serialVersionUID = 1L;

	final double[] mat;
	public String name;

	public PixelFrame(double[] mat, IFrame.Rect rect, int index , int type) {
		super(rect,index,type);
		this.mat = mat;
	}
	
	public double[] getData(){
		return mat;
	}

}
