package org.myjtools.openbbt.plugins.rest;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.openbbt.core.Assertion;
import org.myjtools.openbbt.core.ResourceFinder;
import org.myjtools.openbbt.core.backend.TestCaseExecution;
import org.myjtools.openbbt.core.extensions.Step;
import org.myjtools.openbbt.core.extensions.StepProvider;
import org.myjtools.openbbt.core.testplan.Document;
import org.myjtools.openbbt.plugins.rest.restassured.RestAssuredEngine;

@Extension
public class RestStepProvider implements StepProvider  {

	@Inject
	ResourceFinder resourceFinder;

	private RestEngine restEngine;

	@Override
	public void init(Config config) {
		this.restEngine = new RestAssuredEngine();
		restEngine.setBaseUrl(config.getString("rest.baseURL").orElse(""));
		restEngine.setHttpCodeThreshold(config.getInteger("rest.httpCodeThreshold").orElse(500));
		restEngine.setTimeout(config.getLong("rest.timeout").orElse(10000L));
	}


	// --- request methods ---

	@Step(value = "rest.request.GET", args = {"endpoint:text"})
	public void get(String endpoint) {
		restEngine.requestGET(interpolate(endpoint));
	}

	@Step(value = "rest.request.POST.empty", args = {"endpoint:text"})
	public void post(String endpoint) {
		restEngine.requestPOST(interpolate(endpoint));
	}

	@Step(value = "rest.request.POST.body", args = {"endpoint:text"})
	public void postWithBody(String endpoint, Document body) {
		restEngine.requestPOST(interpolate(endpoint), interpolate(body.content()));
	}

	@Step(value = "rest.request.POST.file", args = {"endpoint:text", "file:text"})
	public void postWithFile(String endpoint, String file) {
		restEngine.requestPOST(interpolate(endpoint), resourceFinder.readAsString(file));
	}

	@Step(value = "rest.request.PUT.body", args = {"endpoint:text"})
	public void putWithBody(String endpoint, Document body) {
		restEngine.requestPUT(interpolate(endpoint), interpolate(body.content()));
	}

	@Step(value = "rest.request.PUT.file", args = {"endpoint:text", "file:text"})
	public void putWithFile(String endpoint, String file) {
		restEngine.requestPUT(interpolate(endpoint), resourceFinder.readAsString(file));
	}

	@Step(value = "rest.request.PATCH.body", args = {"endpoint:text"})
	public void patchWithBody(String endpoint, Document body) {
		restEngine.requestPATCH(interpolate(endpoint), interpolate(body.content()));
	}

	@Step(value = "rest.request.PATCH.file", args = {"endpoint:text", "file:text"})
	public void patchWithFile(String endpoint, String file) {
		restEngine.requestPATCH(interpolate(endpoint), resourceFinder.readAsString(file));
	}

	@Step(value = "rest.request.DELETE", args = {"endpoint:text"})
	public void delete(String endpoint) {
		restEngine.requestDELETE(interpolate(endpoint));
	}


	@Step("rest.response.statusCode")
	public void checkStatusCode(Assertion assertion) {
		Assertion.assertThat(restEngine.responseHttpCode(), assertion);
	}

	@Step("rest.response.body")
	public void checkResponseBody(Document body) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Step(value = "rest.response.body.file", args = {"file:text"})
	public void checkResponseBodyFromFile(String file) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Step("rest.response.body.contains")
	public void checkResponseBodyContains(Document body) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	protected String interpolate(String text) {
		return TestCaseExecution.current().interpolateString(text);
	}

}
