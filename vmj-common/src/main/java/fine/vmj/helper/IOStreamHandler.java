package fine.vmj.helper;

import java.io.File;

@FunctionalInterface public interface IOStreamHandler {

	void apply(File file , int i);
	
}
