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
import org.myjtools.openbbt.core.contributors.SuiteAssembler;
import org.myjtools.openbbt.core.plan.NodeType;
import org.myjtools.openbbt.core.plan.PlanNode;
import org.myjtools.openbbt.core.plan.PlanNodeID;
import org.myjtools.openbbt.core.plan.TagExpression;
import org.myjtools.openbbt.core.project.TestSuite;
import org.myjtools.openbbt.core.util.Log;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import static org.myjtools.openbbt.plugins.gherkin.GherkinConstants.*;

@Extension
public class GherkinSuiteAssembler implements SuiteAssembler {

	private static final Log log = Log.of("plugins.gherkin");
	private static final String STEP_MAP = "gherkin.step-map";

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
	public Optional<PlanNodeID> assembleSuite(TestSuite testSuite) {
		ResourceSet resourceSet = resourceFinder.findResources("*.feature");
		TagExpression tagExpression = testSuite.tagExpression();
		if (resourceSet.size() == 1) {
			var feature = assembleFeatureNode(keywordMapProvider, parser, idTagPattern, resourceSet.get(0), testSuite);
			return feature.flatMap(it -> wrapTestSuite(it, testSuite));
		} else {
			return assembleMultipleFeature(resourceSet, testSuite);
		}
	}


	private Optional<PlanNodeID> wrapTestSuite(PlanNodeID feature, TestSuite testSuite) {
		PlanNode root = new PlanNode(NodeType.TEST_SUITE);
		root.name(testSuite.name());
		var id = repository.persistNode(root);
		repository.attachChildNodeLast(id, feature);
		return Optional.of(id);
	}


	private Optional<PlanNodeID> assembleMultipleFeature(ResourceSet resourceSet, TestSuite testSuite) {

		var root = assembleStandaloneFeatures(resourceSet, testSuite);

		deleteDefinitionTestCasesWithoutId(root);
		deleteImplementationScenarioOutlineContent(root);
		fillImplementationScenarioOutlines(root, testSuite.tagExpression());

		// redefine definition test cases with implementation steps
		repository.searchNodes(PlanNodeCriteria.and(
			PlanNodeCriteria.descendantOf(root),
			PlanNodeCriteria.withNodeType(NodeType.TEST_CASE),
			PlanNodeCriteria.withTag(definitionTag)
		)).forEach(definitionTestCase -> redefine(root,definitionTestCase));

		repository.searchNodes(PlanNodeCriteria.and(
			PlanNodeCriteria.descendantOf(root),
			PlanNodeCriteria.withProperty(GHERKIN_TYPE, GHERKIN_TYPE_FEATURE),
			PlanNodeCriteria.withTag(implementationTag)
		)).forEach(implementationFeature -> repository.deleteNode(implementationFeature));

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

		if (repository.countNodeChildren(root) == 0) {
			repository.deleteNode(root);
			return Optional.empty();
		} else {
			repository.updateNodeField(root, "nodeType", NodeType.TEST_SUITE.value);
			repository.updateNodeField(root, "name", testSuite.name());
			return Optional.ofNullable(root);
		}

	}


	private void deleteImplementationScenarioOutlineContent(PlanNodeID root) {
		log.trace("deleteImplementationScenarioOutlineContent");
		repository.searchNodes(PlanNodeCriteria.and(
			PlanNodeCriteria.descendantOf(root),
			PlanNodeCriteria.withTag(implementationTag),
			PlanNodeCriteria.withProperty(GHERKIN_TYPE, GHERKIN_TYPE_SCENARIO_OUTLINE)
		)).forEach(scenarioOutline ->
			repository.getNodeChildren(scenarioOutline).toList()
				.forEach(child -> repository.deleteNode(child))
		);
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


	private PlanNodeID assembleStandaloneFeatures(ResourceSet resourceSet, TestSuite testSuite) {
		log.trace("provideStandaloneFeatures");
		var id = repository.persistNode(new PlanNode(NodeType.TEST_AGGREGATOR));
		for (Resource resource : resourceSet) {
			assembleFeatureNode(
				keywordMapProvider,
				parser,
				idTagPattern,
				resource,
				testSuite
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


	private void fillImplementationScenarioOutlines(PlanNodeID root, TagExpression tagExpression) {
		log.trace("fillImplementationScenarioOutlines");
		repository.searchNodes(PlanNodeCriteria.and(
			PlanNodeCriteria.descendantOf(root),
			PlanNodeCriteria.withTag(implementationTag),
			PlanNodeCriteria.withProperty(GHERKIN_TYPE, GHERKIN_TYPE_SCENARIO_OUTLINE)
		)).forEach(scenarioOutline -> fillImplementationScenarioOutline(root, scenarioOutline, tagExpression));
	}



	private void fillImplementationScenarioOutline(PlanNodeID root, PlanNodeID impScenarioOutline, TagExpression tagExpression) {

		PlanNodeID impFeature = repository.getParentNode(impScenarioOutline).orElseThrow();
		String identifier = repository.getNodeField(impScenarioOutline, "identifier").orElseThrow().toString();

		PlanNodeID defScenarioOutline = repository.searchNodes(PlanNodeCriteria.and(
			PlanNodeCriteria.descendantOf(root),
			PlanNodeCriteria.withNodeType(NodeType.TEST_AGGREGATOR),
			PlanNodeCriteria.withTag(definitionTag),
			PlanNodeCriteria.withProperty(GHERKIN_TYPE, GHERKIN_TYPE_SCENARIO_OUTLINE),
			PlanNodeCriteria.withField("identifier",identifier)
		)).findFirst().orElseThrow(
			() -> new OpenBBTException(
				"There is no definition feature with name {}",
				repository.getNodeField(impFeature, "name").orElseThrow()
			)
		);

		Examples defExamples = ((ScenarioOutline) underlyingModels.get(defScenarioOutline))
			.examples()
			.getFirst();

		var stepsFromExamples = new FeaturePlanAssembler(
			(Feature) underlyingModels.get(impFeature),
			repository.getNodeField(impFeature, "source").map(Object::toString).orElse(""),
			keywordMapProvider,
			idTagPattern,
			repository,
			tagExpression
		).createScenariosFromExamples(
			(ScenarioOutline) underlyingModels.get(impScenarioOutline),
			defExamples,
			impScenarioOutline
		);

		stepsFromExamples.forEach(step -> repository.attachChildNodeLast(impScenarioOutline, step));

	}



	private void redefine(PlanNodeID root, PlanNodeID defTestCase) {
		log.trace("redefine {}",defTestCase);
		implementationTestCase(root, defTestCase).ifPresentOrElse(
			impTestCase -> redefineTestCase(defTestCase, impTestCase),
			() -> repository.deleteNode(defTestCase)
		);
	}


	private void redefineTestCase(PlanNodeID defTestCase, PlanNodeID impTestCase) {

		// definition background is ignored
		deleteBackground(defTestCase);

		int[] stepMap = extractStepMap(defTestCase, impTestCase);

		var impSteps = repository.searchNodes(PlanNodeCriteria.and(
			PlanNodeCriteria.childOf(impTestCase),
			PlanNodeCriteria.withNodeType(NodeType.STEP)
		)).toList();


		int defStepCount = 0;
		int impStepCount = 0;

		for (PlanNodeID defStep : repository.getNodeChildren(defTestCase).toList()) {
			for (int i = 0; i < stepMap[defStepCount]; i++) {
				PlanNodeID impStep = impSteps.get(impStepCount);
				repository.getParentNode(impStep).ifPresent(impStepParent -> repository.detachChildNode(impStepParent, impStep));
				repository.attachChildNodeLast(defStep,impStep);
				impStepCount++;
			}
			redefineStepNodeType(stepMap, defStepCount, defStep);
			defStepCount++;
		}

		moveBackgroundToOtherTestCase(impTestCase, defTestCase);

	}



	private void redefineStepNodeType(int[] stepMap, int defStepCount, PlanNodeID defStep) {
		if (stepMap[defStepCount] == 0) {
			repository.updateNodeField(defStep, "nodeType", NodeType.VIRTUAL_STEP.value);
		} else {
			repository.updateNodeField(defStep, "nodeType", NodeType.STEP_AGGREGATOR.value);
		}
	}


	private void moveBackgroundToOtherTestCase(PlanNodeID origin, PlanNodeID target) {
		repository.searchNodes(PlanNodeCriteria.and(
			PlanNodeCriteria.childOf(origin),
			PlanNodeCriteria.withProperty(GHERKIN_TYPE, GHERKIN_TYPE_BACKGROUND)
		)).findFirst().ifPresent(originBackground -> {
			repository.detachChildNode(origin, originBackground);
			repository.attachChildNodeFirst(target, originBackground); // background always in first
			repository.updateNodeField(originBackground, "name", "<definition>");
			}
		);
	}


	private int[] extractStepMap(PlanNodeID defTestCase, PlanNodeID impTestCase) {
		// step map is in form: x-x-x-x...
		String stepMapProperty = repository.getProperty(impTestCase, STEP_MAP).orElse(null);
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


	private Optional<PlanNodeID> implementationTestCase(PlanNodeID root, PlanNodeID definitionTestCase) {
		return repository.searchNodes(PlanNodeCriteria.and(
			PlanNodeCriteria.descendantOf(root),
			PlanNodeCriteria.withNodeType(NodeType.TEST_CASE),
			PlanNodeCriteria.withTag(implementationTag),
			PlanNodeCriteria.withField("identifier",repository.getNodeField(definitionTestCase,"identifier").orElseThrow()))
		).findFirst();
	}



	private Optional<PlanNodeID> assembleFeatureNode(
		DefaultKeywordMapProvider keywordMapProvider,
		GherkinParser parser,
		String idTagPattern,
		Resource resource,
		TestSuite testSuite
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
				repository,
				testSuite.tagExpression()
			);
			var result = builder.createTestPlan();
			this.underlyingModels.putAll(builder.underlyingModels());
			return result;
		} catch (RuntimeException | IOException e) {
			log.error(e,"Cannot read resource {}",resource.relativePath());
			return Optional.empty();
		}

	}


}