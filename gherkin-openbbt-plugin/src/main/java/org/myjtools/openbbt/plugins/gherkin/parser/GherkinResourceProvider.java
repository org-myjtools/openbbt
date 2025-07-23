package org.myjtools.openbbt.plugins.gherkin.parser;

import java.nio.file.Path;
import java.util.List;

import es.iti.wakamiti.api.contributors.ResourceProvider;
import es.iti.wakamiti.api.resources.*;
import imconfig.Config;
import jexten.*;

@Extension
public class GherkinResourceProvider implements ResourceProvider {

	@Inject
	Config config;

	@Override
	public List<Resource> resources() {
		return new ResourceFinder().findResources(
			"gherkin",
			Path.of(config.get("resources.path",".")),
			"*.feature"
		);
	}

}
