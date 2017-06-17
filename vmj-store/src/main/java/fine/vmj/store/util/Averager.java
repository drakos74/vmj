package fine.vmj.store.util;

import java.util.Arrays;
import java.util.function.Consumer;

// TODO : fix averager for future use
public class Averager {

	public static class Frame implements Consumer<byte[]>{

		 	private byte[] total_bytes;
		    private int count = 0;
		        
		    public byte[] average() {
		        return null;// Arrays.stream(total_bytes)
		    }
			
		    public void combine(Frame other) {
//		        total += other.total;
		        count += other.count;
		    }


			@Override
			public void accept(byte[] bytes) {
		    	total_bytes = bytes;
				count++;
			}
		    
	}
	
}
