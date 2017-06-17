package fine.vmj.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.store.impl.LocalFrameStore;

public class DataStoreFactory {

	protected static final Logger log = LoggerFactory.getLogger(DataStore.class);

	public static DataStore get(String impl) {
		try {
			return get(DataStoreImpl.valueOf(impl));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}
	
	public static DataStore get(String impl, String[] columnFamilies) {
		try {
			return get(DataStoreImpl.valueOf(impl),columnFamilies);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public static DataStore get(DataStoreImpl impl , String...columnFamilies) {
		switch (impl) {
		case LOCAL_FRAME_STORE:
			return new LocalFrameStore(columnFamilies);
		}
		return null;
	}

	enum DataStoreImpl {
		LOCAL_FRAME_STORE
	}

}
