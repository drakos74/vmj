package fine.vmj.fx.workspace;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.cv.camera.CameraFeed;
import fine.vmj.cv.camera.effect.EffectUtil;
import fine.vmj.cv.util.CVUtils;
import fine.vmj.ml.simple.FrameMatcher;
import fine.vmj.store.data.ByteFrame;
import fine.vmj.store.data.Frame;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;

public class Workspace_Controller extends VideoControllerBase {

	private static final Logger log = LoggerFactory.getLogger(Workspace_Controller.class);

	CameraFeed cameraFeed;

	CameraFeed.Configuration config;

	@FXML
	ImageView c_hist;

	@FXML
	ImageView blurImage;
	@FXML
	ImageView hsvImage;
	@FXML
	ImageView edgeImage;
	@FXML
	ImageView trackImage;
	@FXML
	ImageView cascadeImage;
	@FXML
	ImageView objImage;
	@FXML
	ImageView processedImage;
	@FXML
	ImageView demoImage;
	
	@FXML
	ImageView pr_frame_1;
	@FXML
	ImageView pr_frame_2;
	@FXML
	ImageView pr_frame_3;
	@FXML
	ImageView pr_frame_4;
	@FXML
	ImageView pr_frame_5;
	@FXML
	ImageView pr_frame_6;
	
	ImageView[] pr_frames;
	
	// blur
	@FXML
	Slider greyBlurKernelSize;
	@FXML
	Label greyBlurKernelSizeValue;
	@FXML
	Slider colorBlurKernelSize;
	@FXML
	Label colorBlurKernelSizeValue;
	@FXML
	Slider blurKernelRatio;
	@FXML
	Label blurKernelRatioValue;


	@FXML
	CheckBox do_edgeDetect;
	@FXML
	Slider edgeThreshold;
	@FXML
	Label edge_threshold;
	@FXML
	CheckBox do_objDetect;

	// main frame adjustments
	@FXML
	Slider m_brightness;
	@FXML
	Label m_brightnessValue;
	@FXML
	Slider m_contrast;
	@FXML
	Label m_contrastValue;
	@FXML
	Slider m_gamma;
	@FXML
	Label m_gammaValue;
	
	// metrics
	@FXML
	Label tracked_frames;
	@FXML
	Label replaced_frames;
	@FXML
	Label matched_frames;
	

	private FrameMatcher frameMatcher;

	void init() {

		config = CameraFeed.Configuration.createNew();
		cameraFeed = new CameraFeed(counter_limit, config);

		// do some UI bindings ...
		this.edge_threshold.textProperty().bind(edgeThreshold.valueProperty().asString());

		this.greyBlurKernelSizeValue.textProperty().bind(greyBlurKernelSize.valueProperty().asString());
		this.colorBlurKernelSizeValue.textProperty().bind(colorBlurKernelSize.valueProperty().asString());

		frameMatcher = new FrameMatcher();
		
		pr_frames = new ImageView[]{pr_frame_1,pr_frame_2,pr_frame_3,pr_frame_4,pr_frame_5,pr_frame_6};

	}

	Mat process(Mat frame) {
		cameraFeed.add(frame, count);
		double a = 1.0;
		cameraFeed.pre_process(a,1.0, 0.0, 0.0);

		// apply ui changes to config ... for demo purposes
		config.adjustMinValues(new Scalar(20, 60, 50));
		config.adjustMaxValues(new Scalar(50, 200, 255));

		// create histogram
		Mat histImage = cameraFeed.getHistogram();
		this.c_hist.setImage(CVUtils.mat2Image(histImage));

		if (do_edgeDetect.isSelected()) {
			
			// get edge image ...
			final Mat edges = cameraFeed.edgeDetect(this.edgeThreshold.getValue());
			CVUtils.onCVThread(this.edgeImage.imageProperty(), CVUtils.mat2Image(edges));

			// draw also grey image ...
			CVUtils.onCVThread(this.cascadeImage.imageProperty(), CVUtils.mat2Image(cameraFeed.greyBlurred()));

			// prepare the processed frame ... new empty frame
			final Mat processedFrame = new Mat(frame.size(), frame.type(), new Scalar(0, 0, 0));
			// prepare the demo frame ... copy of current frame
			final Mat demoFrame = new Mat();
			frame.copyTo(demoFrame);
			if (do_objDetect.isSelected()) {
			
				// Mat faceImage = new Mat();
				// frame.copyTo(faceImage);
				List<Frame> frames = cameraFeed.objDetect();
				
				final AtomicInteger matched = new AtomicInteger(0);
				final AtomicInteger replaced = new AtomicInteger(0);
				
				frames.stream().forEach(pframe -> {
					ByteFrame qframe = null;

					int my_index = -1;
					
					Rect rect = new Rect(pframe.getRect().getX(), pframe.getRect().getY(),
							pframe.getRect().getWidth(), pframe.getRect().getHeight());
					ByteFrame matchingFrame = null;// frameMatcher.anyMatch(pframe,count);
					log.info("matchingFrame:" + matchingFrame);
					if (matchingFrame == null) {
						log.info("use old current frame ... ");
					} else {
						my_index = matched.getAndIncrement();
						log.info("use stored frame ... ");
						qframe = frameMatcher.replace(matchingFrame,pframe,count);
					}
					if(qframe != null){
						replaced.getAndIncrement();
					}
					ByteFrame iframe = qframe == null ? (ByteFrame) pframe : qframe;
					rect = new Rect(iframe.getRect().getX(), iframe.getRect().getY(),
							iframe.getRect().getWidth(), iframe.getRect().getHeight());
					
					Imgproc.rectangle(edges, rect.tl(), rect.br(), new Scalar(0, 255, 0), 3);

					final Mat _processedFrame = new Mat(frame.size(), frame.type(), new Scalar(0, 0, 0));

					// copy to black ... 
					Mat mat = CVUtils.convertToMat(iframe);
					EffectUtil.applyCopy(processedFrame, mat, rect);
					// create new black copy to add ...
					EffectUtil.applyCopy(_processedFrame, mat, rect);
					log.info("my_index:"+my_index);
					if(my_index >= 0) CVUtils.onCVThread(
							pr_frames[my_index]
							.imageProperty(), 
							CVUtils
							.mat2Image(_processedFrame));

					
//					EffectUtil.applyAddWeight(demoFrame, _processedFrame);
					EffectUtil.applyCopy(demoFrame, mat, rect);

				});
				
				Platform.runLater(() -> {
					tracked_frames.setText(String.valueOf(frames.size()));
					matched_frames.setText(String.valueOf(matched == null ? 0 : matched.get()));
					replaced_frames.setText(String.valueOf(replaced == null ? 0 : replaced.get()));

				});
				
				CVUtils.onCVThread(this.processedImage.imageProperty(), CVUtils.mat2Image(processedFrame));
				CVUtils.onCVThread(this.demoImage.imageProperty(), CVUtils.mat2Image(demoFrame));
			}
		}

		// apply any effect ...
		Mat processedFrame = cameraFeed.process(frame);
		return processedFrame;
	}

	@Override
	void started() {
		cameraFeed.start();
		do_edgeDetect.setSelected(true);
		do_objDetect.setSelected(true);
	}

	@Override
	void stopped() {
		cameraFeed.stop();
	}

}