package org.myjtools.openbbt.core;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.ExtensionManager;
import org.myjtools.jexten.InjectionProvider;
import org.myjtools.jexten.ModuleLayerProvider;
import org.myjtools.openbbt.core.contributors.ConfigProvider;
import org.myjtools.openbbt.core.contributors.PlanNodeRepositoryFactory;
import org.myjtools.openbbt.core.contributors.SuiteAssembler;
import org.myjtools.openbbt.core.plan.NodeType;
import org.myjtools.openbbt.core.plan.PlanNode;
import org.myjtools.openbbt.core.plan.PlanNodeID;
import org.myjtools.openbbt.core.project.TestSuite;
import org.myjtools.openbbt.core.util.Lazy;
import org.myjtools.openbbt.core.util.Log;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class OpenBBTContextManager implements InjectionProvider {


	private static final Log log = Log.of();

	private final ExtensionManager extensionManager;
	private final OpenBBTPluginManager pluginManager;
	private final Config config;
	private final ResourceFinder resourceFinder;
	private final PlanNodeRepositoryFactory planNodeRepositoryFactory;
	private final Lazy<PlanNodeRepository> planNodeRepository = Lazy.of(this::createPlanNodeRepository);




	public OpenBBTContextManager(Config configuration) {
		this.pluginManager = new OpenBBTPluginManager(configuration);
		this.extensionManager = ExtensionManager
			.create(ModuleLayerProvider.compose(ModuleLayerProvider.boot(),pluginManager.moduleLayerProvider()))
			.withInjectionProvider(this);
		this.config = extensionManager.getExtensions(ConfigProvider.class)
			.map(ConfigProvider::config)
			.reduce(Config.empty(), Config::append)
			.append(configuration);
		this.planNodeRepositoryFactory = extensionManager.getExtension(PlanNodeRepositoryFactory.class)
			.orElse(null);
		this.resourceFinder = new ResourceFinder(config.get(OpenBBTConfig.RESOURCE_PATH, Path::of).orElseThrow(
			()-> new OpenBBTException("Resource path not configured {}: ",OpenBBTConfig.RESOURCE_PATH)
		));
	}



	public Optional<PlanNodeID> assembleTestPlan(OpenBBTContext context) {

		List<SuiteAssembler> assemblers = extensionManager.getExtensions(SuiteAssembler.class).toList();
		if (assemblers.isEmpty()) {
			log.warn("No SuiteAssembler found, cannot assemble test plan");
			return Optional.empty();
		}

		List<PlanNodeID> nodes = new ArrayList<>();

		for (String suiteName : context.testSuites()) {
			TestSuite testSuite = context.testSuite(suiteName).orElseThrow(
				() -> new OpenBBTException("Test suite not found in project: {}", suiteName)
			);
			for (SuiteAssembler assembler : assemblers) {
				assembler.assembleSuite(testSuite).ifPresent(nodes::add);
			}
		}
		if (nodes.isEmpty()) {
			log.warn("No test plan nodes assembled for test suites: {}", context.testSuites());
			return Optional.empty();
		}

		PlanNode root = new PlanNode(NodeType.TEST_PLAN);
		root.name("Test Plan");
		var rootID = getPlanNodeRepository().persistNode(root);
		for (PlanNodeID nodeId : nodes) {
			getPlanNodeRepository().attachChildNodeLast(rootID, nodeId);
		}
		return Optional.ofNullable(rootID);

	}


	@Override
	public Stream<Object> provideInstancesFor(Class<?> type, String name) {
		if (type == Config.class) {
			if (name == null || name.isEmpty()) {
				return Stream.of(config);
			} else {
				return Stream.of(config.inner(name));
			}
		} else if (type == ResourceFinder.class) {
			return Stream.of(resourceFinder);
		} else if (type == PlanNodeRepository.class) {
			if (planNodeRepositoryFactory == null) {
				return Stream.empty();
			}
			return Stream.of(planNodeRepository.get());
		}
		return Stream.empty();
	}


	public <T> Stream<T> getExtensions(Class<T> type) {
		return extensionManager.getExtensions(type);
	}

	public PlanNodeRepository getPlanNodeRepository() {
		if (planNodeRepositoryFactory == null) {
			throw new OpenBBTException("No PlanNodeRepositoryFactory found, cannot create PlanNodeRepository");
		}
		return planNodeRepository.get();
	}

	private PlanNodeRepository createPlanNodeRepository() {
		if (planNodeRepositoryFactory == null) {
			log.warn("No PlanNodeRepositoryFactory found, cannot create PlanNodeRepository");
			return null;
		}
		return planNodeRepositoryFactory.createPlanNodeRepository();
	}


}
