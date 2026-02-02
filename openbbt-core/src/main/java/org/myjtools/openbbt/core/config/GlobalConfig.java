package org.myjtools.openbbt.core.config;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.myjtools.openbbt.core.contributors.ConfigProvider;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
@Extension(scope = Scope.LOCAL, name = "global")
public class GlobalConfig extends ConfigAdapter implements ConfigProvider  {

	public static final String ID_TAG_PATTERN = "idTagPattern";
	public static final String DEFINITION_TAG = "definitionTag";
	public static final String IMPLEMENTATION_TAG = "implementationTag";

	@Override
	protected String resource() {
		return "global-config.yaml";
	}

}
