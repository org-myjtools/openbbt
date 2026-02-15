package org.myjtools.openbbt.plugins.gherkin;


import org.myjtools.gherkinparser.GherkinDialectFactory;
import org.myjtools.gherkinparser.KeywordMapProvider;
import org.myjtools.gherkinparser.KeywordType;
import org.myjtools.gherkinparser.elements.*;
import org.myjtools.gherkinparser.elements.DataTable;
import org.myjtools.openbbt.core.PlanNodeRepository;
import org.myjtools.openbbt.core.plan.*;
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
	private final String backgroundKeyword;
	private final Background background;
	private final Pattern idTagPattern;
	private final TagExpression tagExpression;
	private final PlanNodeRepository repository;
	private final Map<PlanNodeID, Object> underlyingModels = new HashMap<>();



	public FeaturePlanAssembler(
		Feature feature,
		String relativePath,
		KeywordMapProvider keywordMapProvider,
		String idTagPattern,
		PlanNodeRepository repository,
		TagExpression tagExpression
	) {
		this.feature = feature;
		this.relativePath = relativePath;
		var dialect = new GherkinDialectFactory(keywordMapProvider,"en").dialectFor(feature.language());
		this.scenarioKeyword = dialect.keywords(KeywordType.SCENARIO).getFirst();
		this.backgroundKeyword = dialect.keywords(KeywordType.BACKGROUND).getFirst();
		this.repository = repository;
		this.background = feature.children().stream()
			.filter(Background.class::isInstance)
			.map(it -> (Background)it)
			.findFirst()
			.orElse(null);
		this.idTagPattern = Pattern.compile(idTagPattern);
		this.tagExpression = tagExpression;
	}


	public Optional<PlanNodeID> createTestPlan() {
		return featureNode();
	}


	private Optional<PlanNodeID> featureNode() {

		var nodeData = new PlanNode(NodeType.TEST_AGGREGATOR)
			.identifier(idFromTags(feature))
			.name(feature.name())
			.language(feature.language())
			.keyword(feature.keyword())
			.display("{name}")
			.description(feature.description())
			.tags(tags(feature))
			.source(nodeLocation(feature))
			.addProperties(propertiesFromComments(feature))
			.addProperty(GHERKIN_TYPE, GHERKIN_TYPE_FEATURE);

		var id = repository.persistNode(nodeData);
		underlyingModels.put(id, feature);

		for (var child : feature.children()) {
			if (child instanceof Scenario scenario) {
				scenarioNode(scenario,id).ifPresent( scenarioNode ->
					repository.attachChildNodeLast(id, scenarioNode)
				);
			} else if (child instanceof ScenarioOutline scenarioOutline) {
				scenarioOutlineNode(scenarioOutline,id).ifPresent( scenarioOutlineNode ->
					 repository.attachChildNodeLast(id, scenarioOutlineNode)
				);
			}
		}

		if (repository.countNodeChildren(id) == 0) {
			repository.deleteNode(id);
			underlyingModels.remove(id);
			return Optional.empty();
		}

		return Optional.of(id);
	}



	private Optional<PlanNodeID> scenarioNode(Scenario scenario, PlanNodeID parent) {
		return scenarioNode(scenario, scenario.name(), idFromTags(scenario), scenario.keyword(), parent);
	}


	private Optional<PlanNodeID> scenarioNode(ScenarioOutline scenarioOutline, int example, PlanNodeID parent) {
		return scenarioNode(
			scenarioOutline,
			"%s [%s]".formatted(scenarioOutline.name(), example),
			idFromTags(scenarioOutline)+"_"+example,
			scenarioKeyword,
			parent
		);
	}


	private Optional<PlanNodeID> scenarioNode(
		ScenarioDefinition scenarioDefinition,
		String name,
		String identifier,
		String keyword,
		PlanNodeID parent
	) {
		boolean include = tagExpression.evaluate(tags(parent, scenarioDefinition));
		if (!include) {
			return Optional.empty();
		}
		var data = new PlanNode(NodeType.TEST_CASE)
			.identifier(identifier)
			.name(name)
			.language(feature.language())
			.display("{name}")
			.keyword(keyword)
			.description(scenarioDefinition.description())
			.tags(tags(parent,scenarioDefinition))
			.source(nodeLocation(scenarioDefinition))
			.addProperties(propertiesFromComments(scenarioDefinition,parent))
			.addProperty(GHERKIN_TYPE, GHERKIN_TYPE_SCENARIO);

		var id = repository.persistNode(data);
		underlyingModels.put(id,scenarioDefinition);

		var backgroundNodeId = createBackgroundStepsNode(id, null);
		if (backgroundNodeId != null) {
			repository.attachChildNodeLast(id, backgroundNodeId);
		}

		scenarioDefinition.children().forEach( step -> repository.attachChildNodeLast(id, stepNode(step)));
		return Optional.of(id);
	}


	private Optional<PlanNodeID> scenarioOutlineNode(ScenarioOutline scenarioOutline, PlanNodeID parent) {

		boolean include = tagExpression.evaluate(tags(parent, scenarioOutline));
		if (!include) {
			return Optional.empty();
		}
		var node = new PlanNode(NodeType.TEST_AGGREGATOR)
			.identifier(idFromTags(scenarioOutline))
			.name(scenarioOutline.name())
			.display("{name}")
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
			.flatMap(examples -> createScenariosFromExamples(scenarioOutline, examples, id).stream())
			.forEach(childID -> repository.attachChildNodeLast(id, childID));

		return Optional.of(id);
	}


	private PlanNodeID stepNode(Step step) {
		var node = new PlanNode(NodeType.STEP)
			.name(step.text())
			.language(feature.language())
			.keyword(step.keyword())
			.display("{keyword} {name}")
			.source(nodeLocation(step))
			.addProperty(GHERKIN_TYPE,GHERKIN_TYPE_STEP);
		if (step.argument() instanceof DataTable dataTable) {
			node.dataTable(tableOf(dataTable));
		} else if (step.argument() instanceof DocString docString) {
			node.document(documentOf(docString));
		}
		return repository.persistNode(node);
	}


	public PlanNodeID createBackgroundStepsNode(PlanNodeID parent, String name) {
		if (background == null) {
			return null;
		}
		var node = new PlanNode(NodeType.STEP_AGGREGATOR)
			.name(notEmpty(name, background.name()))
			.language(feature.language())
			.keyword(background.keyword())
			.display("{keyword}")
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
		PlanNodeID parent
	) {
		return indexMapped(substitutions(examples), (number, substitution) -> {
			var scenarioID = scenarioNode(scenarioOutline, number+1, parent);
			if (scenarioID.isEmpty()) {
				return scenarioID;
			}
			repository.getNodeChildren(scenarioID.orElseThrow()).forEach(scenarioChildID -> {
				var scenarioChild = repository.getNodeData(scenarioChildID).orElseThrow();
				if (scenarioChild.name() != null) {
					scenarioChild.name(substitution.apply(scenarioChild.name()));
					repository.persistNode(scenarioChild);
				}
			});
			return scenarioID;
		}).stream().filter(Optional::isPresent).map(Optional::get).toList();
	}


	private String nodeLocation(Node node) {
		return "%s[%s,%s]".formatted(relativePath,node.location().line(),node.location().column());
	}


	private Map<String,String> propertiesFromComments(Commented node, PlanNodeID nodeID) {
		SortedMap<String,String> properties = new TreeMap<>(repository.getProperties(nodeID));
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


	private Set<String> tags(PlanNodeID nodeID, Tagged node) {
		Set<String> result = new HashSet<>(repository.getTags(nodeID));
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

	private static String notEmpty(String... options) {
		for (String option : options) {
			if (option != null && !option.isBlank()) {
				return option;
			}
		}
		return "";
	}
}