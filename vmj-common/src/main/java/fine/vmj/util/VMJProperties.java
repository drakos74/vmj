package fine.vmj.util;

public class VMJProperties implements Properties {
	
	public static final SimpleProperty<Boolean> CAMERA_LOG_ON = new SimpleProperty<Boolean>("vmj.camera.log.on","",true);
	public static final SimpleProperty<Boolean> OBJDETECT_LOG_ON = new SimpleProperty<Boolean>("vmj.objdetect.log.on","",false);
	public static final SimpleProperty<Boolean> FRAMESPACE_LOG_ON = new SimpleProperty<Boolean>("vmj.framespace.log.on","",false);
	public static final SimpleProperty<Boolean> RECTS_LOG_ON = new SimpleProperty<Boolean>("vmj.rects.log.on","",false);

	public static final SimpleProperty<Boolean> CLASSIFIER_LOG_ON = new SimpleProperty<Boolean>("vmj.classifier.log.on","",false);
	public static final SimpleProperty<Integer> CLASSIFIER_LOG_FREQUENCY = new SimpleProperty<Integer>("vmj.classifier.log.frequency","",100);

	public static final SimpleProperty<Boolean> WORKSPACE_LOG_ON = new SimpleProperty<Boolean>("vmj.workspace.log.on","",true);
	public static final SimpleProperty<Boolean> SCREEN_LOG_ON = new SimpleProperty<Boolean>("vmj.screen.log.on","",false);
	public static final SimpleProperty<Boolean> DATASTORE_LOG_ON = new SimpleProperty<Boolean>("vmj.store.log.on","",true);
	
	public static final SimpleProperty<Boolean> MATCHER_LOG_ON = new SimpleProperty<Boolean>("vmj.matcher.log.on","",true);

	public static final SimpleProperty<Integer> LOCAL_CYCLE_SIZE = new SimpleProperty<Integer>("vmj.local.cycle.size","",10_000);

	public static final SimpleProperty<Boolean> OBJDETECT_IS_TEST = new SimpleProperty<Boolean>("vmj.objdetect.is.test","",true);

	public static final SimpleProperty<Integer> CLASSIFIER_CYCLE_TIME = new SimpleProperty<Integer>("vmj.classifier.cycle.time","",5);
	public static final SimpleProperty<Integer> CLASSIFIER_CALIBRATION_TIME = new SimpleProperty<Integer>("vmj.classifier.calibration.time","",3);

	public static final SimpleProperty<Boolean> CAMERA_CAPTURE_ON = new SimpleProperty<Boolean>("vmj.camera.capture.on","",true);

	public static final SimpleProperty<Integer> CYCLE_SIZE = new SimpleProperty<Integer>("vmj.cycle.size","",10_000);
	public static final SimpleProperty<Long> STORE_MAX_BYTE_SIZE = new SimpleProperty<Long>("vmj.store.max.byte.size","",1000_000_000L);//1GB
	public static final SimpleProperty<Integer> STORE_MAX_OBJECT_SIZE = new SimpleProperty<Integer>("vmj.store.max.obj.size","",1000);

	public static final SimpleProperty<Boolean> DATASTORE_LOCAL_CACHE = new SimpleProperty<Boolean>("vmj.store.local.cache","specifies if dataStore will utilize local caching",true);
	public static final SimpleProperty<String> DATASTORE_LOCAL_FILES_EXT = new SimpleProperty<String>("vmj.store.local.files.ext","specifies the local file xtension","vmj");
	public static final SimpleProperty<String> DATASTORE_IMPL = new SimpleProperty<String>("vmj.store.impl","","LOCAL_FRAME_STORE");
	public static final SimpleProperty<String> DATASTORE_LOCAL_DIR = new SimpleProperty<String>("vmj.store.local.dir","","store/demo");

	public static final SimpleProperty<Integer> ML_FRAME_SIZE = new SimpleProperty<Integer>("vmj.ml.frame.size","",10);
	public static final SimpleProperty<Integer> IMG_CHANNEL_NUM = new SimpleProperty<Integer>("vmj.img.channel.num","",3);

}

