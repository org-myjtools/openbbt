package org.myjtools.openbbt.plugins.gherkin;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.myjtools.openbbt.core.ConfigAdapter;
import org.myjtools.openbbt.core.contributors.ConfigProvider;

/**
 * Configuration provider for the Gherkin plugin. Loads default configuration values from
 * {@code gherkin-config.yaml} bundled in the classpath, which can be overridden by the
 * user's project configuration.
 *
 * <p>The configuration keys are prefixed with {@code gherkin.} and include:
 * <ul>
 *   <li>{@code gherkin.idTagPattern} &mdash; regex pattern for extracting identifiers from tags</li>
 *   <li>{@code gherkin.definitionTag} &mdash; tag marking a feature as a definition</li>
 *   <li>{@code gherkin.implementationTag} &mdash; tag marking a feature as an implementation</li>
 * </ul>
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see GherkinSuiteAssembler
 */
@Extension(scope = Scope.SINGLETON)
public class GherkinConfig extends ConfigAdapter implements ConfigProvider  {

	/** Configuration key for the regex pattern used to extract identifiers from Gherkin tags. */
	public static final String ID_TAG_PATTERN = "idTagPattern";

	/** Configuration key for the tag that marks a feature as a definition. */
	public static final String DEFINITION_TAG = "definitionTag";

	/** Configuration key for the tag that marks a feature as an implementation. */
	public static final String IMPLEMENTATION_TAG = "implementationTag";

	/** {@inheritDoc} */
	@Override
	protected String resource() {
		return "gherkin-config.yaml";
	}

}
