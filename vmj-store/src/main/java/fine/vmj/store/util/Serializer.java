package fine.vmj.store.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.store.data.ByteFrame;
import fine.vmj.store.impl.LocalFrameStore;

public interface Serializer<T> {
	
	static final Logger log = LoggerFactory.getLogger(Serializer.class);

	byte[] serialize(T obj) throws IOException ;
	
	T deserialize(byte[] bytes) throws IOException, ClassNotFoundException,ClassCastException;
	
	T deserialize(ObjectInputStream o) throws IOException, ClassNotFoundException;

	static abstract class Generic<T> implements Serializer<T>{
		
		public byte[] serialize(T obj) throws IOException {
	        try(ByteArrayOutputStream b = new ByteArrayOutputStream()){
	            try(ObjectOutputStream o = new ObjectOutputStream(b)){
	                o.writeObject(obj);
	            }
	            return b.toByteArray();
	        }
	    }

	    public T deserialize(byte[] bytes) throws IOException, ClassNotFoundException , ClassCastException {
	        try(ByteArrayInputStream b = new ByteArrayInputStream(bytes)){
	            try(ObjectInputStream o = new ObjectInputStream(b)){
	                return (T) o.readObject();
	            }
	        }
	    }
	    
	    public T deserialize(ObjectInputStream o) throws ClassNotFoundException, IOException{
	    	return (T) o.readObject();
	    }
		
	}
	
}
