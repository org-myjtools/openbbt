package org.myjtools.openbbt.core;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.ExtensionManager;
import org.myjtools.jexten.InjectionProvider;
import org.myjtools.jexten.ModuleLayerProvider;
import org.myjtools.openbbt.core.contributors.ConfigProvider;
import org.myjtools.openbbt.core.contributors.RepositoryFactory;
import org.myjtools.openbbt.core.messages.MessageProvider;
import org.myjtools.openbbt.core.messages.Messages;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.persistence.Repository;
import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.core.testplan.PlanBuilder;
import org.myjtools.openbbt.core.util.Lazy;
import org.myjtools.openbbt.core.util.Log;
import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.Stream;

public class OpenBBTRuntime implements InjectionProvider {


	private static final Log log = Log.of();

	private final Clock clock;
	private final ExtensionManager extensionManager;
	private final OpenBBTPluginManager pluginManager;
	private final Config config;
	private final PlanBuilder planBuilder;
	private final ResourceFinder resourceFinder;
	private final ResourceSet resourceSet;
	private final RepositoryFactory repositoryFactory;
	private final Lazy<TestPlanRepository> planNodeRepository = Lazy.of(() -> createRepository(TestPlanRepository.class));


	public OpenBBTRuntime(Config configuration) {
		this(configuration, Instant::now);
	}


	public OpenBBTRuntime(Config configuration, Clock clock) {
		this.clock = clock;
		this.pluginManager = new OpenBBTPluginManager(configuration);
		this.extensionManager = ExtensionManager
			.create(ModuleLayerProvider.compose(ModuleLayerProvider.boot(),pluginManager.moduleLayerProvider()))
			.withInjectionProvider(this);
		this.config = extensionManager.getExtensions(ConfigProvider.class)
			.map(ConfigProvider::config)
			.reduce(Config.empty(), Config::append)
			.append(configuration);
		this.repositoryFactory = extensionManager.getExtension(RepositoryFactory.class)
			.orElse(null);
		this.resourceFinder = new ResourceFinder(config.get(OpenBBTConfig.RESOURCE_PATH, Path::of).orElseThrow(
			()-> new OpenBBTException("Resource path not configured {}: ",OpenBBTConfig.RESOURCE_PATH)
		));
		this.resourceSet = resourceFinder.findResources(configuration().getString(OpenBBTConfig.RESOURCE_FILTER).orElseThrow(
			()-> new OpenBBTException("Resource filter not configured {}: ",OpenBBTConfig.RESOURCE_FILTER)
		));
		this.planBuilder = new PlanBuilder(this);
	}


	public Config configuration() {
		return config;
	}

	public Clock clock() {
		return clock;
	}




	@Override
	public Stream<Object> provideInstancesFor(Class<?> type, String name) {
		if (type == Config.class) {
			if (name == null || name.isEmpty()) {
				return Stream.of(config);
			} else {
				return Stream.of(config.inner(name));
			}
		} else if (type == TestPlanRepository.class) {
			if (repositoryFactory == null) {
				return Stream.empty();
			}
			return Stream.of(planNodeRepository.get());
		} else if (type == Messages.class) {
			return Stream.of(Messages.of(
					getExtensions(MessageProvider.class).filter(it -> it.providerFor(name)).toList()
			));
		} else if (type == ResourceFinder.class) {
			return Stream.of(resourceFinder);
		} else if (type == ResourceSet.class) {
			return Stream.of(resourceSet);
		} else if (type == Clock.class) {
			return Stream.of(clock);
		}
		return Stream.empty();
	}


	public <T> Stream<T> getExtensions(Class<T> type) {
		return extensionManager.getExtensions(type);
	}


	public <T extends Repository> T getRepository(Class<?> type) {
		if (repositoryFactory == null) {
			throw new OpenBBTException("No PlanRepositoryFactory found, cannot create PlanRepository");
		}
		if (type == TestPlanRepository.class) {
			return (T) planNodeRepository.get();
		} else {
			throw new OpenBBTException("Unsupported repository type requested: {}", type.getSimpleName());
		}
	}


	private <T extends Repository> T createRepository(Class<T> type) {
		if (repositoryFactory == null) {
			log.warn("No RepositoryFactory found, cannot create {}", type.getSimpleName());
			return null;
		}
		return repositoryFactory.createRepository(type);
	}


	public ResourceSet resourceSet() {
		return resourceSet;
	}


	public TestPlan buildTestPlan(OpenBBTContext context) {
		return planBuilder.buildTestPlan(context);
	}


}
