package util;

public class TestDoubleInt {

	public static void main(String... args){
		for(int i = 0 ; i < 100 ; i++){
			// will effectively floo the value ... 
			int h = (int) i/2;
			System.out.println(i+" -> "+h);
		}
	}
	
}
