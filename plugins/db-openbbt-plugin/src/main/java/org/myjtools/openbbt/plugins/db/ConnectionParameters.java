package org.myjtools.openbbt.plugins.db;

public record ConnectionParameters(
	String url,
	String username,
	String password,
	String driver,
	String schema,
	String catalog,
	String dialect
) {
}
