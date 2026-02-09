package org.myjtools.openbbt.plugins.gherkin;


import org.myjtools.gherkinparser.GherkinDialectFactory;
import org.myjtools.gherkinparser.KeywordMapProvider;
import org.myjtools.gherkinparser.KeywordType;
import org.myjtools.gherkinparser.elements.*;
import org.myjtools.openbbt.core.PlanNodeRepository;
import org.myjtools.openbbt.core.plan.Document;
import org.myjtools.openbbt.core.plan.NodeType;
import org.myjtools.openbbt.core.plan.PlanNode;
import org.myjtools.openbbt.core.plan.PlanNodeID;
import org.myjtools.openbbt.core.util.Patterns;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.myjtools.openbbt.plugins.gherkin.GherkinConstants.*;

public class FeaturePlanAssembler {

	private final Pattern propertyRegex = Patterns.of("\\s*#+\\s*([^\\s]+)\\s*:\\s*([^\\s]+)\\s*");

	private final Feature feature;
	private final String relativePath;
	private final String scenarioKeyword;
	private final Background background;
	private final Pattern idTagPattern;
	private final PlanNodeRepository repository;
	private final Map<PlanNodeID, Object> underlyingModels = new HashMap<>();


	public FeaturePlanAssembler(
		Feature feature,
		String relativePath,
		KeywordMapProvider keywordMapProvider,
		String idTagPattern,
		PlanNodeRepository repository
	) {
		this.feature = feature;
		this.relativePath = relativePath;
		this.scenarioKeyword = new GherkinDialectFactory(keywordMapProvider,"en")
			.dialectFor(feature.language())
			.keywords(KeywordType.SCENARIO)
			.getFirst();
		this.repository = repository;
		this.background = feature.children().stream()
			.filter(Background.class::isInstance)
			.map(it -> (Background)it)
			.findFirst()
			.orElse(null);
		this.idTagPattern = Pattern.compile(idTagPattern);
	}


	public PlanNodeID createTestPlan() {
		return featureNode();
	}


	private PlanNodeID featureNode() {

		var nodeData = new PlanNode(NodeType.TEST_AGGREGATOR)
			.identifier(idFromTags(feature))
			.name(feature.name())
			.language(feature.language())
			.keyword(feature.keyword())
			.description(feature.description())
			.tags(tags(feature))
			.source(nodeLocation(feature))
			.addProperties(propertiesFromComments(feature))
			.addProperty(GHERKIN_TYPE, GHERKIN_TYPE_FEATURE);

		var id = repository.persistNode(nodeData);
		underlyingModels.put(id, feature);
		PlanNode node = repository.getNodeData(id).orElseThrow();

		for (var child : feature.children()) {
			if (child instanceof Scenario scenario) {
				repository.attachChildNodeLast(id, scenarioNode(scenario,node));
			} else if (child instanceof ScenarioOutline scenarioOutline) {
				repository.attachChildNodeLast(id, scenarioOutlineNode(scenarioOutline,node));
			}
		}

		return id;
	}



	private PlanNodeID scenarioNode(Scenario scenario, PlanNode parent) {
		return scenarioNode(scenario, scenario.name(), idFromTags(scenario), scenario.keyword(), parent);
	}


	private PlanNodeID scenarioNode(ScenarioOutline scenarioOutline, int example, PlanNode parent) {
		return scenarioNode(
			scenarioOutline,
			"%s [%s]".formatted(scenarioOutline.name(), example),
			idFromTags(scenarioOutline)+"_"+example,
			scenarioKeyword,
			parent
		);
	}


	private PlanNodeID scenarioNode(
		ScenarioDefinition scenarioDefinition,
		String name,
		String identifier,
		String keyword,
		PlanNode parent
	) {
		var data = new PlanNode(NodeType.TEST_CASE)
			.identifier(identifier)
			.name(name)
			.language(feature.language())
			.keyword(keyword)
			.description(scenarioDefinition.description())
			.tags(tags(parent,scenarioDefinition))
			.source(nodeLocation(scenarioDefinition))
			.addProperties(propertiesFromComments(scenarioDefinition,parent))
			.addProperty(GHERKIN_TYPE, GHERKIN_TYPE_SCENARIO);

		var id = repository.persistNode(data);
		underlyingModels.put(id,scenarioDefinition);

		var backgroundNodeId = createBackgroundStepsNode(data, null);
		if (backgroundNodeId != null) {
			repository.attachChildNodeLast(id, backgroundNodeId);
		}

		scenarioDefinition.children().forEach( step -> repository.attachChildNodeLast(id, stepNode(step)));
		return id;
	}


	private PlanNodeID scenarioOutlineNode(ScenarioOutline scenarioOutline, PlanNode parent) {

		var node = new PlanNode(NodeType.TEST_AGGREGATOR)
				.identifier(idFromTags(scenarioOutline))
				.name(scenarioOutline.name())
				.display("%s %s".formatted(scenarioOutline.keyword(), scenarioOutline.name()))
				.language(feature.language())
				.keyword(scenarioOutline.keyword())
				.description(scenarioOutline.description())
				.tags(tags(parent,scenarioOutline))
				.source(nodeLocation(scenarioOutline))
				.addProperties(propertiesFromComments(scenarioOutline,parent))
				.addProperty(GHERKIN_TYPE, GHERKIN_TYPE_SCENARIO_OUTLINE);

		node.dataTable(tableOf(scenarioOutline.examples().getFirst()));

		var id = repository.persistNode(node);
		underlyingModels.put(id,scenarioOutline);

		scenarioOutline.examples().stream()
			.flatMap(examples -> createScenariosFromExamples(scenarioOutline, examples, node).stream())
			.forEach(childID -> repository.attachChildNodeLast(id, childID));

		return id;
	}


	private PlanNodeID stepNode(Step step) {
		var node = new PlanNode(NodeType.STEP)
			.name(step.text())
			.language(feature.language())
			.keyword(step.keyword())
			.display("%s %s".formatted(step.keyword(), step.text()))
			.source(nodeLocation(step))
			.addProperty(GHERKIN_TYPE,GHERKIN_TYPE_STEP);
		if (step.argument() instanceof DataTable dataTable) {
			node.dataTable(tableOf(dataTable));
		} else if (step.argument() instanceof DocString docString) {
			node.document(documentOf(docString));
		}
		return repository.persistNode(node);
	}


	public PlanNodeID createBackgroundStepsNode(PlanNode parent, String name) {
		if (background == null) {
			return null;
		}
		var node = new PlanNode(NodeType.STEP_AGGREGATOR)
			.name(name == null ? background.name() : name)
			.language(feature.language())
			.keyword(background.keyword())
			.description(background.description())
			.tags(tags(parent,background))
			.source(nodeLocation(background))
			.addProperties(propertiesFromComments(background))
			.addProperty(GHERKIN_TYPE,GHERKIN_TYPE_BACKGROUND);
		var id = repository.persistNode(node);
		underlyingModels.put(id,background);
		background.children().forEach(step -> repository.attachChildNodeLast(id,stepNode(step)));
		return id;
	}


	public List<PlanNodeID> createScenariosFromExamples(
			ScenarioOutline scenarioOutline,
			Examples examples,
			PlanNode parent
	) {
		return indexMapped(substitutions(examples), (number, substitution) -> {
			var scenarioID = scenarioNode(scenarioOutline, number+1, parent);
			repository.getNodeChildren(scenarioID).forEach(scenarioChildID -> {
				var scenarioChild = repository.getNodeData(scenarioChildID).orElseThrow();
				if (scenarioChild.name() != null) {
					scenarioChild.name(substitution.apply(scenarioChild.name()));
					repository.persistNode(scenarioChild);
				}
			});
			return scenarioID;
		});
	}


	private String nodeLocation(Node node) {
		return "%s[%s,%s]".formatted(relativePath,node.location().line(),node.location().column());
	}


	private Map<String,String> propertiesFromComments(Commented node, PlanNode parent) {
		SortedMap<String,String> properties = new TreeMap<>(parent.properties());
		properties.putAll(propertiesFromComments(node));
		return properties;
	}


	private Map<String,String> propertiesFromComments(Commented node) {
		return node.comments().stream()
			.map(Comment::text)
			.map(propertyRegex::matcher)
			.filter(Matcher::find)
			.collect(Collectors.toMap(it -> it.group(1),it -> it.group(2)));
	}


	private Set<String> tags(Tagged node) {
		return node.tags().stream()
			.map(it -> it.name().substring(1))
			.filter(idTagPattern.asPredicate().negate())
			.collect(Collectors.toCollection(HashSet::new));
	}


	private Set<String> tags(PlanNode parent, Tagged node) {
		Set<String> result = new HashSet<>(parent.tags());
		result.addAll(tags(node));
		return result;
	}


	private org.myjtools.openbbt.core.plan.DataTable tableOf(DataTable dataTable) {
		return new org.myjtools.openbbt.core.plan.DataTable(
				mapped(dataTable.rows(), row -> mapped(row.cells(), TableCell::value))
		);
	}


	private org.myjtools.openbbt.core.plan.DataTable tableOf(Examples examples) {
		return new org.myjtools.openbbt.core.plan.DataTable(concat(
				List.of(mapped(examples.tableHeader().cells(),TableCell::value)),
				mapped(examples.tableBody(), row -> mapped(row.cells(),TableCell::value))
		));
	}


	private Document documentOf(DocString docString) {
		return new Document(docString.contentType(), docString.content());
	}


	private List<Function<String,String>> substitutions(Examples examples) {
		var variables = mapped(
				examples.tableHeader().cells(),
				cell->"<"+cell.value()+">"
		);
		var values = mapped(
				examples.tableBody(),
				row -> mapped(row.cells(),TableCell::value)
		);
		return mapped(values, example ->
				indexMapped(example, (index,value) -> Map.entry(variables.get(index),value) )
						.stream()
						.map(this::toReplaceFunction)
						.reduce(Function::andThen)
						.orElseThrow()
		);
	}


	private Function<String,String> toReplaceFunction(Map.Entry<String,String> pair) {
		return ( input -> input.replace(pair.getKey(), pair.getValue()) );
	}


	private String idFromTags(Tagged tagged) {
		for (Tag tag : List.copyOf(tagged.tags())) {
			var matcher = this.idTagPattern.matcher(tag.name().substring(1));
			if (matcher.find()) {
				return matcher.groupCount() > 0 ? matcher.group(1) : matcher.group(0);
			}
		}
		return null;
	}


	public Map<PlanNodeID,Object> underlyingModels() {
		return underlyingModels;
	}

	private static <T,U> List<U> indexMapped(List<T> list, BiFunction<Integer,T,U> mapper) {
		if (list == null) return List.of();
		List<U> result = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			result.add(mapper.apply(i,list.get(i)));
		}
		return List.copyOf(result);
	}

	private static <T,U> List<U> mapped(List<T> list, Function<T,U> mapper) {
		if (list == null) return List.of();
		return list.stream().map(mapper).toList();
	}

	private static <T> List<T> concat(List<T> list1, List<T> list2) {
		return Stream.concat(list1.stream(), list2.stream()).toList();
	}

}