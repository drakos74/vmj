package fine.vmj.fx.workspace;

import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.cv.FrameSpaceException;
import fine.vmj.cv.FrameTransformException;
import fine.vmj.cv.camera.CameraFeed;
import fine.vmj.cv.camera.FrameSpace;
import fine.vmj.cv.util.CVUtils;
import fine.vmj.store.data.Frame;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;

public class Workspace_ImageProcessing_Controller extends VideoControllerBase {
	
	private static final Logger log = LoggerFactory.getLogger(Workspace_ImageProcessing_Controller.class);

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
	ImageView maskImage;
	@FXML
	ImageView morphImage;
	@FXML
	ImageView thresholdImage;	
	@FXML
	ImageView cleanImage;

	@FXML
	CheckBox do_hsv;
	@FXML
	CheckBox do_mask;
	@FXML
	CheckBox do_morph;
	@FXML
	CheckBox do_back_rem;
	@FXML
	Slider backRemThreshold;
	@FXML
	CheckBox back_rem_inv;
	
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
	
	// dilation / erosion
	@FXML
	Slider dilateKernelSize;
	@FXML
	Label dilateKernelSizeValue;
	@FXML
	Slider erodeKernelSize;
	@FXML
	Label erodeKernelSizeValue;

	// FXML slider for setting HSV ranges
	@FXML
	private Slider hueStart;
	@FXML
	private Label hue_start;
	@FXML
	private Slider hueStop;
	@FXML
	private Label hue_stop;
	@FXML
	private Slider saturationStart;
	@FXML
	private Label saturation_start;
	@FXML
	private Slider saturationStop;
	@FXML
	private Label saturation_stop;
	@FXML
	private Slider valueStart;
	@FXML
	private Label value_start;
	@FXML
	private Slider valueStop;
	@FXML
	private Label value_stop;
	// FXML label to show the current values set with the sliders
	
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
	

	void init() {
		
		config = CameraFeed.Configuration.createNew();
		cameraFeed = new CameraFeed(counter_limit,config);
		
		// do some UI bindings ... 
		this.edge_threshold.textProperty().bind(edgeThreshold.valueProperty().asString());

		this.hue_start.textProperty().bind(hueStart.valueProperty().asString());
		this.hue_stop.textProperty().bind(hueStop.valueProperty().asString());
		this.saturation_start.textProperty().bind(saturationStart.valueProperty().asString());
		this.saturation_stop.textProperty().bind(saturationStop.valueProperty().asString());
		this.value_start.textProperty().bind(valueStart.valueProperty().asString());
		this.value_stop.textProperty().bind(valueStop.valueProperty().asString());
		
		this.greyBlurKernelSizeValue.textProperty().bind(greyBlurKernelSize.valueProperty().asString());
		this.colorBlurKernelSizeValue.textProperty().bind(colorBlurKernelSize.valueProperty().asString());

		this.blurKernelRatioValue.textProperty().bind(blurKernelRatio.valueProperty().asString());
		
		this.dilateKernelSizeValue.textProperty().bind(dilateKernelSize.valueProperty().asString());
		this.erodeKernelSizeValue.textProperty().bind(erodeKernelSize.valueProperty().asString());

		this.greyBlurKernelSize.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                Number old_val, Number new_val) {
                    config.adjustGreyBlurKernel(greyBlurKernelSize.getValue());
            }
        });
		
		this.colorBlurKernelSize.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                Number old_val, Number new_val) {
                    config.adjustColorBlurKernel(colorBlurKernelSize.getValue());
            }
        });
		
		this.blurKernelRatio.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                Number old_val, Number new_val) {
                    config.adjustCannyKernelRatio(blurKernelRatio.getValue());
            }
        });
		
		this.dilateKernelSize.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                Number old_val, Number new_val) {
                    config.adjustDilateKernel(dilateKernelSize.getValue());
            }
        });
		
		this.erodeKernelSize.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                Number old_val, Number new_val) {
                    config.adjustErodeKernel(erodeKernelSize.getValue());
            }
        });
		
		this.m_brightnessValue.textProperty().bind(m_brightness.valueProperty().asString());
		this.m_contrastValue.textProperty().bind(m_contrast.valueProperty().asString());

	}

	Mat process(Mat frame) {
		cameraFeed.add(frame, count);
		double a = 1.0;
		cameraFeed.pre_process(a,m_brightness.getValue(),m_contrast.getValue(),m_gamma.getValue());
		
		// apply ui changes to config ... for demo purposes
		config.adjustMinValues(new Scalar(hueStart.getValue(), saturationStart.getValue(), valueStart.getValue()));
		config.adjustMaxValues(new Scalar(hueStop.getValue(), saturationStop.getValue(), valueStop.getValue()));

		
		// create histogram
		Mat histImage = cameraFeed.getHistogram();
		this.c_hist.setImage(CVUtils.mat2Image(histImage));

		if(do_edgeDetect.isSelected()){
			// get edge image ... 
			Mat edges = cameraFeed.edgeDetect(this.edgeThreshold.getValue());
			CVUtils.onCVThread(this.edgeImage.imageProperty(), CVUtils.mat2Image(edges));
			
			// draw also grey image ...
			CVUtils.onCVThread(this.cascadeImage.imageProperty(), CVUtils.mat2Image(cameraFeed.greyBlurred()));
			
			if(do_objDetect.isSelected()){
//				Mat faceImage = new Mat();
//				frame.copyTo(faceImage);
				List<Frame> frames = cameraFeed.objDetect();
				for (Frame current_frame : frames){
					Rect rect = new Rect(current_frame.getRect().getX(),current_frame.getRect().getY(),current_frame.getRect().getWidth(),current_frame.getRect().getHeight());//object.getRect();
					Imgproc.rectangle(edges, rect.tl(), rect.br(), new Scalar(0, 255, 0), 3);
				}
				CVUtils.onCVThread(this.objImage.imageProperty(), CVUtils.mat2Image(edges));
				CVUtils.onCVThread(this.processedImage.imageProperty(), CVUtils.mat2Image(cameraFeed.processed()));
				CVUtils.onCVThread(this.demoImage.imageProperty(), CVUtils.mat2Image(cameraFeed.demo()));
			}
		}
		
		// create HSV values
		if (do_hsv.isSelected()) {
			
			try{
				cameraFeed.hsv();
			}catch(Exception e){
				e.printStackTrace();
				throw new FrameTransformException("could not apply HSV color to frame at "+count);
			}

			CVUtils.onCVThread(blurImage.imageProperty(), CVUtils.mat2Image(cameraFeed.colorBlurred()));

			// get thresholding values from the UI
			// remember: H ranges 0-180, S and V range 0-255
			Scalar minValues = new Scalar(hueStart.getValue(), saturationStart.getValue(), valueStart.getValue());
			Scalar maxValues = new Scalar(hueStop.getValue(), saturationStop.getValue(), valueStop.getValue());

			CVUtils.onCVThread(this.hsvImage.imageProperty(), CVUtils.mat2Image(cameraFeed.hsved()));
			
			if(do_mask.isSelected()){
				
				try{
					cameraFeed.mask(minValues, maxValues);
				}catch(Exception e){
					log.error(e.getMessage(),e);
					throw new FrameTransformException("could not apply mask to frame at "+count);
				}

				CVUtils.onCVThread(this.maskImage.imageProperty(), CVUtils.mat2Image(cameraFeed.masked()));
				
				if(do_morph.isSelected()){
					Mat morphOutput = cameraFeed.morph();
					CVUtils.onCVThread(this.morphImage.imageProperty(), CVUtils.mat2Image(morphOutput));
//				    frame = FrameSpace.findAndDrawBalls(morphOutput, frame);
					
					if(do_back_rem.isSelected()){
						try{
							cameraFeed.threshold(back_rem_inv.isSelected());
						}catch(Exception e){
							e.printStackTrace();
							throw new FrameSpaceException("Coult not remove background from current frame "+count);
						}
						
						Mat thresholdImg = cameraFeed.thresholdImg();
						CVUtils.onCVThread(this.thresholdImage.imageProperty(), CVUtils.mat2Image(thresholdImg));
						
						Imgproc.dilate(thresholdImg, thresholdImg, new Mat(), new Point(-1, -1), 1);
						Imgproc.erode(thresholdImg, thresholdImg, new Mat(), new Point(-1, -1), 3);

						Imgproc.threshold(thresholdImg, thresholdImg, backRemThreshold.getValue(), 179.0, Imgproc.THRESH_BINARY);

						// create the new image
						Mat foreground = new Mat(frame.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
						frame.copyTo(foreground, thresholdImg);
						
						CVUtils.onCVThread(this.cleanImage.imageProperty(), CVUtils.mat2Image(foreground));
					}
				}
			}
		}

		// apply any effect ... 
		Mat processedFrame = cameraFeed.process(frame);
		return processedFrame;
	}
	
	@Override
	void started(){
		cameraFeed.start();
		do_edgeDetect.setSelected(true);
		do_objDetect.setSelected(true);
	}
	
	@Override
	void stopped(){
		cameraFeed.stop();
	}

}
