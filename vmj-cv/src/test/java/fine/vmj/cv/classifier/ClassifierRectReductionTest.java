package fine.vmj.cv.classifier;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencv.core.Rect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.cv.util.Rects;
import fine.vmj.util.VMJProperties;

public class ClassifierRectReductionTest {

	private static final Logger log = LoggerFactory.getLogger(ClassifierRectReductionTest.class);

	private static int gl_size = 500;
	
	@BeforeClass
	public static void setup(){
		VMJProperties.CLASSIFIER_LOG_ON.setValue(true);
		VMJProperties.CLASSIFIER_LOG_FREQUENCY.setValue(1);
		// test works only with those numbers!!!
		VMJProperties.CLASSIFIER_CYCLE_TIME.setValue(0);
		VMJProperties.CLASSIFIER_CALIBRATION_TIME.setValue(0);
	}
	
	@AfterClass
	public static void teardown(){
		// reset system properties
	}

	private static List<Rect> generateRects(int size, int mean_size) {
		return Arrays.stream(new Rect[size]).map(i -> generateRandomRect(mean_size)).collect(Collectors.toList());
	}

	private static Rect generateRandomRect(int size) {
		int x = ThreadLocalRandom.current().nextInt(0, gl_size);
		int y = ThreadLocalRandom.current().nextInt(0, gl_size);
		int w = ThreadLocalRandom.current().nextInt(size - 50, size + 50);
		int h = ThreadLocalRandom.current().nextInt(size - 50, size + 50);

		return new Rect(x, y, w, h);
	}

	@Test
	public void testAllFramesEqual() {
		
		Classifier.Status status = new Classifier.Status();
		
		// create a lot of rects that are "equal"
		List<Rect> rects = generateRects(11, 4 * gl_size / 5);
		log.info("all_rects ... "+rects.size());
		rects.stream().forEach(r -> log.info(" - "+r));
		// keep only ones that do equal ... 
		List<Rect> _rects = Rects.reduce(rects);
		log.info("un_equal_rects ... "+_rects.size());
		_rects.stream().forEach(r -> log.info(r+" - "+r));
		
		// try them on the update ... make several ... to digest change
		for(int i = 0 ; i <= VMJProperties.CLASSIFIER_CYCLE_TIME.getValue() + VMJProperties.CLASSIFIER_CALIBRATION_TIME.getValue() + 2;i++){
			status.update(_rects.toArray(new Rect[0]));
			log.info(i+" - Rects status:"+status.getRects().size());
		}

		Assert.assertTrue("unique rects should be unique ... "+status.getRects().size()+" vs. "+_rects.size(),status.getRects().size() == _rects.size());
		
		status.update(rects.toArray(new Rect[0]));

		Assert.assertTrue("saturated rects should reduce to uniques ... "+status.getRects().size()+" vs. "+_rects.size(),status.getRects().size() == _rects.size());

		log.info("got "+status.getRects().size()+" in total ... from "+rects.size());

	}

}
