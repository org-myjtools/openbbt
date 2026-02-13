package org.myjtools.openbbt.core;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.ExtensionManager;
import org.myjtools.jexten.InjectionProvider;
import org.myjtools.openbbt.core.contributors.ConfigProvider;
import org.myjtools.openbbt.core.contributors.PlanNodeRepositoryFactory;
import org.myjtools.openbbt.core.util.Lazy;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class OpenBBTDependencyInjection implements InjectionProvider {

	private final ExtensionManager extensionManager;
	private final Config config;
	private final ResourceFinder resourceFinder;
	private final PlanNodeRepositoryFactory planNodeRepositoryFactory;
	private final Lazy<PlanNodeRepository> planNodeRepository = Lazy.of(this::createPlanNodeRepository);



	public OpenBBTDependencyInjection(Config configuration) {
		this.extensionManager = ExtensionManager.create().withInjectionProvider(this);
		this.config = extensionManager.getExtensions(ConfigProvider.class)
			.map(ConfigProvider::config)
			.reduce(Config.empty(), Config::append)
			.append(configuration);
		this.planNodeRepositoryFactory = extensionManager.getExtension(PlanNodeRepositoryFactory.class)
			.orElse(null);
		this.resourceFinder = new ResourceFinder(config.get(OpenBBTConfig.PATH, Path::of).orElseThrow(
			()-> new OpenBBTException("Resource path not configured {}: ",OpenBBTConfig.PATH)
		));
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


	private PlanNodeRepository createPlanNodeRepository() {
		return switch (
			config.get(OpenBBTConfig.REPOSITORY_MODE,String.class).orElse(OpenBBTConfig.REPOSITORY_MODE_MEMORY)
		) {
			case OpenBBTConfig.REPOSITORY_MODE_MEMORY ->
					this.planNodeRepositoryFactory.create(PlanNodeRepositoryFactory.Mode.IN_MEMORY);
			case OpenBBTConfig.REPOSITORY_MODE_FILE ->
					this.planNodeRepositoryFactory.create(
							PlanNodeRepositoryFactory.Mode.FILE,
							config.get(OpenBBTConfig.REPOSITORY_FILE, Path::of
					).orElseThrow(
						()-> new OpenBBTException("Repository file path not configured {}: ",OpenBBTConfig.REPOSITORY_FILE)
					));
			case OpenBBTConfig.REPOSITORY_MODE_REMOTE ->
				this.planNodeRepositoryFactory.create(
					PlanNodeRepositoryFactory.Mode.REMOTE,
					config.get(OpenBBTConfig.REPOSITORY_URL, String::toString).orElseThrow(
						()-> new OpenBBTException("Repository remote URL not configured {}: ",OpenBBTConfig.REPOSITORY_URL)
					),
					config.get(OpenBBTConfig.REPOSITORY_USERNAME, String::toString).orElseThrow(
						()-> new OpenBBTException("Repository remote username not configured {}: ",OpenBBTConfig.REPOSITORY_USERNAME)
					),
					config.get(OpenBBTConfig.REPOSITORY_PASSWORD, String::toString).orElseThrow(
							()-> new OpenBBTException("Repository remote password not configured {}: ",OpenBBTConfig.REPOSITORY_USERNAME)
					)
				);
			default ->
				throw new OpenBBTException("Unsupported repository mode: {}, expected: {}",
					config.get(OpenBBTConfig.REPOSITORY_MODE,String.class).orElse(null),
					List.of(OpenBBTConfig.REPOSITORY_MODE_FILE, OpenBBTConfig.REPOSITORY_MODE_MEMORY, OpenBBTConfig.REPOSITORY_MODE_REMOTE)
				);
		};
	}

}
