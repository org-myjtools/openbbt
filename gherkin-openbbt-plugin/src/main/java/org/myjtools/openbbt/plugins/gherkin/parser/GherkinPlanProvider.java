package org.myjtools.openbbt.plugins.gherkin.parser;


import org.myjtools.gherkinparser.DefaultKeywordMapProvider;
import org.myjtools.gherkinparser.GherkinParser;
import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.jexten.PostConstruct;
import org.myjtools.openbbt.api.PlanNodeID;
import org.myjtools.openbbt.api.Resource;
import org.myjtools.openbbt.api.contributors.PlanProvider;
import org.myjtools.openbbt.api.persistence.PlanNodeRepository;
import org.myjtools.openbbt.api.util.Log;

import java.util.*;

@Extension
public class GherkinPlanProvider implements PlanProvider {

	private static final Log log = Log.of("plugins.gherkin");

	@Inject("openbbt.config")
	Config config;

	@Inject
	PlanNodeRepository repository;

	@Inject
	PlanSerializer debugSerializer;

	private DefaultKeywordMapProvider keywordMapProvider;
	private GherkinParser parser;
	private String idTagPattern;
	private String definitionTag;
	private String implementationTag;
	private final Map<UUID,Object> underlyingModels = new HashMap<>();


	@PostConstruct
	public void init() {
		this.keywordMapProvider = new DefaultKeywordMapProvider();
		this.parser = new GherkinParser(keywordMapProvider);
		this.idTagPattern = config.getString(GherkinConfig.ID_TAG_PATTERN).orElseThrow();
		this.definitionTag = config.getString(GherkinConfig.DEFINITION_TAG).orElseThrow();
		this.implementationTag = config.getString(GherkinConfig.IMPLEMENTATION_TAG).orElseThrow();
	}


	@Override
	public boolean accept(Resource resource) {
		return "gherkin".equals(resource.contentType());
	}


	@Override
	public Optional<PlanNodeID> providePlan(List<Resource> resources) {
		if (resources.size() == 1) {
			return provideSingleFeature(resources.getFirst());
		} else {
			return provideMultipleFeature(resources);
		}
	}


	private Optional<PlanNodeID> provideSingleFeature(Resource resource) {
		return buildFeatureNode(
			keywordMapProvider,
			parser,
			idTagPattern,
			resource
		).map(PlanNodeDAO::nodeID);
	}



	private Optional<PlanNodeID> provideMultipleFeature(List<Resource> resources) {

		var root = provideStandaloneFeatures(resources);
		deleteDefinitionTestCasesWithoutId(root);
		deleteImplementationScenarioOutlineContent(root);
		fillImplementationScenarioOutlines(root);

		// redefine definition test cases with implementation steps
		root.findDescendants(and(withNodeType(NodeType.TEST_CASE),withTag(definitionTag)))
			.forEach(it -> redefine(root,it));

		root.getChildren(node ->
			node.hasProperty(GHERKIN_TYPE,GHERKIN_TYPE_FEATURE) &&
			node.hasTag(implementationTag)
		).forEach(PlanNodeDAO::delete);
		logTree(root);

		root.findDescendants(or(withTag(definitionTag),withTag(implementationTag)))
			.forEach( it-> {
				it.node().tags().remove(implementationTag);
				it.node().tags().remove(definitionTag);
				it.update();
			});
		logTree(root);

		return normalize(root);

	}


	private void deleteImplementationScenarioOutlineContent(PlanNodeDAO root) {
		log.debug("deleteImplementationScenarioOutlineContent");
		root.findDescendants(and(
			withProperty(GHERKIN_TYPE, GHERKIN_TYPE_SCENARIO_OUTLINE),
			withTag(implementationTag)
		),2).forEach(PlanNodeDAO::deleteChildren);
		logTree(root);
	}


	private Optional<PlanNodeID> normalize(PlanNodeDAO root) {
		if (root.getChildrenCount() == 1) {
			var child = repository.getChildren(root.nodeID()).get(0);
			repository.detachChild(root.nodeID(), child);
			repository.delete(root.nodeID());
			return Optional.of(child);
		} else {
			return Optional.of(root.nodeID());
		}
	}


	private PlanNodeDAO provideStandaloneFeatures(List<Resource> resources) {
		log.debug("provideStandaloneFeatures");
		var root = PlanNodeDAO.persist(repository, new PlanNode(NodeType.AGGREGATOR));
		for (Resource resource : resources) {
			buildFeatureNode(
				keywordMapProvider,
				parser,
				idTagPattern,
				resource
			).ifPresent(root::attachChild);
		}
		logTree(root);
		return root;
	}


	private void deleteDefinitionTestCasesWithoutId(PlanNodeDAO root) {
		root.findDescendants(and(
			withTag(definitionTag),
			withNodeType(NodeType.TEST_CASE),
			withField("id", null)
		)).forEach(PlanNodeDAO::delete);
	}


	private void fillImplementationScenarioOutlines(PlanNodeDAO root) {
		log.debug("fillImplementationScenarioOutlines");
		root.findDescendants(and(
			withNodeType(NodeType.AGGREGATOR),
			withTag(implementationTag),
			withProperty(GHERKIN_TYPE,GHERKIN_TYPE_SCENARIO_OUTLINE)
		)).forEach(scenarioOutline -> fillImplementationScenarioOutlines(root,scenarioOutline));
		logTree(root);
	}



	private void fillImplementationScenarioOutlines(PlanNodeDAO root, PlanNodeDAO impScenarioOutline) {

		PlanNodeDAO impFeature = impScenarioOutline.getParent().orElseThrow();

		PlanNodeDAO defScenarioOutline = root.findDescendants(and(
			withNodeType(NodeType.AGGREGATOR),
			withTag(definitionTag),
			withProperty(GHERKIN_TYPE,GHERKIN_TYPE_SCENARIO_OUTLINE),
			withField("id",impScenarioOutline.node().id())
		)).findFirst().orElseThrow(
			() -> new WakamitiException(
				"There is no definition scenario outline with id {id}",
				impScenarioOutline.node().id()
			)
		);

		Examples defExamples = ((ScenarioOutline) underlyingModels.get(defScenarioOutline.nodeID()))
			.examples()
			.get(0);

		var stepsFromExamples = new FeaturePlanBuilder(
			(Feature) underlyingModels.get(impFeature.nodeID()),
			impFeature.node().source(),
			keywordMapProvider,
			idTagPattern,
			repository
		).createScenariosFromExamples(
			(ScenarioOutline) underlyingModels.get(impScenarioOutline.nodeID()),
			defExamples,
			impScenarioOutline
		);

		stepsFromExamples.forEach(impScenarioOutline::attachChild);

	}



	private void redefine(PlanNodeDAO root, PlanNodeDAO defTestCase) {
		log.debug("redefine {}",defTestCase.node().id());
		implementationTestCase(root,defTestCase).ifPresentOrElse(
			impTestCase -> redefineTestCase(defTestCase, impTestCase),
			defTestCase::delete
		);
		logTree(root);
	}


	private void redefineTestCase(PlanNodeDAO defTestCase, PlanNodeDAO impTestCase) {

		// definition background is ignored
		deleteBackground(defTestCase);

		int[] stepMap = extractStepMap(defTestCase, impTestCase);

		var impSteps = impTestCase.getChildren(it -> it.nodeType() == NodeType.STEP);

		int defStepCount = 0;
		int impStepCount = 0;

		for (var defStep : defTestCase.getChildren()) {
			for (int i = 0; i < stepMap[defStepCount]; i++) {
				var impStep = impSteps.get(impStepCount);
				impStep.getParent().ifPresent(impStepParent -> impStepParent.detachChild(impStep));
				defStep.attachChild(impStep);
				impStepCount++;
			}
			redefineStepNodeType(stepMap, defStepCount, defStep);
			defStepCount++;
		}

		moveBackroundToOtherTestCase(impTestCase, defTestCase);

	}



	private void redefineStepNodeType(int[] stepMap, int defStepCount, PlanNodeDAO defStep) {
		if (stepMap[defStepCount] == 0) {
			defStep.node().nodeType(NodeType.VIRTUAL_STEP);
		} else {
			defStep.node().nodeType(NodeType.STEP_AGGREGATOR);
		}
		defStep.update();
	}


	private void moveBackroundToOtherTestCase(PlanNodeDAO origin,PlanNodeDAO target) {
		origin.getChildren().stream()
			.filter(it -> it.node().hasProperty(GHERKIN_TYPE, GHERKIN_TYPE_BACKGROUND))
			.findFirst()
			.ifPresent(originBackground -> {
				origin.detachChild(originBackground);
				target.attachChild(originBackground,0); // background always in first position
				originBackground.node().name("<definition>");
				originBackground.update();
			});
	}


	private int[] extractStepMap(PlanNodeDAO defTestCase, PlanNodeDAO impTestCase) {
		// step map is in form: x-x-x-x...
		String stepMapProperty = impTestCase.node().properties().get(STEP_MAP);
		if (stepMapProperty == null || stepMapProperty.isBlank()) {
			// if not defiend, map 1-to-1 for each step
			stepMapProperty = "-1".repeat(defTestCase.getChildrenCount()).substring(1);
		}
		return Stream.of(stepMapProperty.split("-")).mapToInt(Integer::parseInt).toArray();
	}


	private void deleteBackground(PlanNodeDAO defTestCase) {
		for (var child : defTestCase.getChildren()) {
			if (child.node().hasProperty(GHERKIN_TYPE, GHERKIN_TYPE_BACKGROUND)) {
				child.delete();
			}
		}
	}


	private Optional<PlanNodeDAO> implementationTestCase(
		PlanNodeDAO root,
		PlanNodeDAO definitionTestCase
	) {
		return root.findDescendants(and(
			withNodeType(NodeType.TEST_CASE),
			withTag(implementationTag),
			withField("id",definitionTestCase.node().id())
		)).findFirst();
	}


	private void logTree(PlanNodeDAO root) {
		log.debug("{}", ()->debugSerializer.serializeTreeToString(root.nodeID()));
	}


	private Optional<PlanNodeDAO> buildFeatureNode(
		DefaultKeywordMapProvider keywordMapProvider,
		GherkinParser parser,
		String idTagPattern,
		Resource resource
	) {
		try (var inputStream = resource.open()) {
			var gherkinDocument = parser.parse(inputStream);
			if (gherkinDocument.feature() == null) {
				return Optional.empty();
			}
			var builder = new FeaturePlanBuilder(
				gherkinDocument.feature(),
				resource.relativePath().toString(),
				keywordMapProvider,
				idTagPattern,
				repository
			);
			var result = Optional.ofNullable(builder.createTestPlan());
			this.underlyingModels.putAll(builder.underlyingModels());
			return result;
		} catch (RuntimeException | IOException e) {
			log.error(e,"Cannot read resource {resource}",resource.relativePath());
			return Optional.empty();
		}

	}


}
