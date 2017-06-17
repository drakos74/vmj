package fine.vmj.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.util.VMJProperties;

public class FileIterator {

	private static final Logger log = LoggerFactory.getLogger(FileIterator.class);
	
	private final String filepath;
	private final String ext;
	
	public FileIterator(String... args){
		filepath = args[0];
		ext = args[1];
	}

	public void apply(IOStreamHandler handle) {
		final AtomicInteger i = new AtomicInteger(0);
		try (Stream<Path> paths = Files.walk(Paths.get(filepath))) {
			paths.filter(path -> path.toString().endsWith("." + ext)).forEach(filePath -> {
				if (Files.isRegularFile(filePath)) {
					log.info("loading... " + filePath + " as " + filePath.getFileName());
					handle.apply(new File(filePath.toString()),i.getAndAdd(1));
				}
			});
		} catch (IOException e) {
			log.info("Could not access files in path " + filepath);
		}
	}

}
