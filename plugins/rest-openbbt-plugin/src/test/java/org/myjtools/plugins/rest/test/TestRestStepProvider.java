package org.myjtools.plugins.rest.test;

import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.plan.Plan;
import org.myjtools.openbbt.core.plan.Project;
import java.util.List;
import java.util.Map;

class TestRestStepProvider {
	
	@Test
	void test() {
		Project project = new Project(
			"Test Project",
			"Project for testing the REST plugin",
			"OpenBBT",
			List.of()
		);
		Config config = Config.ofMap(Map.of(
            "rest.baseUrl", "http://localhost:8080"
		));
		OpenBBTContext context = new OpenBBTContext(project, config, List.of(), "", List.of());
		OpenBBTRuntime runtime = new OpenBBTRuntime(config);
		Plan plan = runtime.buildTestPlan(context);

	}

	
}
