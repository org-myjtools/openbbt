package org.myjtools.openbbt.core.resources;

import org.myjtools.openbbt.core.OpenBBTException;

import java.io.*;
import java.nio.file.*;
import java.util.List;



/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public class ResourceFinder {


	public List<Resource> findResources(String contentType, Path startingPath, String globPattern) {
		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:"+globPattern);
		try (var stream = Files.walk(startingPath)) {
			return stream.filter(Files::isRegularFile)
				.filter(path -> pathMatcher.matches(path.getFileName()))
				.map(file -> new Resource(
					contentType,
					file.toUri(),
					file,
					()->newReader(file)
				))
				.toList();
		} catch (IOException e) {
			throw new OpenBBTException(e,"Error reading resources from {}",startingPath);
		}
	}


	private InputStream newReader(Path path) {
		Path absolutePath = path.toAbsolutePath();
	   try {
		   return Files.newInputStream(absolutePath);
	   } catch (IOException e) {
		   throw new OpenBBTException(e,"Cannot read file {}",absolutePath);
	   }
	}

}