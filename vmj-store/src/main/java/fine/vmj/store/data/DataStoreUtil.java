package fine.vmj.store.data;

public class DataStoreUtil {

	public static ByteFrame parseAsByteFrame(Frame frame){
		if(frame instanceof ByteFrame){
			return (ByteFrame) frame;
		}
		throw new IllegalStateException("Cannot cast to ByteFrame "+frame);
	}
	
//	public static PixelFrame parseAsPixelFrame(Frame frame){
//		if(frame instanceof PixelFrame){
//			return (PixelFrame) frame;
//		}
//		throw new IllegalStateException("Cannot cast to PixelFrame "+frame);
//	}
	
}
