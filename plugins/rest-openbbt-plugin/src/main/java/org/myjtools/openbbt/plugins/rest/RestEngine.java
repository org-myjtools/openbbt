package org.myjtools.openbbt.plugins.rest;

public interface RestEngine {

	void setBaseUrl(String baseUrl);
	void setHttpCodeThreshold(Integer httpCode);
	void setTimeout(Long milliseconds);
	void requestGET(String endpoint);
	void requestPOST(String endpoint);
	void requestPOST(String endpoint, String content);
	void requestPUT(String endpoint, String content);
	void requestPATCH(String endpoint, String content);
	void requestDELETE(String endpoint);
	Integer responseHttpCode();
	String responseBody();

}
