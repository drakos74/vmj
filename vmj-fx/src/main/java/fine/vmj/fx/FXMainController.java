package fine.vmj.fx;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

import fine.vmj.cv.camera.FrameSpace;
import fine.vmj.cv.camera.FrameSpace.Portion;
import fine.vmj.cv.camera.effect.EffectUtil;
import fine.vmj.cv.camera.FrameTransform;
import fine.vmj.cv.util.CVUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import scala.Tuple2;

public class FXMainController {

	// the FXML button
	@FXML
	private Button s_btn;
	// the FXML image view
	@FXML
	private ImageView c_feed;
	@FXML
	private ImageView c_hist;
	@FXML
	private ImageView a_sample;
	@FXML
	private ImageView b_sample;
	@FXML
	private ImageView c_sample;
	@FXML
	private ImageView o_feed;
	@FXML
	private CheckBox greyscale;
	@FXML
	private CheckBox img;
	@FXML
	private CheckBox dft_inv;
	// Face Detect
	@FXML
	private CheckBox cl_haar;
	@FXML
	private CheckBox cl_lbp;
	// Edge Detect
	@FXML
	private CheckBox edge_detect;
	@FXML
	private CheckBox obj_map;
	@FXML
	private CheckBox obj_map_inverse;
	@FXML
	private Slider threshold;
	// Object Detect
	@FXML
	private CheckBox obj_detect;
	@FXML
	private ImageView maskImage;
	@FXML
	private ImageView hsvImage;
	@FXML
	private ImageView morphImage;
	// FXML slider for setting HSV ranges
	@FXML
	private Slider hueStart;
	@FXML
	private Slider hueStop;
	@FXML
	private Slider saturationStart;
	@FXML
	private Slider saturationStop;
	@FXML
	private Slider valueStart;
	@FXML
	private Slider valueStop;
	// FXML label to show the current values set with the sliders
	@FXML
	private Label hsvCurrentValues;
	// property for object binding
	private ObjectProperty<String> hsvValuesProp;

	private Mat logo;

	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that realizes the video capture
	private VideoCapture capture = new VideoCapture();
	// a flag to change the button behavior
	private boolean cameraActive = false;
	// the id of the camera to be used
	private static int cameraId = 0;

	// the main stage
	private Stage stage;
	// the JavaFX file chooser
	private FileChooser fileChooser;
	
	// skipFrame
	boolean skip = false;

	/**
	 * Init the needed variables
	 */
	protected void init() {

		this.fileChooser = new FileChooser();

		hsvValuesProp = new SimpleObjectProperty<>();
		this.hsvCurrentValues.textProperty().bind(hsvValuesProp);
		
		// set default values for reasonable viewing result ...
		// enable the 1st part of obj detect feature
		obj_map.setSelected(true);
		// enable the dummy overlay ...
		edge_detect.setSelected(true);
		// disable not used controls ...
		greyscale.setDisable(true);
		cl_lbp.setDisable(true);
		dft_inv.setDisable(true);
		img.setDisable(true);
		obj_map.setDisable(true);
	}

	/**
	 * The action triggered by pushing the button on the GUI
	 *
	 * @param event
	 *            the push button event
	 */
	@FXML
	protected void startCamera(ActionEvent event) {
		if (!this.cameraActive) {
			// start the video capture
			this.capture.open(cameraId);

			// is the video stream available?
			if (this.capture.isOpened()) {
				this.cameraActive = true;

				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameGrabber = new Runnable() {

					@Override
					public void run() {
						// System.out.println(System.currentTimeMillis());
						// effectively grab and process a single frame
						Mat frame = grabFrame();
						// convert and show the frame
						if(!skip || true){
							Image imageToShow = CVUtils.mat2Image(frame);
							updateImageView(c_feed, imageToShow);
							skip = true;
						} else{
							skip = false;
						}
					}
				};

				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

				// update the button content
				this.s_btn.setText("Stop");
			} else {
				// log the error
				System.err.println("Impossible to open the camera connection...");
			}
		} else {
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.s_btn.setText("Start");

			// stop the timer
			this.stopAcquisition();
		}
	}

	@FXML
	protected void loadImg() {
		if (img.isSelected()) {

			File file = new File("src/main/resources/");

			System.out.println("file:" + file.getAbsolutePath());

			this.fileChooser.setInitialDirectory(file);

			// show the open dialog window
			file = this.fileChooser.showOpenDialog(this.stage);
			if (file != null) {
				// read the image in gray scale
				this.logo = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
				// show the image
				this.c_sample.setImage(CVUtils.mat2Image(this.logo));
				// // set a fixed width
				// this.c_dft.setFitWidth(250);
				// // preserve image ratio
				// this.c_dft.setPreserveRatio(true);
			}

			// this.logo = Imgcodecs.imread("resources/fine/vmj/fx/Poli.png");
			if (this.logo == null) {
				return;
			}
			if (this.logo.width() == 0 || this.logo.height() == 0) {
				System.out.println("No image found ... ");
				this.logo = null;
				return;
			}
		}
	}

	/**
	 * The action triggered by selecting the Haar Classifier checkbox. It loads
	 * the trained set to be used for frontal face detection.
	 */
	@FXML
	protected void haarSelected(Event event) {
		// check whether the lpb checkbox is selected and deselect it
		if (this.cl_lbp.isSelected())
			this.cl_lbp.setSelected(false);

		this.checkboxSelection("haarcascades/haarcascade_frontalface_alt.xml");
	}

	/**
	 * The action triggered by selecting the LBP Classifier checkbox. It loads
	 * the trained set to be used for frontal face detection.
	 */
	@FXML
	protected void lbpSelected(Event event) {
		// check whether the haar checkbox is selected and deselect it
		if (this.cl_haar.isSelected())
			this.cl_haar.setSelected(false);

		this.checkboxSelection("lbpcascades/lbpcascade_frontalface.xml");
	}

	@FXML
	protected void thresholdChange(Event event) {
		System.out.println(this.threshold.getValue());
	}

	/**
	 * Method for loading a classifier trained set from disk
	 * 
	 * @param classifierPath
	 *            the path on disk where a classifier trained set is located
	 */
	private void checkboxSelection(String classifierPath) {
		// load the classifier(s)
		FrameSpace.loadClassifier(classifierPath);
	}

	/**
	 * Get a frame from the opened video stream (if any)
	 *
	 * @return the {@link Mat} to show
	 */
	private Mat grabFrame() {
		// init everything
		Mat frame = new Mat();

		// check if the capture is open
		if (this.capture.isOpened()) {
			try {
				// read the current frame
				this.capture.read(frame);

				// if the frame is not empty, process it
				if (!frame.empty()) {

					if (img.isSelected()) {
						if (this.logo == null) {
							this.logo = frame.submat(FrameSpace.getRect(frame, Portion.CENTER));
						}
						Mat imageROI = FrameSpace.getRect(frame, logo);
						// add the logo: method #1 -> Blend /Add
						Core.addWeighted(imageROI, 1.0, logo, 0.7, 0.0, imageROI);
						// add the logo: method #2 -> Overlay
						// Mat mask = logo.clone();
						// logo.copyTo(imageROI, mask);

					}

					if (this.greyscale.isSelected()) {
						Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
						// Imgproc.cvtColor(frame, frame,
						// Imgproc.COLOR_BGR2HSV_FULL);
					}

					// show the histogram
					Mat histImage = FrameTransform.getHistogram(frame, greyscale.isSelected());
					Image histImg = CVUtils.mat2Image(histImage);
					if (c_hist == null) {
						System.out.println("no c_hist defined ... ");
					} else {
						this.c_hist.setImage(histImg);
					}

					if (this.cl_lbp.isSelected() || this.cl_haar.isSelected())
						FrameSpace.faceDetect(frame, true);

					if (this.edge_detect.isSelected()) {
						Mat edges = FrameSpace.edgeDetect(frame, this.threshold.getValue());
//						c_sample.setImage(CVUtils.mat2Image(EffectUtil.createFlow(frame, cFrame)));
						try{
							// populate sample windows ... 
//							Mat g = new Mat(frame.width(),frame.height(),CvType.CV_8UC1);
//							frame.convertTo(g, CvType.CV_8UC1);
//							c_sample.setImage(CVUtils.mat2Image(g));
//							
//							Mat h = new Mat(frame.width(),frame.height(),CvType.CV_8UC1);
//							frame.convertTo(h, CvType.CV_8UC1);
//							b_sample.setImage(CVUtils.mat2Image(h));
						}catch(Exception e){
							e.printStackTrace();
						}
						
						EffectUtil.applyAddWeight(frame, frame);
					}

					if (this.obj_map.isSelected()) {
						// moved to combined effect result ... below
					}

					if (this.obj_map.isSelected() && this.obj_detect.isSelected()) {

						Mat obj = new Mat();

						try {
							obj = FrameSpace.backgroundRemove(frame, this.obj_map_inverse.isSelected());
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						Tuple2<Mat, Scalar[]> hsv = FrameSpace.getHSV(frame,
								new double[] { hueStart.getValue(), hueStop.getValue() },
								new double[] { saturationStart.getValue(), saturationStop.getValue() },
								new double[] { valueStart.getValue(), valueStop.getValue() });

						Mat hsvImage = hsv._1();
						Scalar minValues = hsv._2()[0];
						Scalar maxValues = hsv._2()[1];
						// show the current selected HSV range
						String valuesToPrint = "Hue range: " + minValues.val[0] + "-" + maxValues.val[0]
								+ "\tSaturation range: " + minValues.val[1] + "-" + maxValues.val[1] + "\tValue range: "
								+ minValues.val[2] + "-" + maxValues.val[2];
						
						CVUtils.onCVThread(this.hsvValuesProp, valuesToPrint);
						CVUtils.onCVThread(this.hsvImage.imageProperty(), CVUtils.mat2Image(hsvImage));

						Mat mask = FrameSpace.getMaskImage(hsvImage, minValues, maxValues);

						CVUtils.onCVThread(this.maskImage.imageProperty(), CVUtils.mat2Image(mask));
						Mat morphOutput = FrameSpace.getMorphImage(mask);
						 // show the partial output
						 // find the tennis ball(s) contours and show them
						if (this.obj_detect.isSelected()) {
							frame = FrameSpace.findAndDrawBalls(morphOutput, frame);
						}

						Mat masked = EffectUtil.applyMask(obj,morphOutput,true);
						EffectUtil.applyAddWeight(frame, masked);
						
//						EffectUtil.applyAddWeight(frame, obj);
						o_feed.setImage(CVUtils.mat2Image(obj));
						CVUtils.onCVThread(this.morphImage.imageProperty(), CVUtils.mat2Image(morphOutput));

						
					}
					// aplply the DFT ... TODO investigate on this
					// Mat dft =
					// FrameTransform.applyDFT(frame,dft_inv.isSelected());

				}

			} catch (Exception e) {
				// log the error
				System.err.println("Exception during the image elaboration: " + e);
			}
		}

		return frame;
	}

	/**
	 * Stop the acquisition from the camera and release all the resources
	 */
	private void stopAcquisition() {
		if (this.timer != null && !this.timer.isShutdown()) {
			try {
				// stop the timer
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// log any exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}

		if (this.capture.isOpened()) {
			// release the camera
			this.capture.release();
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
	private void updateImageView(ImageView view, Image image) {
		CVUtils.onCVThread(view.imageProperty(), image);
	}

	/**
	 * Set the current stage (needed for the FileChooser modal window)
	 * 
	 * @param stage
	 *            the stage
	 */
	public void setStage(Stage stage) {
		this.stage = stage;
	}

	/**
	 * On application close, stop the acquisition from the camera
	 */
	protected void setClosed() {
		this.stopAcquisition();
	}
}
