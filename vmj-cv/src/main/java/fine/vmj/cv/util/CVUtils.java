package fine.vmj.cv.util;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.store.data.ByteFrame;
import fine.vmj.store.data.DataStoreUtil;
import fine.vmj.store.data.IFrame;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

/**
 * Provide general purpose methods for handling OpenCV-JavaFX data conversion.
 * Moreover, expose some "low level" methods for matching few JavaFX behavior.
 *
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @author <a href="http://max-z.de">Maximilian Zuleger</a>
 * @version 1.0 (2016-09-17)
 * @since 1.0
 * 
 */
public final class CVUtils
{
	
	private static final Logger log = LoggerFactory.getLogger(CVUtils.class);

	/**
	 * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
	 *
	 * @param frame
	 *            the {@link Mat} representing the current frame
	 * @return the {@link Image} to show
	 */
	public static Image mat2Image(Mat frame)
	{
		try
		{
			return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
		}
		catch (Exception e)
		{
			System.err.println("Cannot convert the Mat obejct: " + e);
			return null;
		}
	}
	
	/**
	 * Generic method for putting element running on a non-JavaFX thread on the
	 * JavaFX thread, to properly update the UI
	 * 
	 * @param property
	 *            a {@link ObjectProperty}
	 * @param value
	 *            the value to set for the given {@link ObjectProperty}
	 */
	public static <T> void onCVThread(final ObjectProperty<T> property, final T value)
	{
		Platform.runLater(() -> {
			property.set(value);
		});
	}
	
	/**
	 * Support for the {@link mat2image()} method
	 * 
	 * @param original
	 *            the {@link Mat} object in BGR or grayscale
	 * @return the corresponding {@link BufferedImage}
	 */
	private static BufferedImage matToBufferedImage(Mat original)
	{
		// init
		BufferedImage image = null;
		int width = original.width(), height = original.height(), channels = original.channels();
		byte[] sourcePixels = new byte[width * height * channels];
		original.get(0, 0, sourcePixels);
		
		if (original.channels() > 1)
		{
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		}
		else
		{
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
		
		return image;
	}
	
	public static Mat convertToMat(ByteFrame frame){
		byte[] bytes = frame.getBytes();
		Mat mat = CVUtils.bytes2Mat(bytes, frame.getRect().getWidth(),frame.getRect().getHeight(), frame.getType());
		return mat;
	}
	
	public static ByteFrame convertToByteFrame(Mat mat, Rect _rect , int l){
		return convertToByteFrame(mat,new IFrame.Rect(_rect.x,_rect.y,_rect.width,_rect.height),l);
	}
	
	public static ByteFrame convertToByteFrame(Mat mat, IFrame.Rect _rect , int l){
		return new ByteFrame(CVUtils.mat2Bytes(mat),_rect,l,mat.type());	
	}
	
	public static byte[] mat2Bytes(Mat mat){
		 int length = (int) (mat.total() * mat.elemSize());
         byte buffer[] = new byte[length];
         mat.get(0, 0, buffer);
         return buffer;
	}
	
	public static Mat bytes2Mat(byte[] bytes , int width , int height , int type){
//		Mat mat = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
		Mat mat = new Mat(height, width, type);
        mat.put(0, 0, bytes);
		return mat;
	}
	
	public static void saveAsFile(Mat img , String file){
		log.info(file);
		Imgcodecs.imwrite(file, img);
	}
	
	
}
