package org.myjtools.openbbt.core.util;

import org.myjtools.openbbt.core.OpenBBTException;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;



public class Hash {

	private static final Base64.Encoder encoder = Base64.getEncoder();

	private static MessageDigest newDigest() {
		try {
			return MessageDigest.getInstance("SHA3-256");
		} catch (NoSuchAlgorithmException e) {
			throw new OpenBBTException(e, "Error obtaining hash algorithm");
		}
	}

	/**
	 * Creates a new Hash instance from the given string
	 * @param content the string to hash
	 * @return a new hash string
	
 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
	public static String of(String content) {
		var bytes = newDigest().digest(content.getBytes(StandardCharsets.UTF_8));
		return encoder.encodeToString(bytes);
	}


	/**
	 * Creates a new Hash instance from the content of the given URI.
	 * The content is read from the URI and hashed using SHA3-256 algorithm.
	 * @param uri the URI to read and hash
	 * @return a new hash string
	 */
	public static String of(URI uri) {
		try (var stream = new DigestInputStream(uri.toURL().openStream(),newDigest())) {
			return encoder.encodeToString(stream.getMessageDigest().digest());
		} catch (IOException e) {
			throw new OpenBBTException(e,"Cannot calculate hash of {resource}",uri);
		}
	}





}
