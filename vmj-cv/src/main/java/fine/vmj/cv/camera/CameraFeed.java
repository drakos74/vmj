package fine.vmj.cv.camera;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.cv.camera.effect.EffectUtil;
import fine.vmj.cv.classifier.Classifier;
import fine.vmj.cv.util.CVUtils;
import fine.vmj.store.DataStoreFactory;
import fine.vmj.store.data.ByteFrame;
import fine.vmj.store.data.Frame;
import fine.vmj.store.impl.LocalFrameStore;
import fine.vmj.util.VMJProperties;

public class CameraFeed extends VisualProcessor {

	private static final Logger log = LoggerFactory.getLogger(CameraFeed.class);

	private static boolean logOn = fine.vmj.util.VMJProperties.CAMERA_LOG_ON.getValue();

	private static int greyBlurKernel_default = 3;
	private static int colorBlurKernel_default = 7;
	private static int cannyKernelRatio_default = 3;
	private static int dilateKernel_default = 32;
	private static int erodeKernel_default = 12;

	// rolling frame buffer ...
	// TODO : make queue ...
	final Mat[] frameBuffer;

	// configuration
	Configuration config;

	// features
	Features features;

	Mat currentFrame = new Mat();
	Mat grayFrame = new Mat();
	Mat grayBlurFrame = new Mat();
	Mat edgeFrame = new Mat();
	Mat colorBlurFrame = new Mat();
	Mat hsvFrame = new Mat();
	Mat maskFrame = new Mat();
	Mat morphFrame = new Mat();
	Mat thresholdFrame = new Mat();
	Mat thresholdImg = new Mat();

	Mat processedFrame = new Mat();
	Mat demoFrame = new Mat();

	FrameSpec spec = new FrameSpec();

	public LocalFrameStore frameStore = (LocalFrameStore) DataStoreFactory.get(VMJProperties.DATASTORE_IMPL.getValue(),
			new String[] { "store", "demo", "201764" });

	private final int max_count;
	private int count;

	@Deprecated
	public CameraFeed(int max_count) {
		super(Configuration.createNew().classifiers);
		this.max_count = max_count;
		frameBuffer = new Mat[max_count];
		this.config = Configuration.createNew();
	}

	public CameraFeed(int max_count, Configuration config) {
		super(config.classifiers);
		this.max_count = max_count;
		frameBuffer = new Mat[max_count];
		this.config = config;
	}

	public CameraFeed setConfig(Configuration config) {
		this.config = config;
		return this;
	}

	public void start() {
		log.info("camera feed started!");
	}

	public void stop() {
		log.info("camera feed stopped!");
	}

	public void add(Mat frame, int count) {
		// be careful ... count resets at max_count=counter_limit!
		this.count = count;
		frameBuffer[count] = frame;
		currentFrame = frame;
		// find previous frame
		Mat prevFrame = count == 0 ? frameBuffer[max_count - 1] : frameBuffer[count - 1];

	}

	public Mat getHistogram() {
		return FrameTransform.getHistogram(currentFrame, false);
	}

	/**
	 * detect edges ... create cache of greyScale image
	 */
	public Mat edgeDetect(double threshold) {

		// init
		Mat detectedEdges = new Mat();

		// convert to grayscale
		Imgproc.cvtColor(currentFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);

		// System.out.println(config);

		// reduce noise with a 3x3 kernel
		Imgproc.blur(grayFrame, grayBlurFrame, new Size(config.greyBlurKernel, config.greyBlurKernel));

		grayBlurFrame.copyTo(detectedEdges);

		// canny detector, with ratio of lower:upper threshold of 3:1
		Imgproc.Canny(detectedEdges, detectedEdges, threshold, threshold * config.cannyKernelRatio);
		edgeFrame = new Mat();
		// using Canny's output as a mask, display the result
		currentFrame.copyTo(edgeFrame, detectedEdges);

		return edgeFrame;
	}

	public List<Frame> objDetect() {

		List<Rect> _rects = detectAt(grayBlurFrame, count);

		if (logOn)
			log.info("ALL_RECTS:" + _rects.size());

		TreeMap<Double, Integer> ratios = new TreeMap<Double, Integer>();
		double mean = 0;

		// edgeImage.
		int i = 0;
		
		for (Rect rect : _rects) {
			Mat subframe = edgeFrame.submat(rect);
			Mat m = new Mat();
			Core.extractChannel(subframe, m, 0);
			int n = Core.countNonZero(m);
			double _ratio = (n / rect.area());
			// promote biger rects .... !!!
			double ratio = _ratio * Math.sqrt(rect.width * rect.height);
			ratios.put(1 / ratio, i);
			mean += ratio;
			if (logOn)
				log.info("i:" + i + "[" + n + "," + rect.size() + " -> " + ratio + " (" + _ratio + ") ]");
			i++;
		}

		// find mean
		final double _mean = mean / (i);
		// find closest to mean ...

		List<Rect> rects = new ArrayList<Rect>();
		List<Frame> frames = new ArrayList<Frame>();

		// create processedFrame from rects ...
		// TODO : create hash for each frame ... 
		try {
			features = new Features(ratios.size() / 2);
			processedFrame = new Mat(currentFrame.size(), currentFrame.type(), new Scalar(0, 0, 0));
			int threshold = ratios.size() / 2;
			for (int l = 0; l < threshold; l++) {
				Rect _rect = _rects.get(l);
				rects.add(_rect);
				// TODO : blow up rect !
				// TODO : smooth edges !
				Mat subMat = currentFrame.submat(_rect);
				EffectUtil.applyCopy(processedFrame, subMat, _rect);
				features.add(subMat, _rect, l);
				Frame frame = CVUtils.convertToByteFrame(subMat, _rect, l);
				frames.add(frame);
				frameStore.cache(CVUtils.convertToByteFrame(subMat, _rect, l), threshold, count);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// create demo frame to play with effects ...
		currentFrame.copyTo(demoFrame);
		try {

			EffectUtil.applyAddWeight(demoFrame, processedFrame);

			// Mat _colorBlurFrame = new Mat();
			// Mat _hsvFrame = new Mat();
			// Mat _maskFrame = new Mat();
			//
			// // blur the colored frame
			// Imgproc.blur(processedFrame, _colorBlurFrame, new
			// Size(config.colorBlurKernel, config.colorBlurKernel));
			// // convert the frame to HSV
			// Imgproc.cvtColor(_colorBlurFrame, _hsvFrame,
			// Imgproc.COLOR_BGR2HSV);
			// Core.inRange(_hsvFrame, config.min_values, config.max_values,
			// _maskFrame);
			// Mat dilateElement =
			// Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
			// new Size(config.dilateKernel, config.dilateKernel));
			// Mat erodeElement =
			// Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
			// new Size(config.erodeKernel, config.erodeKernel));
			//
			// Imgproc.erode(_maskFrame, demoFrame, erodeElement);
			//
			// Imgproc.dilate(_maskFrame, demoFrame, dilateElement);
			// Core.normalize(demoFrame, demoFrame);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (logOn)
			log.info("FILTERED_RECTS:" + rects.size());

		return frames;

	}

	public void hsv() {
		// blur the colored frame
		Imgproc.blur(currentFrame, colorBlurFrame, new Size(config.colorBlurKernel, config.colorBlurKernel));
		// convert the frame to HSV
		Imgproc.cvtColor(colorBlurFrame, hsvFrame, Imgproc.COLOR_BGR2HSV);
	}

	public void mask(Scalar minValues, Scalar maxValues) {
		Core.inRange(hsvFrame, minValues, maxValues, maskFrame);
	}

	public Mat morph() {

		Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
				new Size(config.dilateKernel, config.dilateKernel));
		Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
				new Size(config.erodeKernel, config.erodeKernel));

		Imgproc.erode(maskFrame, morphFrame, erodeElement);

		Imgproc.dilate(maskFrame, morphFrame, dilateElement);

		return morphFrame;
	}

	public void threshold(boolean inverse) {
		Mat hsvImg = new Mat();
		List<Mat> hsvPlanes = new ArrayList<>();

		int thresh_type = Imgproc.THRESH_BINARY_INV;
		if (inverse)
			thresh_type = Imgproc.THRESH_BINARY;

		// threshold the image with the average hue value
		hsvImg.create(currentFrame.size(), CvType.CV_8U);
		Imgproc.cvtColor(currentFrame, hsvImg, Imgproc.COLOR_BGR2HSV);
		Core.split(hsvImg, hsvPlanes);

		// get the average hue value of the image
		double threshValue = FrameSpace.getHistAverage(hsvImg, hsvPlanes.get(0));

		Imgproc.threshold(hsvPlanes.get(0), thresholdImg, threshValue, 179.0, thresh_type);

		Imgproc.blur(thresholdImg, thresholdImg, new Size(5, 5));

	}

	public void pre_process(double a, double b, double c, double g) {

		currentFrame.convertTo(currentFrame, -1, b, c);
	}

	public Mat process(Mat frame) {
		return frame;
	}

	public static class Configuration {
		final Classifier.Config[] classifiers;
		int greyBlurKernel;
		int colorBlurKernel;
		int dilateKernel;
		int erodeKernel;

		int cannyKernelRatio;

		Scalar min_values = new Scalar(0, 0, 0);
		Scalar max_values = new Scalar(0, 0, 0);

		private Configuration(int greyBlurKernel, int colorBlurKernel, int cannyKernelRatio, int dilateKernel,
				int erodeKernel, Classifier.Config[] classifiers) {
			this.greyBlurKernel = greyBlurKernel;
			this.colorBlurKernel = colorBlurKernel;
			this.dilateKernel = dilateKernel;
			this.erodeKernel = erodeKernel;

			this.cannyKernelRatio = cannyKernelRatio;
			this.classifiers = classifiers;
		};

		public static Configuration createNew() {
			return new Configuration(greyBlurKernel_default, colorBlurKernel_default, cannyKernelRatio_default,
					dilateKernel_default, erodeKernel_default, Arrays.stream(Classifier.Type.values()).map(cl -> {
						Classifier.Config config = new Classifier.Config(cl);
						config.initMinSize(-1);
						config.initMaxSize(-1);
						config.initScaleStep(FrameSpace.scale_factor);
						config.initMinNeighbours(FrameSpace.min_neighbours);
						return config;
					}).collect(Collectors.toList()).toArray(new Classifier.Config[Classifier.Type.values().length]));
		}

		public void adjustGreyBlurKernel(double size) {
			this.greyBlurKernel = new Double(size).intValue();
		}

		public void adjustCannyKernelRatio(double ratio) {
			this.cannyKernelRatio = new Double(ratio).intValue();
		}

		public void adjustColorBlurKernel(double size) {
			this.colorBlurKernel = new Double(size).intValue();
		}

		public void adjustDilateKernel(double size) {
			this.dilateKernel = new Double(size).intValue();
		}

		public void adjustErodeKernel(double ratio) {
			this.erodeKernel = new Double(ratio).intValue();
		}

		public void adjustMinValues(Scalar minValues) {
			this.min_values = minValues;
		}

		public void adjustMaxValues(Scalar maxValues) {
			this.max_values = maxValues;
		}

		@Override
		public String toString() {
			StringBuilder value = new StringBuilder();
			value.append("this.greyBlurKernel = " + greyBlurKernel);
			value.append("\n");
			value.append("this.colorBlurKernel = " + colorBlurKernel);
			value.append("\n");
			value.append("this.dilateKernel = " + dilateKernel);
			value.append("\n");
			value.append("this.erodeKernel = " + erodeKernel);
			value.append("\n");
			value.append("this.cannyKernelRatio = " + cannyKernelRatio);
			value.append("\n");
			value.append("this.classifiers = " + classifiers.length);
			return value.toString();
		}

	}

	public static class Features {

		public final int obj_count;

		Mat[] frames;
		Rect[] rects;

		private Features(int obj_count) {
			this.obj_count = obj_count;
			frames = new Mat[obj_count];
			rects = new Rect[obj_count];
		}

		public void add(Mat frame, Rect rect, int index) {
			frames[index] = frame;
			rects[index] = rect;
		}

	}

	// GETTERS

	public Mat greyed() {
		return grayFrame;
	}

	public Mat greyBlurred() {
		return grayBlurFrame;
	}

	public Mat colorBlurred() {
		return colorBlurFrame;
	}

	public Mat hsved() {
		return hsvFrame;
	}

	public Mat masked() {
		return maskFrame;
	}

	public Mat morphed() {
		return morphFrame;
	}

	public Mat thresholded() {
		return thresholdFrame;
	}

	public Mat thresholdImg() {
		return thresholdImg;
	}

	public Mat processed() {
		return processedFrame;
	}

	public Mat demo() {
		return demoFrame;
	}

	// Frame Spec class
	public static class FrameSpec {

	}

}
