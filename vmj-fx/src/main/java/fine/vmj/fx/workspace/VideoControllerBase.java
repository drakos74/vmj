package fine.vmj.fx.workspace;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.cv.camera.CameraFeed;
import fine.vmj.cv.util.CVUtils;
import fine.vmj.store.data.ByteFrame;
import fine.vmj.store.data.Frame;
import fine.vmj.store.data.PixelFrame;
import fine.vmj.util.VMJProperties;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public abstract class VideoControllerBase {

	private static final Logger log = LoggerFactory.getLogger(VideoControllerBase.class);

	protected static final boolean logOn = VMJProperties.WORKSPACE_LOG_ON.getValue();

	private static int skipFrames = 0;

	private ScheduledExecutorService timer;
	private VideoCapture capture = new VideoCapture();
	private boolean isOn = false;
	private static int cameraId = 0;

	private Stage stage;
	private FileChooser fileChooser;

	static final int counter_limit = fine.vmj.util.VMJProperties.CYCLE_SIZE.getValue();
	static final boolean cameraCaptureOn = fine.vmj.util.VMJProperties.CAMERA_CAPTURE_ON.getValue();
	//10_000; // gives about 333.3 secs (5 mins.) of cache
	AtomicInteger cycles = new AtomicInteger(0);
	int count = 0;

	@FXML
	Button m_btn;

	@FXML
	ImageView screen;

	abstract void init();

	abstract Mat process(Mat frame);
	
	abstract void stopped();
	
	abstract void started();

	Mat grabFrame() {
		Mat frame = new Mat();
		if (this.capture.isOpened()) {
			// counter update ... first thing!

			try {
				this.capture.read(frame);

				// if the frame is not empty, process it
				if (!frame.empty()) {
					frame = process(frame);
				}

				// reset count ...
				if (count == counter_limit) {
					cycles.set(0);
				}

			} catch (Exception e) {
				// log the error
				log.error(e.getMessage(),e);
			}
		} else if(!cameraCaptureOn){
			frame = process(null);
		}
		count = cycles.addAndGet(1);

		return frame;
	}
	
	void updateView(ImageView view , List<Frame> frames){
		int size = frames.size();
		Frame s_frame = frames.get(count % size);
		
		Mat this_frame = new Mat();
//		if(s_frame instanceof ByteFrame){
			byte[] bytes = ((ByteFrame) s_frame).getBytes();
			this_frame = CVUtils.bytes2Mat(bytes, s_frame.getRect().getWidth(),s_frame.getRect().getHeight(), s_frame.getType());
//		} else if(s_frame instanceof PixelFrame){
//			throw new IllegalStateException("Method not implemented yet for PixelFrames "+s_frame);
//		} else{
//			throw new IllegalStateException("Unknown frame type encountered "+s_frame);
//		}
		
		CVUtils.onCVThread(view.imageProperty(), CVUtils.mat2Image(this_frame));
	}

	void updateView(Mat frame) {
		if(frame == null || frame.empty()){
			if(logOn)log.info("no frame to update main screen ... ");
			return;
		}
		if (0 % count == 0) {
			Image image = CVUtils.mat2Image(frame);
			updateImageView(screen, image);
		}
	}

	@FXML
	protected void doStart(ActionEvent event) {
		if (!this.isOn) {
			// start the video capture
			if(!this.capture.isOpened() && cameraCaptureOn){
				this.capture.open(cameraId);
			}

			// is the video stream available?
			if (this.capture.isOpened() || !cameraCaptureOn) {
				this.isOn = true;

				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameRunner = new Runnable() {
					@Override
					public void run() {
						Mat frame = grabFrame();
						updateView(frame);
					}
				};

				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameRunner, 0, 33, TimeUnit.MILLISECONDS);

				// update the button content
				this.m_btn.setText("Stop");
				this.started();
			} else {
				// log the error
				System.err.println("Impossible to open the camera connection...");
			}
		} else {
			// the camera is not active at this point
			this.isOn = false;
			// update again the button content
			this.m_btn.setText("Start");
			// stop the timer
			this.stopAcquisition();
			this.stopped();
		}
	}

	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 * 
	 * @param view
	 *            the {@link ImageView} to update
	 * @param image
	 *            the {@link Image} to show
	 */
	void updateImageView(ImageView view, Image image) {
		CVUtils.onCVThread(view.imageProperty(), image);
	}

	/**
	 * Set the current stage (needed for the FileChooser modal window)
	 * 
	 * @param stage
	 *            the stage
	 */
	void setStage(Stage stage) {
		this.stage = stage;
	}

	/**
	 * On application close, stop the acquisition from the camera
	 */
	void setClosed() {
		this.isOn = false;
		this.stopAcquisition();
		if (this.capture.isOpened()) {
			// release the camera
			this.capture.release();
			// await release of resources
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.error("Exception in stopping the camera feed... " + e);
			}
		}
	}

	/**
	 * Stop the acquisition from the camera and release all the resources
	 */
	void stopAcquisition() {
		
		try {
			// stop the timer
			this.timer.shutdownNow();
			this.timer.awaitTermination(36, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// log any exception
			System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
		} finally {

		}
	}

}
