package fine.vmj.ml.simple;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bytedeco.javacpp.opencv_core.Rect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.helper.FileIterator;
import fine.vmj.store.DataStore;
import fine.vmj.store.DataStoreException;
import fine.vmj.store.data.ByteFrame;
import fine.vmj.store.data.Frame;
import fine.vmj.store.impl.LocalFrameStore;

public class FrameMatcher {

	private static final Logger log = LoggerFactory.getLogger(FrameMatcher.class);

	private static final boolean logOn = fine.vmj.util.VMJProperties.MATCHER_LOG_ON.getValue();

	private final DataStore<Frame, List<Frame>> dataStore;

	private Map<Frame, List<Frame>> frames;

	private Map<Frame, Condition> matches = new HashMap<Frame, Condition>();

	public FrameMatcher() {

		// get the DataStore instance
		dataStore = new LocalFrameStore("Users", "drakos", "Projects", "dr-eek", "free-servers", "selfie", "VMJ",
				"vmj-fx", "store", "demo", "201764");

		try {
			frames = dataStore.load();
		} catch (DataStoreException e) {
			log.error(e.getMessage(), e);
		}
	}

	// TODO : synchornize ... this must be applied to all frames before we call
	// replace

	public ByteFrame anyMatch(final Frame frame, int c) {
		// TODO : merge with replace method ...
		if (frames != null) {
			if (matches.containsKey(frame)) {
				if (logOn)
					log.info("is actively being tracked " + frame);
				if (!matches.get(frame).isLocked()) {
					if (logOn)
						log.info("lock is open ... ");
					matches.get(frame).lock(true);
					return (ByteFrame) matches.get(frame).frame;
				}
			}
			if (logOn)
				log.info("not being tracked ... search for match ... ");
			Set<Frame> keys = frames.keySet();
			return (ByteFrame) keys.stream()
					// .filter(fr -> {
					// // Assume match any ...
					// if (fr.equals(frame)) {
					// // store in matches
					// matches.put(frame, new Condition(fr,
					// frames.get(fr).size()));
					// return true;
					// }
					// return false;
					// })
					.map(fr -> {
						matches.put(frame, new Condition(fr, frames.get(fr).size()));
						return fr;
					}).findAny().orElse(null);
		}
		return null;
	}

	public ByteFrame replace(final Frame keyframe, final Frame referenceFrame, int c) {
		List<Frame> _frames = frames.get(keyframe);
		if (logOn)
			log.info("retrieveFrame:" + keyframe);
		if (_frames == null) {
			// ... :(
		}
		int _index = c % _frames.size();
		Frame this_frame = _frames.get(_index);
		int life = matches.get(referenceFrame).range;
		if (logOn)
			log.info("unlock the reference and reduce range [" + life + "] ... " + referenceFrame);
		if (life > 0) {
			matches.get(referenceFrame).lock(false);
			matches.get(referenceFrame).range--;
		} else {
			matches.remove(referenceFrame);
			return null;
		}
		if (this_frame == null) {
			throw new RuntimeException(
					"Cannot get matching frame ... at [" + c + " % " + _frames.size() + " = " + _index + "]");
		}
		return (ByteFrame) this_frame;
	}

	private static class Condition {

		private int range = -1;
		private boolean lock = false;
		private final Frame frame;

		Condition(Frame frame, int range) {
			this.range = range;
			this.frame = frame;
			// lock when initialized
			this.lock = true;
		}

		void lock(boolean lock) {
			this.lock = lock;
		}

		boolean isLocked() {
			return lock;
		}

	}

}
