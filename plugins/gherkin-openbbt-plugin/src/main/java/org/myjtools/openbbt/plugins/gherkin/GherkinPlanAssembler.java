package org.myjtools.openbbt.plugins.gherkin;


import org.myjtools.gherkinparser.DefaultKeywordMapProvider;
import org.myjtools.gherkinparser.GherkinParser;
import org.myjtools.gherkinparser.elements.Examples;
import org.myjtools.gherkinparser.elements.Feature;
import org.myjtools.gherkinparser.elements.ScenarioOutline;
import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.jexten.PostConstruct;
import org.myjtools.openbbt.core.*;
import org.myjtools.openbbt.core.contributors.PlanAssembler;
import org.myjtools.openbbt.core.plan.NodeType;
import org.myjtools.openbbt.core.plan.PlanNode;
import org.myjtools.openbbt.core.plan.PlanNodeID;
import org.myjtools.openbbt.core.util.Log;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import static org.myjtools.openbbt.plugins.gherkin.GherkinConstants.*;

@Extension
public class GherkinPlanAssembler implements PlanAssembler {

	private static final Log log = Log.of("plugins.gherkin");
	private static final String STEP_MAP = "step-map";

	@Inject("gherkin")
	Config config;

	@Inject
	PlanNodeRepository repository;

	@Inject
	ResourceFinder resourceFinder;

	private DefaultKeywordMapProvider keywordMapProvider;
	private GherkinParser parser;
	private String idTagPattern;
	private String definitionTag;
	private String implementationTag;
	private final Map<PlanNodeID,Object> underlyingModels = new HashMap<>();


	@PostConstruct
	public void init() {
		this.keywordMapProvider = new DefaultKeywordMapProvider();
		this.parser = new GherkinParser(keywordMapProvider);
		this.idTagPattern = config.getString(GherkinConfig.ID_TAG_PATTERN).orElseThrow();
		this.definitionTag = config.getString(GherkinConfig.DEFINITION_TAG).orElseThrow();
		this.implementationTag = config.getString(GherkinConfig.IMPLEMENTATION_TAG).orElseThrow();
	}


	@Override
	public Optional<PlanNodeID> assemblePlan(org.myjtools.openbbt.core.plan.TagExpression tagExpression) {
		return Optional.empty();
/*		ResourceSet resourceSet = resourceFinder.findResources("*.feature");
		if (resourceSet.size() == 1) {
			return assembleFeatureNode(keywordMapProvider, parser, idTagPattern, resourceSet.get(0));
		} else {
			return assembleMultipleFeature(resourceSet);
		}
*/	}


/*
	private Optional<PlanNodeID> assembleMultipleFeature(ResourceSet resourceSet) {

		var root = assembleStandaloneFeatures(resourceSet);

		deleteDefinitionTestCasesWithoutId(root);
		deleteImplementationScenarioOutlineContent(root);
		fillImplementationScenarioOutlines(root);

		// redefine definition test cases with implementation steps
		repository.searchNodes(PlanNodeCriteria.and(
			PlanNodeCriteria.descendantOf(root),
			PlanNodeCriteria.withNodeType(NodeType.TEST_CASE),
			PlanNodeCriteria.withTag(definitionTag)
		)).forEach(it -> redefine(root,it));

		root.getChildren(node ->
				node.hasProperty(GHERKIN_TYPE,GHERKIN_TYPE_FEATURE) &&
						node.hasTag(implementationTag)
		).forEach(PlanNodeDAO::delete);


		repository.searchNodes(PlanNodeCriteria.and(
			PlanNodeCriteria.descendantOf(root),
			PlanNodeCriteria.or(
				PlanNodeCriteria.withTag(definitionTag),
				PlanNodeCriteria.withTag(implementationTag)
			)
		)).forEach(it -> {
			repository.removeTag(it, implementationTag);
			repository.removeTag(it, definitionTag);
		});
		return normalize(root);

	}


	private void deleteImplementationScenarioOutlineContent(PlanNodeID root) {
		log.trace("deleteImplementationScenarioOutlineContent");
		repository.searchNodes(PlanNodeCriteria.and(
			PlanNodeCriteria.descendantOf(root),
			PlanNodeCriteria.withTag(implementationTag),
			PlanNodeCriteria.withProperty(GHERKIN_TYPE, GHERKIN_TYPE_SCENARIO_OUTLINE)
		)).forEach(it -> repository.deleteNode(it));
	}


	private Optional<PlanNodeID> normalize(PlanNodeID root) {
		if (repository.countNodeChildren(root) == 1) {
			var child = repository.getNodeChildren(root).findFirst().orElseThrow();
			repository.detachChildNode(root, child);
			repository.deleteNode(root);
			return Optional.of(child);
		} else {
			return Optional.of(root);
		}
	}


	private PlanNodeID assembleStandaloneFeatures(ResourceSet resourceSet) {
		log.trace("provideStandaloneFeatures");
		var id = repository.persistNode(new PlanNode(NodeType.TEST_AGGREGATOR));
		for (Resource resource : resourceSet) {
			assembleFeatureNode(
				keywordMapProvider,
				parser,
				idTagPattern,
				resource
			).ifPresent(child -> repository.attachChildNodeLast(id, child));
		}
		return id;
	}


	private void deleteDefinitionTestCasesWithoutId(PlanNodeID root) {
		repository.searchNodes(PlanNodeCriteria.and(
			PlanNodeCriteria.descendantOf(root),
			PlanNodeCriteria.withTag(definitionTag),
			PlanNodeCriteria.withNodeType(NodeType.TEST_CASE),
			PlanNodeCriteria.withField("identifier", null)
		)).forEach(it -> repository.deleteNode(it));
	}


	private void fillImplementationScenarioOutlines(PlanNodeID root) {
		log.trace("fillImplementationScenarioOutlines");
		repository.searchNodes(PlanNodeCriteria.and(
			PlanNodeCriteria.descendantOf(root),
			PlanNodeCriteria.withTag(implementationTag),
			PlanNodeCriteria.withProperty(GHERKIN_TYPE, GHERKIN_TYPE_SCENARIO_OUTLINE)
		)).forEach(scenarioOutline -> fillImplementationScenarioOutline(root, scenarioOutline));
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



	private void redefine(PlanNode root, PlanNode defTestCase) {
		log.trace("redefine {}",defTestCase.nodeID());
		implementationTestCase(root,defTestCase).ifPresentOrElse(
			impTestCase -> redefineTestCase(defTestCase, impTestCase),
			defTestCase::delete
		);
	}


	private void redefineTestCase(PlanNode defTestCase, PlanNode impTestCase) {

		// definition background is ignored
		deleteBackground(defTestCase.nodeID());

		int[] stepMap = extractStepMap(defTestCase.nodeID(), impTestCase.nodeID());

		var impSteps = repository.searchNodes(PlanNodeCriteria.and(
			PlanNodeCriteria.childOf(impTestCase.nodeID()),
			PlanNodeCriteria.withNodeType(NodeType.STEP)
		));


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


	private int[] extractStepMap(PlanNodeID defTestCase, PlanNodeID impTestCase) {
		// step map is in form: x-x-x-x...
		String stepMapProperty = repository.getNodeProperty(impTestCase, STEP_MAP).orElse(null);
		if (stepMapProperty == null || stepMapProperty.isBlank()) {
			// if not defiend, map 1-to-1 for each step
			int defTestCaseChildren = repository.countNodeChildren(defTestCase);
			stepMapProperty = "-1".repeat(defTestCaseChildren).substring(1);
		}
		return Stream.of(stepMapProperty.split("-")).mapToInt(Integer::parseInt).toArray();
	}


	private void deleteBackground(PlanNodeID defTestCase) {
		repository.getNodeChildren(defTestCase)
			.filter(child -> repository.existsProperty(child, GHERKIN_TYPE, GHERKIN_TYPE_BACKGROUND))
			.forEach( child -> repository.deleteNode(child) );
	}


	private Optional<PlanNodeID> implementationTestCase(PlanNode root, PlanNode definitionTestCase) {
		return repository.searchNodes(PlanNodeCriteria.and(
			PlanNodeCriteria.descendantOf(root.nodeID()),
			PlanNodeCriteria.withNodeType(NodeType.TEST_CASE),
			PlanNodeCriteria.withTag(implementationTag),
			PlanNodeCriteria.withField("identifier",definitionTestCase.identifier())
		)).findFirst();
	}



	private Optional<PlanNodeID> assembleFeatureNode(
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
			var builder = new FeaturePlanAssembler(
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
			log.error(e,"Cannot read resource {}",resource.relativePath());
			return Optional.empty();
		}

	}

*/
}