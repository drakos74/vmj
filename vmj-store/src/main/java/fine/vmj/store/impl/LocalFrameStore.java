package fine.vmj.store.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.helper.FileIterator;
import fine.vmj.store.DataStore;
import fine.vmj.store.DataStoreException;
import fine.vmj.store.data.ByteFrame;
import fine.vmj.store.data.Frame;
import fine.vmj.store.data.IFrame;
import fine.vmj.store.data.IFrame.Category;
import fine.vmj.util.VMJProperties;

// TODO : rename to FrameBuffer ... 
public class LocalFrameStore extends DataStore<Frame, List<Frame>> {

	private static final Logger log = LoggerFactory.getLogger(LocalFrameStore.class);

	protected final List<Frame>[] localStorage = new List[fine.vmj.util.VMJProperties.CYCLE_SIZE.getValue()];

	protected final Semaphore[] locks = new Semaphore[9];

	protected final Map<IFrame.Category, List<Frame>> feeds = new ConcurrentHashMap<IFrame.Category, List<Frame>>();

	private AtomicInteger iter = new AtomicInteger(0);

	public LocalFrameStore(String... columnFamilies) {
		super(columnFamilies);
		IntStream.range(0, locks.length).forEach(i -> locks[i] = new Semaphore(1));
	}

	// creates a cover frame representing the feed ...
	private Frame createCover(IFrame.Category cat, List<Frame> frames) {
		// TODO : combine all frames ...
		// return frames
		// .stream()
		// .map(frame -> frame.getBytes())
		// .collect(Averager.Frame::new, Averager.Frame::accept,
		// Averager.Frame::combine);
		// for now take first ...
		return frames.get(0);
		// return cat + "_" + (new Date().getTime());
	}

	private void flush(IFrame.Category cat) {
		try {
			Frame cover = createCover(cat, feeds.get(cat));
			put(cover, feeds.get(cat));
		} catch (DataStoreException e) {
			log.error("Could not flush frame list to store ... " + e.getMessage() + "feed : " + cat + "["
					+ feeds.get(cat) + "]", e);
		} finally {
			if (logOn)
				log.info("clear for " + cat);
			feeds.remove(cat);
		}
	}

	public void cache(final ByteFrame frame, final int batch_size, final int index) {
		// my generations lock
		if(logOn)log.info("cache ... "+frame+" of "+batch_size+" at "+index);
		final int mlock = index % locks.length;
		try {
			if (frame.getIndex() == 0) {
				// get my generations lock ...
				log.info(mlock + "|" + frame.getIndex() + " - get generations[" + index + "] lock[" + (mlock) + "] of "
						+ (locks.length - 1));
				locks[mlock].acquire();
			}

			List<Frame> frames = localStorage[index];

			if (frames == null) {
				if (logOn)
					log.info(mlock + "|" + "no_current frames in cache... create frame container at index:" + index);
				localStorage[index] = new ArrayList<Frame>();
			}

			localStorage[index].add(frame);

			if (logOn)
				log.info(mlock + "|" + "localStorage[length:" + localStorage.length + "] - current_index:" + index
						+ " - current_size:" + localStorage[index].size() + "]");
			if (logOn)
				log.info(mlock + "|" + "feeds[feeds:" + feeds.size() + "]");

			// categorize .. asynchronously
			if (logOn)
				log.info(mlock + "|" + "frame[" + frame.getIndex() + " - " + (batch_size - 1) + "]");
			// if it s last frame of the batch ...
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			if (logOn)
				log.info(mlock + "|" + "release [w:" + mlock + "]");
			locks[mlock].release();
		}

		// execute by the end of the generation ...
		if (frame.getIndex() == batch_size - 1) {
			log.info(mlock + "|" + "batch conditions met at " + (batch_size - 1) + " == " + frame.getIndex());

			Runnable categorize = () -> {
				// previous generation lock
				try {
				int plock = (index - 1) % locks.length;
					if (logOn)
						log.info(mlock + "|" + "will categorize .. frames of generation[" + index + "] with lock["
								+ plock + "]");
					locks[plock].acquire();
					if (logOn)
						log.info(mlock + "|" + "categorize .. frames of generation[" + index + "] now with lock["
								+ plock + "]");
					final List<Frame> prev_frames = localStorage[index - 1];
					final List<Frame> new_frames = localStorage[index];

					if (logOn)
						log.info(mlock + "|" + "last_frames:" + (prev_frames != null ? prev_frames.size() : "null"));
					if (logOn)
						log.info(mlock + "|" + "new_frames:" + (new_frames != null ? new_frames.size() : "null"));

					if (logOn)
						log.info(mlock + "|" + "re-evalutae localStorage for geenration " + mlock);
					localStorage[index] = new ArrayList<Frame>();

					if (prev_frames == null) {
						// TODO : call on flush
						// assign to new categories ...
						if (logOn)
							log.info(mlock + "|" + "no previous data. Will assign new categories ... ");
						for (Frame _frame : new_frames) {
							startNewCategory(_frame);
							if (logOn)
								log.info(mlock + "|" + " add to localStorage " + _frame);
							localStorage[index].add(_frame);
						}
						localStorage[index - 1] = null;
						locks[mlock].release();
						locks[plock].release();
						if(logOn)log.info(mlock + "|" + "categorization initialization complete for generation " + index
								+ " release locks[w:" + mlock + ",r:" + plock + "]");
						return;
					}

					Boolean[] olds = new Boolean[prev_frames.size()];
					Arrays.fill(olds, Boolean.FALSE);
					Boolean[] news = new Boolean[new_frames.size()];
					Arrays.fill(news, Boolean.FALSE);

					int i = 0;
					prev_frames: for (Frame prev_frame : prev_frames) {
						int j = 0;
						new_frames: for (Frame new_frame : new_frames) {
							log.info(mlock + "|" + "loop all for [" + i + "-" + j + "]");
							if (olds[i] == true) {
								if (logOn)
									log.info(mlock + "|" + " old[" + i + "] is already mathed!");
								i++;
								continue prev_frames;
							}
							if (news[j] == true) {
								if (logOn)
									log.info(mlock + "|" + " new[" + j + "] is already mathed! skipping check for old["
											+ i + "]");
								j++;
								continue new_frames;
							}
							// TODO : improve comparison ... rects can change a
							// bit ...
							if (prev_frame.getRect().equals(new_frame.getRect())) {
								if (logOn)
									log.info(mlock + "|" + "frame match ... [" + i + "," + j + ",[" + prev_frame.cat
											+ "," + new_frame.cat + "]] will categorize as " + prev_frame.cat);

								// overwrite category with one from prev
								new_frame.cat = prev_frame.cat;
								// if and only if we have category (frame index
								// already categorized ... )
								// it might be extra new object detected ...
								feeds.compute(prev_frame.cat, (cat, list) -> addToFeeds(cat, list, new_frame));
								if (logOn)
									log.info(mlock + "|" + " add to localStorage " + new_frame);
								localStorage[index].add(new_frame);
								olds[i] = true;
								news[j] = true;
							} else {
								if (logOn)
									log.info(mlock + "|" + "no_match ... [" + i + "," + j + ",[" + prev_frame.cat + ","
											+ new_frame.cat + "]]\n" + "prev_frame:" + prev_frame + "\n" + "new_frame:"
											+ new_frame);
							}
							j++;
						}
						i++;
					}

					if (logOn)
						log.info(mlock + "|" + "checked  all previous [" + i + "]");

					if (logOn)
						log.info("sumup results for previous generation");
					for (int o = 0; o < olds.length; o++) {
						if (olds[o] != true) {
							if (logOn)
								log.info(mlock + "|" + "old " + o + " did not match ... will flush ... "
										+ prev_frames.get(o));
							flush(prev_frames.get(o).cat);
						} else {
							if (logOn)
								log.info(mlock + "|" + "old " + o + " did match ... ");
						}
					}

					if (logOn)
						log.info(mlock + "|" + "sumup results for new generation");
					for (int n = 0; n < news.length; n++) {
						if (news[n] != true) {
							if (logOn)
								log.info(mlock + "|" + "new " + n
										+ " did not match ... will assign category and continue ... ");
							startNewCategory(new_frames.get(n));
							addToCache(new_frames.get(n), index);
						} else {
							if(logOn)log.info(mlock + "|" + "new " + n + " did match ... ");
						}
					}
				} catch (Throwable e) {
					log.error(e.getMessage(), e);
				} finally {
					if(logOn)log.info(mlock + "|" + "categorization complete for generation " + index + " release locks[w:"
							+ mlock + ",r:" + plock + "]");
					localStorage[index - 1] = null;
					locks[mlock].release();
					locks[plock].release();
				}

			};

			executor.submit(categorize);
		} else {
			if(logOn)log.info(mlock + "|" + "No batch conditions met ... " + (batch_size - 1) + " vs. " + frame.getIndex());
		}

	}

	private void startNewCategory(final Frame frame) {
		IFrame.Category category = frame.cat;
		if (feeds.containsKey(frame.cat)) {
			frame.cat = category.getNext();
			if (logOn)
				log.info("try to assign category " + frame.cat);
			if (frame.cat == null) {
				if (logOn)
					log.info("Missed frame ... " + frame);
				return;
			}
			startNewCategory(frame);
			return;
		}
		feeds.compute(category, (cat, list) -> addToFeeds(cat, list, frame));
		if (logOn)
			log.info("assigned frame [" + frame + "] to category " + category);
	}

	private void addToCache(Frame frame, int index) {
		if (logOn)
			log.info(" add to localStorage " + frame + " at " + index);
		localStorage[index].add(frame);
	}

	private List<Frame> addToFeeds(IFrame.Category cat, List<Frame> list, Frame frame) {
		if (list == null) {
			if (logOn)
				log.info("encountered empty cat " + cat);
			list = new ArrayList<Frame>();
		}
		list.add(frame);
		return list;
	}

	// DEBUG methods

	public String peekLocalStorage() {
		return Arrays.toString(IntStream.range(0, localStorage.length).filter(i -> localStorage[i] != null)
				.map(i -> localStorage[i].size()).toArray());
	}

	public String peekFeeds() {
		return Arrays.toString(feeds.entrySet().stream()
				.map(entry -> entry.getKey().toString() + ":" + entry.getValue().size()).toArray());
	}

	public Map<Category, List<Frame>> getFeeds() {
		return feeds;
	}

	@Override
	protected void external_put(byte[] key, byte[] value, String... columnFamilies) throws DataStoreException {

		StringBuilder path = new StringBuilder();

		path.append(String.join("/", columnFamilies));
		LocalDateTime date = LocalDateTime.now();
		int time = Integer.valueOf(date.getYear() + "" + date.getDayOfYear());
		path.append((columnFamilies.length > 0 ? "/" : "") + String.valueOf(time));

		String dir = path.toString();
		if (!Files.exists(Paths.get(dir))) {
			log.info("dir does not exist ... will create");
			try {
				Files.createDirectory(Paths.get(dir));
			} catch (IOException e) {
				throw new DataStoreException("Could not create path " + Paths.get(dir), e);
			}
		}

		path.append("/_" + iter.getAndIncrement());

		try {
			File file = new File(path.toString() + "." + VMJProperties.DATASTORE_LOCAL_FILES_EXT.getValue());
			FileOutputStream f = new FileOutputStream(file, false);
			ObjectOutputStream o = new ObjectOutputStream(f);

			log.info("going to write to file ... " + file);

			// Write objects to file
			o.writeObject(key);
			o.writeObject(value);

			o.close();
			f.close();

			log.info("file has been written ... " + file.getAbsolutePath());

			log.info("verify file will be skipped ... ");

			// try (FileInputStream fi = new FileInputStream(file)) {
			// try (ObjectInputStream oi = new ObjectInputStream(fi);) {
			// // Read objects
			// byte[] __key = (byte[]) oi.readObject();
			// Frame _key = keySerializer.deserialize(__key);
			//// List<ByteFrame> _value = (List<ByteFrame>) oi.readObject();//
			// valueSerializer.deserialize(oi);
			//
			// // Frame k = keySerializer.deserialize(key);
			// // List<Frame> v =
			// // valueSerializer.deserialize(value);
			//
			// log.info(_key.toString());
			// log.info("file verified ... data seems fine");
			//
			//
			// } catch (ClassNotFoundException e) {
			// log.error(e.getMessage(), e);
			// }
			// } catch (IOException e) {
			// log.error(e.getMessage(), e);
			// }

			// TODO : convert to img ... and save as .jpg
		} catch (Exception e) {
			throw new DataStoreException("Could not complete external_put for " + key, e);
		}

	}

	@Override
	protected byte[] external_get(byte[] key, String... columnFamilies) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Set<fine.vmj.store.DataStore.ByteArray> external_get_keys() {
		return null;
	}

	@Override
	protected boolean external_clear(byte[] key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected Map<Frame, List<Frame>> external_load() {

		Map<Frame, List<Frame>> feeds = new ConcurrentHashMap<Frame, List<Frame>>();
		
		String path =  "/"+String.join("/", columns);
			log.info("external load from path : "+path);
		new FileIterator("/"+String.join("/", columns), VMJProperties.DATASTORE_LOCAL_FILES_EXT.getValue()).apply((File file, int i) -> {
					// Read objects
					try {
						try (FileInputStream fi = new FileInputStream(file)) {
							try (ObjectInputStream io = new ObjectInputStream(fi);) {
								Frame key = keySerializer.deserialize((byte[]) io.readObject());
								key.setName(file.getName().toString());
								List<Frame> value = valueSerializer.deserialize((byte[]) io.readObject());
								feeds.put(key, value);
							}
						} catch (FileNotFoundException e) {
							log.info(e.getMessage(), e);
						} catch (IOException e) {
							log.info(e.getMessage(), e);
						}

					} catch (Exception e) {
						throw new RuntimeException("Could not deserialize objects", e);
					}
				});

		return feeds;
	}
}
