package org.myjtools.openbbt.plugins.gherkin.test;

import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.myjtools.jexten.ExtensionManager;
import org.myjtools.jexten.Inject;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTDependencyInjection;
import org.myjtools.openbbt.core.PlanNodeRepository;
import org.myjtools.openbbt.core.ResourceFinder;
import org.myjtools.openbbt.core.contributors.PlanAssembler;
import org.myjtools.openbbt.core.persistence.DataSourceProvider;
import org.myjtools.openbbt.core.persistence.JooqRepository;
import org.myjtools.openbbt.core.plan.PlanNodeID;
import org.myjtools.openbbt.core.plan.TagExpression;
import org.myjtools.openbbt.plugins.gherkin.GherkinPlanAssembler;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;

class GherkinPlanAssemblerTest {

	@Test
	void test() {
		OpenBBTDependencyInjection di = new OpenBBTDependencyInjection(Config.ofMap(Map.of(
			OpenBBTConfig.PATH, "src/test/resources/test-features",
			OpenBBTConfig.REPOSITORY_MODE, OpenBBTConfig.REPOSITORY_MODE_MEMORY
		)));
		var planAssembler = di.getExtensions(PlanAssembler.class).findFirst().orElseThrow();
		PlanNodeID planID = planAssembler.assemblePlan(TagExpression.EMPTY).orElseThrow();

	}
}
