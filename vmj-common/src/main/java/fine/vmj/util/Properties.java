package fine.vmj.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Properties {

	Logger log = LoggerFactory.getLogger(VMJProperties.class);

	class SimpleProperty<A> {

		A value;
		String description;
		String name;

		public SimpleProperty(String name, String description, A default_value) {
			this.name = name;
			try {
				this.value = (A) System.getProperty(name);
			} catch (Exception e) {
				// ..
			}
			if (this.value == null) {
				this.value = default_value;
			}
			log.info("init property:" + name + " of type " + this.value.getClass().getSimpleName() + " as "
					+ this.value);
		}

		public A getValue() {
			return this.value;
		}

		public void setValue(A value) {
			this.value = value;
		}
	}
	
	class RangeProperty<A extends Number> extends SimpleProperty<Number>{
		
		private Number min;
		private Number max;
		private Number step;
		
		public RangeProperty(String name,String description ,  A min , A max ,A step , A default_value){
			super(name,description,default_value);
			this.min = min;
			this.max = max;
			this.step = step;
			this.normalize(this.value);
		}
		
		private void normalize(Number value){
			if(value.doubleValue() < min.doubleValue()){
				this.value = min;
			} else if(value.doubleValue() > max.doubleValue()){
				this.value = max;
			}
		}
		
		public double getDouble(){
			return value.doubleValue();
		}
		
		public int getInt(){
			return value.intValue();
		}
		
		public long getLong(){
			return value.longValue();
		}
		
	}

}
