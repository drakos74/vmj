package fine.vmj.cv.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Rects {
	
	private static final Logger log = LoggerFactory.getLogger(Rects.class);
	
	private static final boolean logOn = fine.vmj.util.VMJProperties.RECTS_LOG_ON.getValue();
	
	// TODO : test rects unit operations
	
	private static double scale_factor = 0.75;

	public static boolean equals(Rect rect1, Rect rect2) {

		if(rect1.equals(rect2)){
			return true;
		}
		
		Rect _rect1 = rect1.clone();
		Rects.scale(_rect1,scale_factor);
		Point _point2 = Rects.center(rect2);
		
		if (_rect1.contains(_point2)) {
			if(logOn)log.info("equal:rect["+_rect1+"],point["+_point2+"]");

			return true;
		}
		
		if(logOn)log.info("no_equal:rect["+_rect1+"],point["+_point2+"]");


		Rect _rect2 = rect2.clone();
		Rects.scale(_rect2, scale_factor);
		Point _point1 = Rects.center(rect1);
		if (_rect2.contains(_point1)) {
			if(logOn)log.info("equal:["+rect1+","+rect2+"]");

			return true;
		}
		
		if(logOn)log.info("no_equal:["+rect1+","+rect2+"]");
		return false;
	}
	
	public static Point center(Rect rect){
		return new Point(rect.x + rect.width / 2 , rect.y + rect.height / 2);
	}
	
	public static void scale(Rect rect , double s){
		Point middle = center(rect);
		double w = rect.width * s;
		double h = rect.height * s;
		rect.set(new double[]{middle.x - w / 2,middle.y - h /2,w,h});
	}

	public static Rect merge(Rect rect1, Rect rect2) {
		if(rect1.equals(rect2)){
			return new Rect(rect1.tl(),rect1.br());
		}
		Point tl = new Point(DoubleStream.of(rect1.tl().x, rect2.tl().x).min().getAsDouble(),
				DoubleStream.of(rect1.tl().y, rect2.tl().y).min().getAsDouble());
		Point br = new Point(DoubleStream.of(rect1.br().x, rect2.br().x).max().getAsDouble(),
				DoubleStream.of(rect1.br().y, rect2.br().y).max().getAsDouble());
		return new Rect(tl, br);
	}

	public static List<Rect> tryMerge(List<Rect> rects) {
		if(rects.size() == 1){
			return rects;
		}
		
		if(rects.size() > 2){
			throw new UnsupportedOperationException("not allowed ... "+rects.size());
		}
		
		Rect rect1 = rects.get(0);
		Rect rect2 = rects.get(1);

		if (equals(rect1, rect2)) {
			return Arrays.asList(merge(rect1, rect2));
		}
		return Stream.of(rect1, rect2).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	public static List<Rect> reduce(List<Rect> rects) {

		List<Rect>[] new_rects = new ArrayList[] { (ArrayList<Rect>) rects };

		int this_size = 0;
		int last_size = rects.size();

		final AtomicBoolean first_first = new AtomicBoolean(true);
		while (this_size != last_size) {
			if(logOn)log.info(this_size + " - " + last_size);
			
			last_size = new_rects[0].size();

			final AtomicInteger counter = new AtomicInteger(0);
			new_rects[0] = new_rects[0].stream().collect(Collectors.groupingBy(item -> {
				final int i = counter.getAndIncrement();
				int key = ( (i + (first_first.get() ? 0 : 1)) % 2 == 0) ? i : i - 1;
				if(logOn)log.info(key+" in "+i);
				return ( (i + (first_first.get() ? 0 : 1)) % 2 == 0) ? i : i - 1;
			})).entrySet().stream()
					.flatMap(entry -> {
						if(logOn)log.info("tryMerge at "+entry.getKey()+" ["+entry.getValue().size()+"]");
					return tryMerge(entry.getValue()).stream();	
					})
					.collect(Collectors.toList());
			
			this_size = new_rects[0].size();
			
			first_first.set(!first_first.get());

		}

		return new_rects[0];
	}

}
