package fine.vmj.cv.camera.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.DenseOpticalFlow;
import org.opencv.video.DualTVL1OpticalFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.cv.camera.CameraFeed;
import fine.vmj.cv.camera.FrameSpace;
import scala.Tuple2;

public class EffectUtil {
	
	private static final Logger log = LoggerFactory.getLogger(EffectUtil.class);

	static boolean logOn = fine.vmj.util.VMJProperties.SCREEN_LOG_ON.getValue();

	private static int maxBufferSize = 33;

	static List<ArrayList<Mat>> imgBuffer = new ArrayList<ArrayList<Mat>>();
	static List<Boolean> drainBuffer = new ArrayList<Boolean>();
	static Mat imgLayer1 = null;

	static AtomicInteger counter = new AtomicInteger(0);
	static final int max_count = maxBufferSize; // 30;

	static {
		for (int i = 0; i < maxBufferSize; i++) {
			imgBuffer.add(new ArrayList<Mat>());
			drainBuffer.add(false);
		}
	}

	public static void applyAddWeight(Mat frame, Mat overlay) {
		if(logOn)log.info("---" + counter.get() + "----");
		record(frame, overlay);
		// ArrayList<Mat> currentBuffer = null;
		//

		// if(imgBuffer.size() == 0){
		// currentBuffer = new ArrayList<Mat>();
		// imgBuffer.add(currentBuffer);
		// } else{
		// currentBuffer = imgBuffer.get(0);
		// }
		//
		// System.out.println(Thread.currentThread().getName()+" -
		// "+counter.get()+" ["+currentBuffer.size()+"]");
		// int size = currentBuffer.size();
		// if(size < max_count){
		// System.out.println("store overlay in buffer");
		// currentBuffer.add(overlay);
		// // overlay self
		// Mat imageROI = FrameSpace.getRect(frame, frame);
		// Core.addWeighted(imageROI, 1.0, frame, 0.7, 0.0, imageROI);
		// } else if(size > max_count){
		// // should never happen
		// throw new IllegalStateException("This should never happen [ "+size+"
		// - "+counter.get()+" ]");
		// } else{
		// int index = counter.get() % max_count;
		// System.out.println("drain overlay buffer at "+index);
		// Mat _overlay =
		// currentBuffer.get(index);//createFlow(frame,currentBuffer.get(index));
		// Mat imageROI = FrameSpace.getRect(frame, _overlay);
		// Core.addWeighted(imageROI, 1.0, _overlay, 0.7, 0.0, imageROI);
		// if(index == max_count - 1){
		// System.out.println("reset overlay buffer ... ");
		// currentBuffer.clear();
		// }
		// }

		count();
		if(logOn)log.info("---___----");
	}

	public static void count() {
		int count = counter.getAndIncrement();
		if (count == max_count - 1) {
			counter.set(0);
		}
	}

	public static void applyAdd(Mat frame, Mat overlay) {
		Mat imageROI = FrameSpace.getRect(frame, overlay);
		Core.addWeighted(imageROI, 1.0 , overlay, 1.0 / maxBufferSize, 0.0, imageROI);
	}
	
	public static void applyCopy(Mat frame, Mat overlay) {
		Mat imageROI = FrameSpace.getRect(frame, overlay);
		if(logOn)log.info("applyCopy : "+overlay.channels()+" vs "+frame.channels()+" on "+imageROI);
		Core.addWeighted(imageROI, 1.0 , overlay, 1.0 , 0.0, imageROI);
	}
	
	public static void applyCopy(Mat frame, Mat overlay, Rect roi) {
		Mat imageROI = frame.submat(roi);
		if(logOn)log.info("applyCopy : "+overlay.channels()+" vs "+frame.channels()+" on "+imageROI);
		Core.addWeighted(imageROI, 1.0 , overlay, 1.0 , 0.0, imageROI);
	}

	public static Mat applyMask(Mat frame, Mat mask, boolean inverse) {

		// System.out.println("frame["+frame.width()+"-"+frame.height()+"] +
		// mask["+mask.width()+"-"+mask.height()+"]");

		Mat dest = new Mat();
		// Core.bitwise_xor(frame, mask, dest);
		frame.copyTo(dest, mask);

		// try{
		// Core.add(frame, mask , frame);
		//
		// }catch(Exception e){
		// e.printStackTrace();
		// }
		return dest;
	}

	public static Mat createFlow(Mat pFrame, Mat cFrame) {

		try {
			Mat pGray = new Mat(), cGray = new Mat(), Optical_Flow = new Mat();

			Imgproc.cvtColor(pFrame, pFrame, Imgproc.COLOR_BGR2GRAY);
			Imgproc.cvtColor(cFrame, cFrame, Imgproc.COLOR_BGR2GRAY);

			pFrame.convertTo(pGray, CvType.CV_8UC1);
			cFrame.convertTo(cGray, CvType.CV_8UC1);

			final DenseOpticalFlow tvl1 = DualTVL1OpticalFlow.create();
			tvl1.calc(pGray, cGray, Optical_Flow);

			final Mat OF = new Mat(pGray.rows(), pGray.cols(), CvType.CV_8UC1);
			// final FloatBuffer in = Optical_Flow.ge.createBuffer(),
			// out = OF.createBuffer();

			final int height = pGray.rows(), width = pGray.cols();

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					final double[] xVelocity = Optical_Flow.get(x, y);
					final double[] yVelocity = Optical_Flow.get(x, y);
					final double pixelVelocity = (float) Math
							.sqrt(xVelocity[0] * xVelocity[0] + yVelocity[1] * yVelocity[1]);
					OF.put(x, y, pixelVelocity);
				}
			}
			return OF;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Mat();
	}

	public static void drain(Mat frame, List<Mat> buffer) {
		final int index = 0;// counter.get() % i;
		final Mat _overlay = buffer.get(index);
		applyAdd(frame, _overlay);
		buffer.remove(0);
	}

	public static void record(Mat frame, Mat overlay) {

		if(logOn)log.info("record_start...");
		int i = 1;
		boolean added = false;
		for (List<Mat> buffer : imgBuffer) {
			int index = i - 1;
			boolean drain = drainBuffer.get(index);
			if(logOn)log.info("Buffer at " + index + " size=" + buffer.size());
			
			if (buffer.size() > index) {
				// trigger ... 
				drain = true;
				drainBuffer.set(index, drain);
				// use buffer ...
				if(logOn)log.info("drain buffer " + index + " , use buffer frame ");
				drain(frame, buffer);
			} else {
				// we need to aply some image always ...
				if (drain) {
					if (buffer.size() == 0) {
						drain = false;
						drainBuffer.set(index, drain);
						imgBuffer.get(index-1).clear();
						if(logOn)log.info("clear buffer " + index + " , use same frame ");
						applyAdd(frame, frame);
					} else {
						if(logOn)log.info("drain buffer " + index+" , use buffer frame");
						drain(frame, buffer);
					}
				} else {
					if(added)buffer.add(overlay);
					// dont use same frame twice
					added = true;
					if(logOn)log.info("put to buffer " + index + " , use same frame ");
					applyAdd(frame, frame);
//					break;
				}
			}
			i++;
		}
		if(logOn)log.info("record_end...");

	}

}
