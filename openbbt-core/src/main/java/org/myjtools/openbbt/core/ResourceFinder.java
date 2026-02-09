package org.myjtools.openbbt.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;



/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public class ResourceFinder {

	private final Path startingPath;

	public ResourceFinder(Path startingPath) {
		this.startingPath = startingPath;
	}

	public ResourceSet findResources(String globPattern) {
		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:"+globPattern);
		try (var stream = Files.walk(startingPath)) {
			var resources = stream.filter(Files::isRegularFile)
				.filter(path -> pathMatcher.matches(path.getFileName()))
				.map(file -> new Resource(file.toUri(), file, ()->newReader(file)))
				.toList();
			return new ResourceSet(resources);
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