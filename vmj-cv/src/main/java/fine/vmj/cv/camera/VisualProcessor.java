package fine.vmj.cv.camera;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.cv.classifier.Classifier;

public abstract class VisualProcessor {

	static final Logger log = LoggerFactory.getLogger(VisualProcessor.class);

	static boolean logOn = fine.vmj.util.VMJProperties.OBJDETECT_LOG_ON.getValue();

	private final Classifier.Config[] classifiers;

	private final boolean doAll = false;

	private final boolean isTest = fine.vmj.util.VMJProperties.OBJDETECT_IS_TEST.getValue();

	private final Map<Classifier.Type, Classifier.Status> rects = new HashMap<Classifier.Type, Classifier.Status>();

	private final List<Rect> random_rects;

	VisualProcessor(Classifier.Config[] classifiers) {
		this.classifiers = classifiers;
		Arrays.stream(classifiers).forEach(cl -> rects.put(cl.type, new Classifier.Status()));

		// generate random rects ...
		Mat frame = new Mat(720,1280,3);
		int num_rects = ThreadLocalRandom.current().nextInt(5, 11);
		log.info("generate some random rects ... " + num_rects);
		random_rects = IntStream.range(0, num_rects).mapToObj(i -> {
			Rect rect = new Rect(new double[] {
					ThreadLocalRandom.current().nextDouble((double) frame.size().width / 3,
							(double) frame.size().width - frame.size().width / 3),
					ThreadLocalRandom.current().nextDouble((double) frame.size().height / 3,
							(double) frame.size().height - frame.size().height / 3),
					ThreadLocalRandom.current().nextDouble((double) frame.size().width / 12,
							(double) frame.size().width / 8),
					ThreadLocalRandom.current().nextDouble((double) frame.size().width / 12,
							(double) frame.size().width / 8) });
			log.info("auto-detect : " + rect);
			return rect;
		}).collect(Collectors.toList());

	}

	List<Rect> detectAt(Mat frame, int count) {

		// TODO : configurable , how soon objects get detected ...

		if (isTest) {
			return random_rects;
		}

		if (doAll) {
			List<Rect> rects = new ArrayList<Rect>();
			if (logOn)
				log.info("use all Classifiers :" + classifiers.length);
			for (Classifier.Config classifier : classifiers) {
				if (logOn)
					log.info("Classifier:" + classifier.type);
				try {
					rects.addAll(Arrays.asList(FrameSpace.detectWithClassifier(frame, classifier)));
				} catch (Exception e) {
					if (logOn)
						log.warn("Classifier application failed for " + classifier, e);
				}
			}

			return rects;
		} else {
			// use only one classifier each cycle
			Classifier.Config classifier = classifiers[count % classifiers.length];

			try {
				Classifier.Status status = rects.get(classifier.type);
				if (logOn)
					log.info("Classifier:" + classifier.type);
				if (!status.isSleeping()) {
					Rect[] _rects = FrameSpace.detectWithClassifier(frame, classifier);
					if (logOn)
						log.info("Rects:" + _rects.length);
					status.update(_rects);
				} else {
					// skipping this one ...
					if (logOn)
						log.info(classifier.type + " is sleeping ...");
				}
			} catch (Exception e) {
				if (logOn)
					log.warn("Classifier application failed for " + classifier, e);
			}

			// return all rects ...
			return rects.values().stream().flatMap(rect -> rect.getRects().stream()).collect(Collectors.toList());// aggregate
																													// all
		}

	}

}
