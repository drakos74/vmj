package fine.vmj.fx.workspace;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.cv.camera.CameraFeed;
import fine.vmj.cv.camera.FrameTransform;
import fine.vmj.cv.camera.effect.EffectUtil;
import fine.vmj.cv.util.CVUtils;
import fine.vmj.ml.deep.FrameEncoder;
import fine.vmj.store.DataStoreException;
import fine.vmj.store.data.ByteFrame;
import fine.vmj.store.data.DataStoreUtil;
import fine.vmj.store.data.Frame;
import fine.vmj.store.data.IFrame;
import fine.vmj.store.data.ByteFrame;
import fine.vmj.util.VMJProperties;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

// need to start with ... fine.vmj.util.VMJProperties.CAMERA_CONTROLLER_DRY_RUN.setValue(true);
public class Workspace_ML_Controller extends VideoControllerBase {

	private static final Logger log = LoggerFactory.getLogger(Workspace_ML_Controller.class);

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
	Label num_feeds;
	@FXML
	Label num_storage;
	@FXML
	Label num_store;

	ImageView[] feeds;

	Map<Frame, List<Frame>> video_feeds = new HashMap<Frame, List<Frame>>();
	
	final Size frame_size = new Size(VMJProperties.ML_FRAME_SIZE.getValue(),VMJProperties.ML_FRAME_SIZE.getValue());
	final int channel_num = VMJProperties.ML_FRAME_SIZE.getValue();

	void init() {

		config = CameraFeed.Configuration.createNew();
		cameraFeed = new CameraFeed(counter_limit, config);

		feeds = new ImageView[] { feed1, feed2, feed3, feed4, feed5, feed6, feed7, feed8, feed9, feed10, feed11,
				feed12 };

		try {

			video_feeds = cameraFeed.frameStore.load();

			num_feeds.setText(video_feeds.size() + "");

		} catch (DataStoreException e) {
			log.error(e.getMessage(), e);
		}
		
		try{
			getFrameEncoder();
		}catch(Exception e){
			log.error(e.getMessage(),e);
		}

	}

	private FrameEncoder getFrameEncoder() {
		try{
			if (frameEncoder == null) {
				synchronized (this) {
					if (frameEncoder == null) {
						frameEncoder = new FrameEncoder(10, Double.valueOf(frame_size.width).intValue(),
								Double.valueOf(frame_size.height).intValue(),channel_num);
					}
				}
			}
		}catch(Exception e){
			log.error(e.getMessage(),e);
		}
		return frameEncoder;
	}

	@Override
	Mat process(Mat frame) {
		// log.info(count + "");
		// play the frames in feeds
		frame = new Mat(screen.fitHeightProperty().intValue(), screen.fitWidthProperty().intValue(), CvType.CV_8UC3,
				new Scalar(0.0, 0.0, 0.0));
		AtomicInteger i = new AtomicInteger(0);

		video_feeds.forEach((k, v) -> {
			int ii = i.getAndIncrement();

			if (ii < feeds.length) {
				updateView(feeds[ii], v);
			}

		});

		List<Frame> frames = video_feeds.keySet().stream().collect(Collectors.toList());

		try {
			EffectUtil.applyAdd(frame, CVUtils.convertToMat(DataStoreUtil.parseAsByteFrame(frames.get(count % frames.size()))));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return frame;
	}

	@Override
	void stopped() {
		// TODO Auto-generated method stub

	}

	@Override
	void started() {
		// run the ml ...
		int i = 0;
		// don't use lambdas ... should expect to be computational heavy
		
		
//		video_feeds.forEach((k, v) -> {
//			log.info("encode frame " + k);
//			try {
//				// convert to Mat in order to resize ... 
//				Mat mat = CVUtils.convertToMat(DataStoreUtil.parseAsByteFrame(k));
//				FrameTransform.resize(mat, frame_size);
//				ByteFrame frame = CVUtils.convertToByteFrame(mat,new IFrame.Rect(0, 0, Double.valueOf(frame_size.width).intValue() , Double.valueOf(frame_size.height).intValue()),k.getIndex());
//				frameEncoder.encode((ByteFrame)frame);
//			} catch (IOException e) {
//				log.error(e.getMessage(),e);
//			}
//		});

		Iterator<Frame> it = video_feeds.keySet().iterator();
		
		// TODO : save together with feeds ... in previous step
		
		while(it.hasNext()){
			Frame k = it.next();
			log.info("saving as image : "+k.getName());
			// convert to Mat in order to resize ... 
			Mat mat = CVUtils.convertToMat(DataStoreUtil.parseAsByteFrame(k));
			Mat re_mat = FrameTransform.resize(mat, frame_size);
			File file = Paths.get("store/demo/201764/"+k.getName()+".jpg").toFile();
			
			CVUtils.saveAsFile(re_mat,file.getAbsolutePath());
			
			// DO NOT ENCODE in here .. probably opencv class path clash ... 
//			ByteFrame frame = CVUtils.convertToByteFrame(re_mat,new IFrame.Rect(0, 0, Double.valueOf(re_mat.width()).intValue() , Double.valueOf(re_mat.height()).intValue()),k.getIndex());
//			try {
//				frameEncoder.encode(file);
//			} catch (IOException e) {
//				log.error(e.getMessage(),e);
//			}
		}

	}
}
