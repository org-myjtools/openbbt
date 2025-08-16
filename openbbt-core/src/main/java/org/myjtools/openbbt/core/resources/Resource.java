package org.myjtools.openbbt.core.resources;

import org.myjtools.openbbt.core.util.Hash;
import org.myjtools.openbbt.core.util.Lazy;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.function.Supplier;


public class Resource {

	private final String contentType;
	private final URI URI;
	private final Path relativePath;
	private final Supplier<InputStream> reader;
	private final Lazy<String> hash;


	public Resource(
		String contentType,
		URI URI,
		Path relativePath,
		Supplier<InputStream> reader
	) {
		this.contentType = contentType;
		this.URI = URI;
		this.relativePath = relativePath;
		this.reader = reader;
		this.hash = Lazy.of(() -> Hash.of(URI));
	}


	public String contentType() {
		return contentType;
	}


	public InputStream open() {
		return reader.get();
	}

	public String hash() {
		return hash.get();
	}


	public URI URI() {
		return URI;
	}


	public Path relativePath() {
		return relativePath;
	}

}
