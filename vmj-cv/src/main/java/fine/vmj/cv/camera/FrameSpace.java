package fine.vmj.cv.camera;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;
import fine.vmj.cv.classifier.Classifier;

public class FrameSpace {

	static final Logger log = LoggerFactory.getLogger(FrameSpace.class);
	
	static boolean logOn = fine.vmj.util.VMJProperties.FRAMESPACE_LOG_ON.getValue();
	
	private static volatile CascadeClassifier faceCascade = new CascadeClassifier();

	private static int absoluteFaceSize = 0;
	// This means that this size of face is detected in the image if present.
	// However, by rescaling the input image, you can resize a larger face to a
	// smaller one, making it detectable by the algorithm.
	public static double scale_factor = 1.1;
	// This parameter will affect the quality of the detected faces. Higher
	// value results in less detections but with higher quality. 3~6 is a good
	// value for it.
	public static int min_neighbours = 3;

	/**
	 * step functions ...
	 */

	public static Mat getRect(Mat frame, Mat overlay) {
		Rect roi = new Rect(frame.cols() - overlay.cols(), frame.rows() - overlay.rows(), overlay.cols(),
				overlay.rows());
		Mat imageROI = frame.submat(roi);
		return imageROI;
	}

	public static Rect getRect(Mat frame, Portion portion) {

		switch (portion) {
		case BOTTOM_RIGHT:
			return new Rect(frame.cols() / 2, frame.rows() / 2, frame.cols() / 2, frame.rows() / 2);
		default:
			return new Rect(0, 0, frame.cols(), frame.rows());
		}

	}

	public static Mat greyScale(Mat frame) {
		Mat grayFrame = new Mat();

		// convert the frame in gray scale
		Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
		// equalize the frame histogram to improve the result
		Imgproc.equalizeHist(grayFrame, grayFrame);
		return grayFrame;
	}

	public static Mat blur(Mat frame, int size) {
		Mat blurredImage = new Mat();
		Imgproc.blur(frame, blurredImage, new Size(size, size));
		return blurredImage;
	}

	public static Mat threshold(Mat frame, boolean inverse) {
		Mat hsvImg = new Mat();
		List<Mat> hsvPlanes = new ArrayList<>();
		Mat thresholdImg = new Mat();

		int thresh_type = Imgproc.THRESH_BINARY_INV;
		if (inverse)
			thresh_type = Imgproc.THRESH_BINARY;

		// threshold the image with the average hue value
		hsvImg.create(frame.size(), CvType.CV_8U);
		Imgproc.cvtColor(frame, hsvImg, Imgproc.COLOR_BGR2HSV);
		Core.split(hsvImg, hsvPlanes);

		// get the average hue value of the image
		double threshValue = getHistAverage(hsvImg, hsvPlanes.get(0));

		Imgproc.threshold(hsvPlanes.get(0), thresholdImg, threshValue, 179.0, thresh_type);

		Imgproc.blur(thresholdImg, thresholdImg, new Size(5, 5));

		return thresholdImg;
	}

	public static Tuple2<Mat, Scalar[]> hsv(Mat blurredFrame, double[] hue, double[] sat, double[] v) {

		Mat hsvImage = new Mat();

		// convert the frame to HSV
		Imgproc.cvtColor(blurredFrame, hsvImage, Imgproc.COLOR_BGR2HSV);

		// get thresholding values from the UI
		// remember: H ranges 0-180, S and V range 0-255
		Scalar minValues = new Scalar(hue[0], sat[0], v[0]);
		Scalar maxValues = new Scalar(hue[1], sat[1], v[1]);

		return new Tuple2<Mat, Scalar[]>(hsvImage, new Scalar[] { minValues, maxValues });
	}

	public static Rect[] detectWithClassifier(Mat grayframe, Classifier.Type classifier, int... size) {

		MatOfRect objs = new MatOfRect();
		Classifier.get(classifier).detectMultiScale(grayframe, objs, scale_factor, min_neighbours,
				0 | Objdetect.CASCADE_SCALE_IMAGE, size.length > 0 ? new Size(size[0], size[0]) : new Size(),
				size.length > 1 ? new Size(size[1], size[1]) : new Size());

		// each rectangle in faces is a face: draw them!
		Rect[] rects = objs.toArray();

		return rects;

	}

	public static Rect[] detectWithClassifier(Mat grayframe, Classifier.Config classifier) {

		if(logOn)log.info("detect_with ... "+classifier+"");
		
		MatOfRect objs = new MatOfRect();
		Classifier.get(classifier.type).detectMultiScale(grayframe, objs, classifier.scale_step, classifier.min_neighb,
				0 | Objdetect.CASCADE_SCALE_IMAGE,
				classifier.min_size > 0 ? new Size(classifier.min_size, classifier.min_size) : new Size(),
				classifier.max_size > 0 ? new Size(classifier.max_size, classifier.max_size) : new Size());

		// each rectangle in faces is a face: draw them!
		Rect[] rects = objs.toArray();

		return rects;

	}

	public static Mat getMaskImage(Mat hsvImage, Scalar minValues, Scalar maxValues) {

		Mat mask = new Mat();
		Core.inRange(hsvImage, minValues, maxValues, mask);
		return mask;

	}

	/**
	 * Effect functions ...
	 */

	/**
	 * Method for face detection and tracking
	 * 
	 * @param frame
	 *            it looks for faces in this frame
	 */
	public static Rect[] faceDetect(Mat frame, boolean debug) {

		Mat grayFrame = greyScale(frame);

		// compute minimum face size (20% of the frame height, in our case)
		if (absoluteFaceSize == 0) {
			int height = grayFrame.rows();
			if (Math.round(height * 0.01f) > 0) {
				absoluteFaceSize = Math.round(height * 0.01f);
			}
		}
		Rect[] facesArray = detectWithClassifier(grayFrame, Classifier.Type.FRONTAL_FACE_ALT, absoluteFaceSize);

		if (debug) {
			for (int i = 0; i < facesArray.length; i++)
				Imgproc.rectangle(grayFrame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0), 3);
		}

		return facesArray;

	}

	public static void loadClassifier(String classifierPath) {
		if(logOn)log.info(FrameSpace.class.getClassLoader().getResource(classifierPath).getPath());
		faceCascade.load(FrameSpace.class.getClassLoader().getResource(classifierPath).getPath());
	}

	/**
	 * Perform the operations needed for removing a uniform background
	 * 
	 * @param frame
	 *            the current frame
	 * @return an image with only foreground objects
	 */
	public static Mat backgroundRemove(Mat frame, boolean inverse) {
		// init
		Mat hsvImg = new Mat();
		List<Mat> hsvPlanes = new ArrayList<>();
		Mat thresholdImg = new Mat();

		int thresh_type = Imgproc.THRESH_BINARY_INV;
		if (inverse)
			thresh_type = Imgproc.THRESH_BINARY;

		// threshold the image with the average hue value
		hsvImg.create(frame.size(), CvType.CV_8U);
		Imgproc.cvtColor(frame, hsvImg, Imgproc.COLOR_BGR2HSV);
		Core.split(hsvImg, hsvPlanes);

		// get the average hue value of the image
		double threshValue = getHistAverage(hsvImg, hsvPlanes.get(0));

		Imgproc.threshold(hsvPlanes.get(0), thresholdImg, threshValue, 179.0, thresh_type);

		Imgproc.blur(thresholdImg, thresholdImg, new Size(5, 5));
		// dilate to fill gaps, erode to smooth edges
		Imgproc.dilate(thresholdImg, thresholdImg, new Mat(), new Point(-1, -1), 1);
		Imgproc.erode(thresholdImg, thresholdImg, new Mat(), new Point(-1, -1), 3);

		Imgproc.threshold(thresholdImg, thresholdImg, threshValue, 179.0, Imgproc.THRESH_BINARY);

		// create the new image
		Mat foreground = new Mat(frame.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
		frame.copyTo(foreground, thresholdImg);

		return foreground;
	}

	/**
	 * Get the average hue value of the image starting from its Hue channel
	 * histogram
	 * 
	 * @param hsvImg
	 *            the current frame in HSV
	 * @param hueValues
	 *            the Hue component of the current frame
	 * @return the average Hue value
	 */
	public static double getHistAverage(Mat hsvImg, Mat hueValues) {
		// init
		double average = 0.0;
		Mat hist_hue = new Mat();
		// 0-180: range of Hue values
		MatOfInt histSize = new MatOfInt(180);
		List<Mat> hue = new ArrayList<>();
		hue.add(hueValues);

		// compute the histogram
		Imgproc.calcHist(hue, new MatOfInt(0), new Mat(), hist_hue, histSize, new MatOfFloat(0, 179));

		// get the average Hue value of the image
		// (sum(bin(h)*h))/(image-height*image-width)
		// -----------------
		// equivalent to get the hue of each pixel in the image, add them, and
		// divide for the image size (height and width)
		for (int h = 0; h < 180; h++) {
			// for each bin, get its value and multiply it for the corresponding
			// hue
			average += (hist_hue.get(h, 0)[0] * h);
		}

		// return the average hue of the image
		return average = average / hsvImg.size().height / hsvImg.size().width;
	}

	/**
	 * Apply Canny
	 * 
	 * @param frame
	 *            the current frame
	 * @return an image elaborated with Canny
	 */
	public static Mat edgeDetect(Mat frame, double threshold) {
		// init
		Mat grayImage = new Mat();
		Mat detectedEdges = new Mat();

		// convert to grayscale
		Imgproc.cvtColor(frame, grayImage, Imgproc.COLOR_BGR2GRAY);

		// reduce noise with a 3x3 kernel
		Imgproc.blur(grayImage, detectedEdges, new Size(3, 3));

		// canny detector, with ratio of lower:upper threshold of 3:1
		Imgproc.Canny(detectedEdges, detectedEdges, threshold, threshold * 3);

		// using Canny's output as a mask, display the result
		Mat dest = new Mat();
		frame.copyTo(dest, detectedEdges);

		return dest;
	}

	public static Tuple2<Mat, Scalar[]> getHSV(Mat frame, double[] hue, double[] sat, double[] v) {
		// init
		Mat blurredImage = new Mat();
		Mat hsvImage = new Mat();
		// remove some noise
		Imgproc.blur(frame, blurredImage, new Size(7, 7));

		// convert the frame to HSV
		Imgproc.cvtColor(blurredImage, hsvImage, Imgproc.COLOR_BGR2HSV);

		// get thresholding values from the UI
		// remember: H ranges 0-180, S and V range 0-255
		Scalar minValues = new Scalar(hue[0], sat[0], v[0]);
		Scalar maxValues = new Scalar(hue[1], sat[1], v[1]);

		return new Tuple2<Mat, Scalar[]>(hsvImage, new Scalar[] { minValues, maxValues });
	}

	public static Mat getMorphImage(Mat mask) {

		Mat morphOutput = new Mat();
		// morphological operators
		// dilate with large element, erode with small ones
		Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(32, 32));
		Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));

		Imgproc.erode(mask, morphOutput, erodeElement);
		Imgproc.erode(mask, morphOutput, erodeElement);

		Imgproc.dilate(mask, morphOutput, dilateElement);
		Imgproc.dilate(mask, morphOutput, dilateElement);

		return morphOutput;

	}

	/**
	 * Given a binary image containing one or more closed surfaces, use it as a
	 * mask to find and highlight the objects contours
	 * 
	 * @param maskedImage
	 *            the binary image to be used as a mask
	 * @param frame
	 *            the original {@link Mat} image to be used for drawing the
	 *            objects contours
	 * @return the {@link Mat} image with the objects contours framed
	 */
	public static Mat findAndDrawBalls(Mat maskedImage, Mat frame) {
		// init
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();

		// find contours
		Imgproc.findContours(maskedImage, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

		// if any contour exist...
		if (hierarchy.size().height > 0 && hierarchy.size().width > 0) {
			// for each contour, display it in blue
			for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
				Imgproc.drawContours(frame, contours, idx, new Scalar(250, 0, 0));
			}
		}

		return frame;
	}

	public static String inspect(Mat frame) {
		String inspection = new String(
				"[" + frame.width() + "-" + frame.height() + "]|[" + frame.cols() + "-" + frame.rows() + "]");
		if(logOn)log.info(inspection);
		return inspection;
	}

	public enum Portion {
		TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
	}

}
