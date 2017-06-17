package util;

import org.junit.Assert;
import org.junit.Test;

public class TestModulo {

	
	public static void main(String... args){
		
		int[] ints = new int[]{0,1,2,3,4};
		
		for(int i = 0 ; i < 100 ; i++){
			int mod = i % ints.length;
			System.out.println(mod+" - "+ints[mod]);
		}
	}
	
	@Test
	public void testModuloConditions(){
		int[] ints = new int[]{0,1,2,3,4};
		
		for(int i = 0 ; i < 100 ; i++){
			int mod = i % ints.length;
			System.out.println(mod+" - "+ints[mod]);
			Assert.assertTrue(mod == ints[mod]);
		}
	}
	
}
