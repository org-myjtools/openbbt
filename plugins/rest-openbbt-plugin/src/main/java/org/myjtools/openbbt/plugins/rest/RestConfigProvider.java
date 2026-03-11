package org.myjtools.openbbt.plugins.rest;

import org.myjtools.jexten.Extension;
import org.myjtools.openbbt.core.ConfigAdapter;
import org.myjtools.openbbt.core.contributors.ConfigProvider;

@Extension
public class RestConfigProvider extends ConfigAdapter implements ConfigProvider {
	
	@Override
	protected String resource() {
		return "config.yaml";
	}

}
