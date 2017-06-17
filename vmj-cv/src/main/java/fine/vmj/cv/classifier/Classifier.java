package fine.vmj.cv.classifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import org.opencv.core.Rect;
import org.opencv.objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.cv.camera.CameraFeed;
import fine.vmj.cv.util.Rects;
import fine.vmj.util.VMJProperties;

public class Classifier {

	private static final Logger log = LoggerFactory.getLogger(Classifier.class);

	public static final boolean logOn = fine.vmj.util.VMJProperties.CLASSIFIER_LOG_ON.getValue();
	public static final int logFrequency = fine.vmj.util.VMJProperties.CLASSIFIER_LOG_FREQUENCY.getValue();

	public static final int cycle_time = VMJProperties.CLASSIFIER_CYCLE_TIME.getValue();
	public static final int calibration_time = VMJProperties.CLASSIFIER_CALIBRATION_TIME.getValue();

	final static ClassLoader classLoader = Classifier.class.getClassLoader();

	private static Map<Type, CascadeClassifier> cascades = new HashMap<Type, CascadeClassifier>();

	static {

		try {
			cascades.put(Type.FRONTAL_FACE, new CascadeClassifier());
			cascades.put(Type.FRONTAL_FACE_ALT, new CascadeClassifier());

			for (Type type : Type.values()) {
				CascadeClassifier this_classifier = new CascadeClassifier();
				this_classifier.load(classLoader.getResource(type.getPath()).getPath());
				cascades.put(type, this_classifier);
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			System.out.println("Could not init class " + Classifier.class.getSimpleName());
		}

	}

	public static CascadeClassifier get(Type classifier) {
		return cascades.get(classifier);
	}

	public enum Type {
		EYE("haarcascades/haarcascade_eye.xml"), FRONTAL_FACE_ALT_TREE(
				"haarcascades/haarcascade_frontalface_alt_tree.xml"), FRONTAL_FACE_ALT(
						"haarcascades/haarcascade_frontalface_alt.xml"), FRONTAL_FACE_ALT2(
								"haarcascades/haarcascade_frontalface_alt2.xml"), FRONTAL_FACE(
										"haarcascades/haarcascade_frontalface_default.xml"), BODY(
												"haarcascades/haarcascade_fullbody.xml"), LOWER_BODY(
														"haarcascades/haarcascade_lowerbody.xml"), EYES(
																"haarcascades/haarcascade_mcs_eyepair_big.xml"), EYES_SMALL(
																		"haarcascades/haarcascade_mcs_eyepair_small.xml"), MOUTH(
																				"haarcascades/haarcascade_mcs_mouth.xml"), EYE_LEFT(
																						"haarcascades/haarcascade_mcs_lefteye.xml"), EYE_RIGHT(
																								"haarcascades/haarcascade_mcs_righteye.xml"), PROFILE_FACE(
																										"haarcascades/haarcascade_profileface.xml"), UPPER_BODY(
																												"haarcascades/haarcascade_mcs_upperbody.xml"), UPPER_BODY2(
																														"haarcascades/haarcascade_upperbody.xml"), SMILE(
																																"haarcascades/haarcascade_smile.xml");

		private final String path;

		private Type(String path) {
			this.path = path;
		}

		public String getPath() {
			return this.path;
		}
	}

	public static class Config {

		public int min_size;
		public int max_size;
		public double scale_step;
		public int min_neighb;
		public final Classifier.Type type;

		public Config(Type type) {
			this.type = type;
		}

		public void initMinSize(int height) {
			if (Math.round(height * 0.01f) > 0) {
				this.min_size = Math.round(height * 0.01f);
			}
		}

		public void initMaxSize(int height) {
			if (Math.round(height * 0.01f) > 0) {
				this.max_size = Math.round(height * 0.01f);
			}
		}

		public void initScaleStep(double step) {
			this.scale_step = (double) Math.round(step * 100) / 100;
		}

		public void initMinNeighbours(int n) {
			this.min_neighb = n;
		}

		@Override
		public String toString() {
			StringBuilder string = new StringBuilder();
			string.append("[");
			string.append("type=" + this.type);
			string.append(",");
			string.append("min_size=" + this.min_size);
			string.append(",");
			string.append("max_size=" + this.max_size);
			string.append(",");
			string.append("scale=" + this.scale_step);
			string.append(",");
			string.append("neighbours=" + this.min_neighb);
			string.append("]");
			return string.toString();
		}

	}

	// TODO : is pretty lazy ... improve by taking advantage of State
	public static class Status {

		List<Rect> rects = new ArrayList<Rect>();

		boolean isLive = false;
		// start asleep in order to auto-calibrate image ...
		boolean isSleeping = true;

		// keep track of status
		// max count of status before taking a decision
		public static final int max_counter = calibration_time;
		public static final int confidence_threshold = max_counter / 2;
		private int event_counter = 0;

		// max number of sleep/awake cycles
		public static final int reset_counter = cycle_time;
		private int cycle_counter = 0;

		// start from 0TO0 ...
		State state = State._0TO0;

		public Status() {

		}

		// must run before update
		public boolean isSleeping() {
			cycle_counter++;
			if (logOn && cycle_counter % logFrequency == 0)
				log.info("cycle_counter : " + cycle_counter + " of " + reset_counter);
			// enable or disable at threshold ...
			if (cycle_counter > reset_counter) {
				resetCycleCount();
				if (isSleeping) {
					// wake up to get some samples
					isSleeping = false;
					if (logOn)
						System.out.println(
								"max sleep time passed " + reset_counter + " while :" + !isLive + "[->" + isLive + "]");
				}
			}
			if (isSleeping == false) {
				if (logOn && (cycle_counter % logFrequency == 0 || cycle_counter > reset_counter - 2))
					System.out.println("sleeping at " + cycle_counter);
			}
			return isSleeping;
		}

		public boolean isAlive() {
			return isLive;
		}

		// lazy logic to emulate certainty
		public void update(Rect[] _rects) {
			// we want to stabilise # of rects ...
			if(logOn)log.info("start update with "+_rects.length);
			List<Rect> new_rects = Rects.reduce(new ArrayList<Rect>(Arrays.asList(_rects)));

			int new_size = new_rects.size();
			int old_size = rects.size();

			if (new_size == 0) {
				// no elements found
				if (old_size == 0) {
					// consecutive misses ...
					assertState(State._0TO0);
				} else {
					// found nothing this time ...
					assertState(State._1TO0);
					resetRects();
				}
			} else {
				rects = new_rects;
				if (old_size == new_size) {
					assertState(State._1TO1);
				} else if (old_size == 0) {
					// we just found one ... wait
					assertState(State._0TO1);
				} else {
					// we found different number of rects ...
					if (logOn)
						log.info("we just found different number of rects [" + new_size + " vs. " + old_size
								+ "] ... wait for more positives at " + cycle_counter);
					isLive = false;
				}
			}
		}

		private void assertState(State new_state) {
			if (state == new_state) {
				event_counter++;
			} else {
				resetEventCount();
			}
			state = new_state;
			if (logOn)
				log.info("Came at state : " + state + " in " + event_counter + " of " + max_counter);
			if (event_counter > confidence_threshold) {
				if (logOn)
					log.info("encountered state " + state + " for " + max_counter + " times ... will take action ... ");
				// need to take some action ...
				switch (state) {
				case _0TO0:
					isLive = false;
					break;
				case _1TO1:
					isLive = true;
					break;
				case _0TO1:
				case _1TO0:
					throw new IllegalStateException(
							"Unexpected state encountered ... " + state + " while this was also the last one ... ");
				}
				if (logOn)
					log.info("setting live to " + isLive);
				if (event_counter > max_counter) {
					// reset event count ... going to sleep now ... and serve
					// same rect ...
					resetCycleCount();
					isSleeping = true;
				}
			}
		}

		private void resetCycleCount() {
			if (logOn)
				log.info("reset cycle_count from [" + cycle_counter + "]");
			cycle_counter = 0;
			resetEventCount();
		}

		private void resetEventCount() {
			if (logOn)
				log.info("reset event_count from [" + event_counter + "]");
			event_counter = 0;
		}

		private void resetRects() {
			rects = Collections.emptyList();
		}

		public List<Rect> getRects() {
			return isLive ? rects : Collections.emptyList();
		}

		private enum State {
			_0TO1, // found nothing in previous step
			_0TO0, // found still nothing
			_1TO1, // found again
			_1TO0;// found nothing in this step
		}

	}

}
