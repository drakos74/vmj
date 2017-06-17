package fine.vmj.store.data;

import java.io.Serializable;

public interface IFrame extends Serializable {
	
//	Rect getRect();
//	
//	int getIndex();
//	
//	int getType();

	class Rect implements Serializable {

		private static final long serialVersionUID = 1L;
		
		final int x;
		final int y;
		final int w;
		final int h;

		public Rect(int x, int y, int size) {

			this.x = x;
			this.y = y;
			this.w = size;
			this.h = size;

		}
		
		public Rect(int x, int y, int width , int height) {

			this.x = x;
			this.y = y;
			this.w = width;
			this.h = height;

		}
		
		@Override
		public boolean equals(Object rect){
			if(rect instanceof Rect == false){
				System.out.println(rect+" is not of type Rect...");
				return false;
			}
			
			Rect other = (Rect) rect;
			
//			System.out.println("other:"+other);
//			System.out.println("this:"+this);
			
			return 
					other.x == this.x &&
					other.y == this.y &&
					other.w == this.w &&
					other.h == this.h;
		}
		
		public int getX(){
			return x;
		}
		
		public int getY(){
			return y;
		}
		
		public int getWidth(){
			return w;
		}
		
		public int getHeight(){
			return h;
		}
		
		@Override 
		public String toString(){
			return "[x:"+x+",y:"+y+",w:"+w+",h:"+h+"]";
		}
	}

	enum Category {
		CAT1(1),
		CAT2(2),
		CAT3(3),
		CAT4(4),
		CAT5(5),
		CAT6(6),
		CAT7(7),
		CAT8(8),
		CAT9(9),
		CAT10(10),
		CAT11(11),
		CAT12(12);
		
		private static final String prefix = "CAT";
		
		private final int index;
		
		Category(int i){
			this.index = i;
		}
		
		public int getIndex(){
			return index;
		}
		
		public Category getNext(){
			try{
				return Category.valueOf(prefix+(index+1));
			}catch(Exception e){
				return null;
			}
		}
		
	}
	
}
