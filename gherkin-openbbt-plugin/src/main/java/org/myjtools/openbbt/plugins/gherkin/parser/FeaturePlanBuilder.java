package org.myjtools.openbbt.plugins.gherkin.parser;

import es.iti.wakamiti.api.repository.PlanRepository;
import static es.iti.wakamiti.api.lang.Functions.*;
import static es.iti.wakamiti.plugins.gherkin.GherkinConstants.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.*;
import java.util.stream.Collectors;

import es.iti.wakamiti.api.plan.*;
import es.iti.wakamiti.plugins.gherkin.parser.*;
import es.iti.wakamiti.plugins.gherkin.parser.elements.*;
import es.iti.wakamiti.plugins.gherkin.parser.elements.DataTable;


class FeaturePlanBuilder {

    private final Pattern propertyRegex = Pattern.compile(
        "\\s*#+\\s*([^\\s]+)\\s*:\\s*([^\\s]+)\\s*"
    );

    private final Feature feature;
    private final String relativePath;
    private final String scenarioKeyword;
    private final Background background;
    private final Pattern idTagPattern;
    private final PlanRepository repository;
    private final Map<PlanNodeID, Object> underlyingModels = new HashMap<>();


    FeaturePlanBuilder(
        Feature feature,
        String relativePath,
        KeywordMapProvider keywordMapProvider,
        String idTagPattern,
        PlanRepository repository
    ) {
        this.feature = feature;
        this.relativePath = relativePath;
        this.scenarioKeyword = new GherkinDialectFactory(keywordMapProvider,"en")
            .dialectFor(feature.language())
            .keywords(KeywordType.SCENARIO)
            .get(0);
        this.repository = repository;
        this.background = cast(first(feature.children()), Background.class);
        this.idTagPattern = Pattern.compile(idTagPattern);
    }


    public PlanNodeDAO createTestPlan() {
        return featureNode();
    }


    private PlanNodeDAO featureNode() {

        var nodeData = new PlanNode(NodeType.AGGREGATOR)
            .id(idFromTags(feature))
            .name(feature.name())
            .language(feature.language())
            .keyword(feature.keyword())
            .description(feature.description())
            .tags(tags(feature))
            .source(nodeLocation(feature))
            .addProperties(propertiesFromComments(feature))
            .addProperty(GHERKIN_TYPE, GHERKIN_TYPE_FEATURE);

        var dao = PlanNodeDAO.persist(repository,nodeData);
        underlyingModels.put(dao.nodeID(), feature);

        for (var child : feature.children()) {
            if (child instanceof Scenario scenario) {
                dao.attachChild(scenarioNode(scenario,dao));
            } else if (child instanceof ScenarioOutline scenarioOutline) {
                dao.attachChild(scenarioOutlineNode(scenarioOutline,dao));
            }
        }

        return dao;
    }



    private PlanNodeDAO scenarioNode(Scenario scenario, PlanNodeDAO parent) {
        return scenarioNode(scenario, scenario.name(), idFromTags(scenario), scenario.keyword(), parent);
    }


    private PlanNodeDAO scenarioNode(
        ScenarioOutline scenarioOutline,
        int example,
        PlanNodeDAO parent
    ) {
        return scenarioNode(
            scenarioOutline,
            "%s [%s]".formatted(scenarioOutline.name(), example),
            idFromTags(scenarioOutline)+"_"+example,
            scenarioKeyword,
            parent
        );
    }


    private PlanNodeDAO scenarioNode(
        ScenarioDefinition scenarioDefinition,
        String name,
        String id,
        String keyword,
        PlanNodeDAO parent
    ) {
        var data = new PlanNode(NodeType.TEST_CASE)
            .id(id)
            .name(name)
            .language(feature.language())
            .keyword(keyword)
            .description(scenarioDefinition.description())
            .tags(tags(parent,scenarioDefinition))
            .source(nodeLocation(scenarioDefinition))
            .addProperties(propertiesFromComments(scenarioDefinition,parent))
            .addProperty(GHERKIN_TYPE, GHERKIN_TYPE_SCENARIO);
        var dao = PlanNodeDAO.persist(repository,data);
        underlyingModels.put(dao.nodeID(),scenarioDefinition);

        ifPresent(createBackgroundStepsNode(dao,null), dao::attachChild);
        scenarioDefinition.children().forEach( step -> dao.attachChild(stepNode(step)));
        return dao;
    }


    private PlanNodeDAO scenarioOutlineNode(ScenarioOutline scenarioOutline, PlanNodeDAO parent) {

        var node = new PlanNode(NodeType.AGGREGATOR)
            .id(idFromTags(scenarioOutline))
            .name(scenarioOutline.name())
            .displayNamePattern("{keyword}: {name}")
            .language(feature.language())
            .keyword(scenarioOutline.keyword())
            .description(scenarioOutline.description())
            .tags(tags(parent,scenarioOutline))
            .source(nodeLocation(scenarioOutline))
            .dataTable(let(first(scenarioOutline.examples()),this::tableOf))
            .addProperties(propertiesFromComments(scenarioOutline,parent))
            .addProperty(GHERKIN_TYPE, GHERKIN_TYPE_SCENARIO_OUTLINE);

        var dao = PlanNodeDAO.persist(repository,node);
        underlyingModels.put(dao.nodeID(),scenarioOutline);

        scenarioOutline.examples().stream()
            .flatMap(examples -> createScenariosFromExamples(scenarioOutline, examples, dao).stream())
            .forEach(dao::attachChild);

        return dao;
    }


    private PlanNodeDAO stepNode(Step step) {
        var node = new PlanNode(NodeType.STEP)
            .name(step.text())
            .language(feature.language())
            .keyword(step.keyword())
            .displayNamePattern("{keyword} {name}")
            .source(nodeLocation(step))
            .addProperty(GHERKIN_TYPE,GHERKIN_TYPE_STEP);
        if (step.argument() instanceof DataTable dataTable) {
            node.dataTable(tableOf(dataTable));
        } else if (step.argument() instanceof DocString docString) {
            node.document(documentOf(docString));
        }
        return PlanNodeDAO.persist(repository,node);
    }


    public PlanNodeDAO createBackgroundStepsNode(PlanNodeDAO parent, String name) {
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
        var dao = PlanNodeDAO.persist(repository,node);
        underlyingModels.put(dao.nodeID(),background);
        background.children().forEach(step -> dao.attachChild(stepNode(step)));
        return dao;
    }


    public List<PlanNodeDAO> createScenariosFromExamples(
        ScenarioOutline scenarioOutline,
        Examples examples,
        PlanNodeDAO parent
    ) {
        return indexMapped(substitutions(examples), (number, substitution) -> {
            var scenarioDao = scenarioNode(scenarioOutline, number+1, parent);
            scenarioDao.getChildren().forEach( step -> {
                if (step.node().name() != null) {
                    step.node().name( substitution.apply(step.node().name()) );
                    step.update();
                }
            });
            return scenarioDao;
         });
    }


    private String nodeLocation(Node node) {
        return "%s[%s,%s]".formatted(relativePath,node.location().line(),node.location().column());
    }


    private Map<String,String> propertiesFromComments(Commented node, PlanNodeDAO parent) {
        SortedMap<String,String> properties = new TreeMap<>(parent.node().properties());
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

    private Set<String> tags(PlanNodeDAO parent, Tagged node) {
        Set<String> result = new HashSet<>(parent.node().tags());
        result.addAll(tags(node));
        return result;
    }


    private es.iti.wakamiti.api.plan.DataTable tableOf(DataTable dataTable) {
        return new es.iti.wakamiti.api.plan.DataTable(
            mapped(dataTable.rows(), row -> mapped(row.cells(), TableCell::value))
        );
    }


    private es.iti.wakamiti.api.plan.DataTable tableOf(Examples examples) {
        return new es.iti.wakamiti.api.plan.DataTable(concat(
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

}
