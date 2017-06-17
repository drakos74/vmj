package fine.vmj.store;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.store.util.Serializer;
import fine.vmj.util.VMJProperties;

public abstract class DataStore<K, V> {

	protected static final Logger log = LoggerFactory.getLogger(DataStore.class);

	protected static boolean logOn = fine.vmj.util.VMJProperties.DATASTORE_LOG_ON.getValue();

	protected static boolean localCacheIsOn = fine.vmj.util.VMJProperties.DATASTORE_LOCAL_CACHE.getValue();

	// static values

	private static final String OBJECT_COUNT = "OBJECT_SIZE";
	private static final String KEY_SIZE = "KEY_SIZE";
	private static final String VALUE_SIZE = "VALUE_SIZE";

	// static objects ...

	private static final Map<ByteArray, byte[]> storage = new ConcurrentHashMap<ByteArray, byte[]>();

	private static final Map<String, AtomicLong> metrics = new ConcurrentHashMap<String, AtomicLong>();

	protected final static ExecutorService executor = Executors.newFixedThreadPool(10);

	protected final Serializer<K> keySerializer;
	protected final Serializer<V> valueSerializer;

	private final long max_byte_storage;
	private final int max_object_storage;
	
	protected final String[] columns;

	protected DataStore(String... columns) {
		this.keySerializer = new KeySerializer();
		this.valueSerializer = new ValueSerializer();
		max_byte_storage = fine.vmj.util.VMJProperties.STORE_MAX_BYTE_SIZE.getValue();
		max_object_storage = fine.vmj.util.VMJProperties.STORE_MAX_OBJECT_SIZE.getValue();
		metrics.put(KEY_SIZE, new AtomicLong(0));
		metrics.put(VALUE_SIZE, new AtomicLong(0));
		metrics.put(OBJECT_COUNT, new AtomicLong(0));
		this.columns = columns.length > 0 ? columns : VMJProperties.DATASTORE_LOCAL_DIR.getValue().split("/");
	}

	protected DataStore(Serializer<K> keySerializer, Serializer<V> valueSerializer, int max_object_size,
			long max_byte_size, String... columns) {
		this.keySerializer = keySerializer;
		this.valueSerializer = valueSerializer;
		this.max_byte_storage = max_byte_size;
		this.max_object_storage = max_object_size;
		this.columns = columns.length > 0 ? columns : VMJProperties.DATASTORE_LOCAL_DIR.getValue().split("/");
	}

	public V get(K key) throws DataStoreException {
		byte[] _key;
		try {
			_key = keySerializer.serialize(key);
		} catch (Exception e) {
			throw new DataStoreException("Could not serialize key [" + key + "]", e);
		}
		return __get(_key);
	}

	V __get(byte[] key) throws DataStoreException {
		byte[] _value = localCacheIsOn ? internal_get(key) : external_get(key,getColumnFamilies());
		log.info("_got " + _value);
		if (_value == null) {
			return null;
		}

		try {
			V value = valueSerializer.deserialize(_value);
			log.info("got " + value);
			return value;
		} catch (Exception e) {
			throw new DataStoreException("Could not deserialize value of [" + _value + ":" + _value.length + "]");

		}
	}

	protected byte[] internal_get(byte[] key) {
		return storage.get(new ByteArray(key));
	}

	public void put(K key, V value) throws DataStoreException {
		byte[] _key;
		byte[] _value;

		try {
			_key = keySerializer.serialize(key);
		} catch (Exception e) {
			throw new DataStoreException("Could not serialize key [" + key + "]", e);
		}

		try {
			_value = valueSerializer.serialize(value);
		} catch (Exception e) {
			throw new DataStoreException("Could not serialize value [" + value + "]", e);
		}

		__put(_key, _value);

	}

	protected void __put(byte[] key, byte[] value) throws DataStoreException {
		long __key_byte_size = metrics.get(KEY_SIZE).addAndGet(key.length);
		long __value_byte_size = metrics.get(VALUE_SIZE).addAndGet(value.length);

		long __byte_size = __key_byte_size + __value_byte_size;

		log.info("total_bytes in store " + __byte_size);

		if (__byte_size > max_byte_storage) {
			throw new DataStoreException("Byte size [" + __byte_size + "] of [" + (key.length) + "+" + value.length
					+ "] is bigger than threshold [" + max_byte_storage + "]");
		}

		long __obj_size = metrics.get(OBJECT_COUNT).addAndGet(1);
		if (__obj_size > max_object_storage) {
			throw new DataStoreException("Objects size [" + __obj_size + "] of [" + 1 + "] is bigger than threshold ["
					+ max_object_storage + "]");
		}

		if (key != null && value != null) {
			if (localCacheIsOn) {
				internal_put(key, value);
			} else {
				external_put(key, value, createColumnFamilies());
			}

		} else {
			throw new DataStoreException("Could not store value of [" + key + "," + value + "]");
		}
	}

	protected void internal_put(byte[] key, byte[] value) {
		storage.put(new ByteArray(key), value);
	}

	protected boolean clear(K key) throws DataStoreException {
		byte[] _key;
		try {
			_key = keySerializer.serialize(key);
		} catch (Exception e) {
			throw new DataStoreException("Could not serialize key [" + key + "]", e);
		}
		return __clear(_key);
	}

	boolean __clear(byte[] key) throws DataStoreException {
		return localCacheIsOn ? internal_clear(key) : external_clear(key);
	}

	protected boolean internal_clear(byte[] key) {
		try {
			byte[] prev = storage.remove(new ByteArray(key));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public Set<V> getAll() throws DataStoreException {
		Set<K> keys = getKeys();
		log.info("got " + keys.size() + " keys ... ");
		if (keys.stream().anyMatch(k -> k == null)) {
			throw new DataStoreException("Could not serialize all keys");
		}

		return keys.stream().map(t -> {
			try {
				return get(t);
			} catch (DataStoreException de) {
				log.error(de.getMessage(), de);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			return null;
		}).filter(f -> f != null).collect(Collectors.toSet());
	}

	@SuppressWarnings("unchecked")
	protected Set<K> getKeys() throws DataStoreException {
		Set<ByteArray> keys = localCacheIsOn ? __internal_get_keys() : external_get_keys();
		log.info(" _internal_keys:" + keys.size());
		return keys.stream().map(k -> {
			try {
				return keySerializer.deserialize(k.bytes);
			} catch (ClassNotFoundException | ClassCastException | IOException e) {
				// throw new DataStoreException("Could not deserialize key
				// :"+k);
				log.error("Could not deserialize key :" + k, e);
				return null;
			}
		}).collect(Collectors.toSet());
	}

	protected Set<ByteArray> __internal_get_keys() {
		return storage.keySet();
	}

	// STATS methods

	public double getSize() {
		return (double) (metrics.get(KEY_SIZE).get() + metrics.get(VALUE_SIZE).get())
				/ fine.vmj.util.VMJProperties.STORE_MAX_BYTE_SIZE.getValue();
	}

	public double getCount() {
		return (double) metrics.get(OBJECT_COUNT).intValue()
				/ fine.vmj.util.VMJProperties.STORE_MAX_OBJECT_SIZE.getValue();
	}

	public void flush() throws DataStoreException {

		log.info("pre-check ... ");

		log.info("storage:" + storage.size());
		storage.forEach((k, v) -> {
			log.info("k:" + k + ",v" + v);
		});

		log.info("main...");

		Set<ByteArray> keys = __internal_get_keys();
		log.info("got " + keys.size() + " keys ... ");
		if (keys.stream().anyMatch(k -> k == null)) {
			log.error("Could not deserialise all keys ... ");
			throw new DataStoreException("Could not serialize all keys");
		}

		keys.stream().forEach(t -> {
			try {
				byte[] value = internal_get(t.bytes);
				if (value == null) {
					log.error("Value is null at key : " + t);
				} else {
					external_put(t.bytes,value,getColumnFamilies());
				}
			} catch (DataStoreException de) {
				log.error(de.getMessage(), de);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		});

	}
	
	public Map<K,V> load() throws DataStoreException{
		try{
			return external_load();
		}catch(Exception e){
			throw new DataStoreException("Could not load columnFamilies "+Arrays.toString(columns),e);
		}
	}
	
	protected abstract Map<K,V> external_load();

	protected abstract void external_put(byte[] key, byte[] value, String... columnFamilies) throws DataStoreException;

	protected abstract byte[] external_get(byte[] key, String... columnFamilies) throws DataStoreException;

	protected abstract Set<ByteArray> external_get_keys() throws DataStoreException;

	protected abstract boolean external_clear(byte[] key) throws DataStoreException;

	protected String[] getColumnFamilies(){
		return columns;
	}
	
	protected String[] createColumnFamilies(){
		return columns;
	}
	
	private class KeySerializer extends Serializer.Generic<K> {
	};

	private class ValueSerializer extends Serializer.Generic<V> {
	};

	public static class ByteArray {

		private byte[] bytes;

		ByteArray(byte[] bytes) {
			this.bytes = bytes;
		}

		@Override
		public int hashCode() {
			return bytes.length;
		}

		@Override
		public boolean equals(Object other) {

			if (other instanceof ByteArray) {
				ByteArray that = (ByteArray) other;
				return Arrays.equals(this.bytes, that.bytes);
			} else {
				return false;
			}

		}

	}

}
