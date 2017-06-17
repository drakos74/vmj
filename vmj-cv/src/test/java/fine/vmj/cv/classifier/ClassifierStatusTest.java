package fine.vmj.cv.classifier;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opencv.core.Rect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.util.VMJProperties;


public class ClassifierStatusTest {

	private static final Logger log = LoggerFactory.getLogger(ClassifierStatusTest.class);

	@BeforeClass
	public static void setup(){
		VMJProperties.CLASSIFIER_LOG_ON.setValue(true);
		VMJProperties.CLASSIFIER_LOG_FREQUENCY.setValue(1);
		// test works only with those numbers!!!
		VMJProperties.CLASSIFIER_CYCLE_TIME.setValue(10000);
		VMJProperties.CLASSIFIER_CALIBRATION_TIME.setValue(100);
	}
	
	@AfterClass
	public static void teardown(){
		// reset system properties
	}
	
	@Test
	public void CountZeroStatus(){
		log.info("CountZeroStatus...");
		int live = 0;
		Classifier.Status status = new Classifier.Status();
		
		for(int i = 0; i < Classifier.Status.reset_counter * 10; i++){
			if(status.isAlive()){
				status.update(new Rect[0]);
				live++;
			}
			Assert.assertTrue(status.getRects().size() == 0);
		}
		Assert.assertTrue("Should not have live more than 10 ["+live+"] - ["+Arrays.toString(status.getRects().toArray())+"]",live < 10);
		
	}
	
	@Test
	public void CountAlwaysRectStatus(){
		log.info("CountAlwaysRectStatus...");
		Classifier.Status status = new Classifier.Status();
		
		for(int i = 0; i < Classifier.Status.reset_counter * 3; i++){
			if(!status.isSleeping()){
				status.update(new Rect[]{new Rect()});
			}
			// needs 2 sleep cycles to be certain ...
			if(i >= Classifier.Status.reset_counter && i < Classifier.Status.reset_counter + Classifier.Status.max_counter + 2/*  it s 2 because we test > only to reset counts ... */){
				Assert.assertFalse("Should have waken up at "+i+" due to "+Classifier.Status.reset_counter,status.isSleeping);
				if(i >= Classifier.Status.reset_counter + Classifier.Status.confidence_threshold + 2 /*  it s 2 because we test > only to reset counts ... */){
					Assert.assertTrue("Should be live now at "+i+" due to "+Classifier.Status.max_counter,status.isLive);
					Assert.assertTrue(status.getRects().size() == 1);
				} else{
					Assert.assertFalse("Should not be live yet at "+i+" due to "+Classifier.Status.max_counter,status.isLive);
				}
			} else{
				if(i <= Classifier.Status.reset_counter * 2 + Classifier.Status.max_counter + 2 || i >= Classifier.Status.reset_counter * 2 + Classifier.Status.max_counter * 2 + 3){
					Assert.assertTrue("Should sleep until "+Classifier.Status.reset_counter+" ["+i+"]",status.isSleeping);
				} else{
					Assert.assertFalse("Should not sleep for too long ... "+Classifier.Status.reset_counter+" ["+i+"]",status.isSleeping);
				}
				if(i >= Classifier.Status.reset_counter + Classifier.Status.confidence_threshold + 2 /*  it s 2 because we test > only to reset counts ... */){
					Assert.assertTrue("Should be live now at "+i+" due to "+Classifier.Status.max_counter,status.isLive);
					Assert.assertTrue(status.getRects().size() == 1);
				} else{
					Assert.assertFalse("Should not be live yet at "+i+" due to "+Classifier.Status.max_counter,status.isLive);
					Assert.assertTrue(status.getRects().size() == 0);
				}
			}
		}
	}
	
	@Test
	public void CountAlternatingRectStatus(){
		log.info("CountAlternatingRectStatus...");
		Classifier.Status status = new Classifier.Status();
		
		for(int i = 0; i < Classifier.Status.reset_counter * 3; i++){
			if(!status.isSleeping()){
				status.update(new Rect[i % 13]);
			}
			Assert.assertTrue("Should never have verified same number of rects ... ["+status.getRects().size()+"] at "+i,status.getRects().size() == 0);

			Assert.assertFalse("Should never have woken up ["+i+"]",status.isLive);
		}
		
	}
	
	private boolean isInRangeI(int i){
		return (i > Classifier.Status.reset_counter * 10 / 3 && i < Classifier.Status.reset_counter * 10 * 2 / 3);
	}
	
}
