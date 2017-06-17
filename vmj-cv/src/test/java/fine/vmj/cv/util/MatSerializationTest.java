package fine.vmj.cv.util;

import java.io.File;
import java.nio.file.Paths;

import org.junit.Test;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;

public class MatSerializationTest {

	static File file = Paths.get("src/test/resources/Poli.png").toFile();
	public static final String img_path = "/Users/drakos/Projects/dr-eek/free-servers/selfie/VMJ/vmj-cv/src/test/resources/MANIFOLD 2012 Photo memories.png";

	static
	{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	
	public static void main(String... args) {
		new MatSerializationTest().testMatToBytesAndBack();
	}

	public static Mat generateImg() {
		System.out.println("Welcome to OpenCV " + Core.VERSION);
		Mat m = new Mat(5, 10, CvType.CV_8UC1, new Scalar(0));
		System.out.println("OpenCV Mat: " + m);
		Mat mr1 = m.row(1);
		mr1.setTo(new Scalar(1));
		Mat mc5 = m.col(5);
		mc5.setTo(new Scalar(5));
		System.out.println("OpenCV Mat data:\n" + m.dump());
		return m;
	}

	public static Mat loadImg() {
		try {
			System.out.println(file.getAbsolutePath()+" - exists:"+file.exists());
			Mat img = Imgcodecs.imread(file.getAbsolutePath() /* , Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE */ );
			System.out.println(img.width() + "," + img.height());
			return img;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Test
	public void testMatToBytesAndBack() {
		Mat mat = loadImg();// generateImg();
		System.out.println("serialise ... ");
		byte[] bytes = CVUtils.mat2Bytes(mat);
		System.out.println("did seriealise : "+bytes.length);
//		Mat mad = CVUtils.bytes2Mat(bytes);
		
		Mat mad = new Mat(mat.width(), mat.height(), CvType.CV_8UC3);
		mat.put(0, 0, bytes);
		
		System.out.println(mad.width()+","+mad.height());
	}

}
