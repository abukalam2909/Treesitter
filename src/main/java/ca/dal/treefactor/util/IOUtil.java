package ca.dal.treefactor.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class IOUtil {

	private IOUtil(){}

	/**
	 * 
	 * Copied from <a href="https://www.baeldung.com/java-copy-directory">Baeldung</a>, accessed 7th October 2024.
	 * <br>
	 * @param sourceDirectoryLocation
	 * @param destinationDirectoryLocation
	 * @throws IOException
	 */
	public static void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation) 
			  throws IOException {
		Stream<Path> files = Files.walk(Paths.get(sourceDirectoryLocation));
		try {
			files.forEach(source -> {
		          Path destination = Paths.get(destinationDirectoryLocation, source.toString()
		            .substring(sourceDirectoryLocation.length()));
		          try {
		              Files.copy(source, destination);
		          } catch (IOException e) {
		              e.printStackTrace();
		          }
		      });	
		} finally {
			files.close();
		}
	}

}
