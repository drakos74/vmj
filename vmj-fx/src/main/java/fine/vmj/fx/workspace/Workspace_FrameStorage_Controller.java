package fine.vmj.fx.workspace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.cv.camera.CameraFeed;
import fine.vmj.cv.camera.effect.EffectUtil;
import fine.vmj.cv.util.CVUtils;
import fine.vmj.fx.workspace.util.WorkspaceProperties;
import fine.vmj.ml.deep.FrameEncoder;
import fine.vmj.store.DataStoreException;
import fine.vmj.store.data.ByteFrame;
import fine.vmj.store.data.Frame;
import fine.vmj.store.data.ByteFrame;
import fine.vmj.store.data.IFrame;
import fine.vmj.util.VMJProperties;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;

public class Workspace_FrameStorage_Controller extends VideoControllerBase {

	private static final Logger log = LoggerFactory.getLogger(Workspace_FrameStorage_Controller.class);
	
	CameraFeed cameraFeed;
	
	FrameEncoder frameEncoder;
	
	CameraFeed.Configuration config;

	@FXML
	ImageView feed1;
	@FXML
	ImageView feed2;
	@FXML
	ImageView feed3;
	@FXML
	ImageView feed4;
	@FXML
	ImageView feed5;
	@FXML
	ImageView feed6;
	@FXML
	ImageView feed7;
	@FXML
	ImageView feed8;
	@FXML
	ImageView feed9;
	@FXML
	ImageView feed10;
	@FXML
	ImageView feed11;
	@FXML
	ImageView feed12;

	@FXML
	CheckBox do_edgeDetect;
	@FXML
	Slider edgeThreshold;
	@FXML
	Label edge_threshold;
	@FXML
	CheckBox do_objDetect;

	@FXML
	Label num_rects;
	@FXML
	Label num_feeds;
	@FXML
	Label num_storage;
	@FXML
	Label num_store;

	ImageView[] feeds;

	void init() {

		config = CameraFeed.Configuration.createNew();
		cameraFeed = new CameraFeed(counter_limit, config);

		// do some UI bindings ...
		this.edge_threshold.textProperty().bind(edgeThreshold.valueProperty().asString());

		feeds = new ImageView[] { feed1, feed2, feed3, feed4, feed5, feed6 , feed7 , feed8 , feed9 , feed10 , feed11 , feed12};

	}
	
	private FrameEncoder getFrameEncoder(){
		if(frameEncoder == null){
			synchronized(this){
				if(frameEncoder == null){
					frameEncoder = new FrameEncoder(10,Double.valueOf(screen.getFitWidth()).intValue(),Double.valueOf(screen.getFitHeight()).intValue(),3);
				}
			}
		}
		return frameEncoder;
	}

	Mat process(Mat frame) {
		cameraFeed.add(frame, count);
		double a = 1.0;
		cameraFeed.pre_process(a, WorkspaceProperties.SCREEN_BRIGHTNESS.getDouble(),
				WorkspaceProperties.SCREEN_CONTRAST.getDouble(), WorkspaceProperties.SCREEN_GAMMA.getDouble());

		// apply ui changes to config ... for demo purposes
		config.adjustMinValues(new Scalar(WorkspaceProperties.SCREEN_HUE_START.getDouble(),
				WorkspaceProperties.SCREEN_SATURATION_START.getDouble(),
				WorkspaceProperties.SCREEN_VALUE_START.getDouble()));

		if (do_edgeDetect.isSelected()) {
			// get edge image ...
			Mat edges = cameraFeed.edgeDetect(this.edgeThreshold.getValue());

			if (do_objDetect.isSelected()) {
				// Mat faceImage = new Mat();
				// frame.copyTo(faceImage);
				List<Frame> frames = cameraFeed.objDetect();
				for (Frame current_frame : frames){
					Rect rect = new Rect(current_frame.getRect().getX(),current_frame.getRect().getY(),current_frame.getRect().getWidth(),current_frame.getRect().getHeight());//object.getRect();
					
					// TODO : draw with color depending on classifier ... 
					Imgproc.rectangle(edges, rect.tl(), rect.br(), new Scalar(0, 255, 0), 3);
				}

				Map<IFrame.Category, List<Frame>> _feeds = cameraFeed.frameStore.getFeeds();

				Platform.runLater(() -> {
					num_rects.setText(String.valueOf(frames.size()));
					num_feeds.setText(cameraFeed.frameStore.peekFeeds());
					num_storage.setText(cameraFeed.frameStore.peekLocalStorage());
					num_store.setText(cameraFeed.frameStore.getCount()+"|"+cameraFeed.frameStore.getSize());
				});
				EffectUtil.applyAdd(frame, edges);

				// print feeds ...
				final List<Integer> views = new ArrayList<Integer>(); 
				_feeds.entrySet().stream().filter(p -> p.getValue().size() > 0).forEach(feed -> {
					log.info("Show frames for cat " + feed.getKey()+" - "+feed.getValue().size());
					try {
						int index = Arrays.asList(IFrame.Category.values()).indexOf(feed.getKey());
						views.add(index);
						updateView(feeds[index],feed.getValue());
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				});
				// reset the rest of the feed screens...
				if(logOn)IntStream.range(0, feeds.length).forEach(index -> {
					if(!views.contains(index)){
						CVUtils.onCVThread(feeds[index].imageProperty(), CVUtils.mat2Image(new Mat(feeds[index].fitHeightProperty().intValue(),feeds[index].fitWidthProperty().intValue(),0,new Scalar(255.0,255.0,255.0,0.0))));
					}
				});
				// TODO : get feedback from camera and store, in order to adjust parameters
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
		log.info("stopped!");
		// flush storage to file ... 
		Executors.newScheduledThreadPool(1).schedule(()->{
			try {
				log.info("Will try to flush now ... ");
				cameraFeed.frameStore.flush();
			} catch (DataStoreException e) {
				log.error(e.getMessage(),e);
			}
		}, 5, TimeUnit.SECONDS);
		
//		try {
//			log.info("cameraFeed.frameStore.getAll():"+cameraFeed.frameStore.getAll().size());
//		} catch (DataStoreException e1) {
//			log.error(e1.getMessage(),e1);
//		}
//		try {
//			cameraFeed.frameStore.getAll().forEach(k -> {
//				log.info(k+"");
//				try {
//					getFrameEncoder().encode((Frame)cameraFeed.frameStore.get((Frame)k));
//				} catch (DataStoreException e) {
//					log.error(e.getMessage(),e);
//				}
//			});
//		} catch (DataStoreException e) {
//			log.error(e.getMessage(),e);
//		}
	}
}
