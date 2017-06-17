package fine.vmj.ml.perc.multi;

import org.junit.Ignore;
import org.junit.Test;

import fine.vmj.ml.demo.SimpleSingleLayerPerceptron;

public class SimpleMultiLayerPerceptronTest {

	@Test
	@Ignore // test takes too long
	public void testLayerEmptyrun(){
		try{
			SimpleSingleLayerPerceptron.main(new String[0]);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
