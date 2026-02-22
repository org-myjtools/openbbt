package org.myjtools.openbbt.plugins.markdownplan;

import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.jexten.PostConstruct;
import org.myjtools.openbbt.core.*;
import org.myjtools.openbbt.core.contributors.SuiteAssembler;
import org.myjtools.openbbt.core.plan.DataTable;
import org.myjtools.openbbt.core.plan.Document;
import org.myjtools.openbbt.core.plan.NodeType;
import org.myjtools.openbbt.core.plan.PlanNode;
import org.myjtools.openbbt.core.plan.PlanNodeID;
import org.myjtools.openbbt.core.plan.TagExpression;
import org.myjtools.openbbt.core.project.TestSuite;
import org.myjtools.openbbt.core.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SPI implementation that assembles a test suite from Markdown {@code .md} files.
 *
 * <p>Each Markdown file is parsed according to the following heading convention:</p>
 * <ul>
 *   <li>{@code #} (H1) — feature ({@link NodeType#TEST_AGGREGATOR})</li>
 *   <li>{@code ##} (H2) — test case ({@link NodeType#TEST_CASE})</li>
 *   <li>{@code ###} (H3) — step keyword (e.g. Given / When / Then)</li>
 * </ul>
 * <p>Tags are expressed as blockquotes immediately after the heading
 * ({@code > @tag1 @tag2}). Properties are expressed as HTML comments
 * ({@code <!--- key: value --->}) also immediately after the heading.
 * List items under a step heading become {@link NodeType#STEP} nodes.</p>
 *
 * <p>A step may carry an argument: a GFM table immediately after the bullet list
 * is attached as a {@link DataTable}, and a fenced code block is attached as a
 * {@link Document}. In both cases the argument is associated with the last step
 * in the preceding list.</p>
 */
@Extension
public class MarkdownSuiteAssembler implements SuiteAssembler {

    private static final Log log = Log.of("plugins.markdown");
    private static final String MARKDOWN_TYPE = "markdownType";
    private static final String MARKDOWN_TYPE_FEATURE = "feature";
    private static final String MARKDOWN_TYPE_TESTCASE = "testCase";
    private static final Pattern PROPERTY_LINE = Pattern.compile("\\s*([^:\\s]+)\\s*:\\s*(.+)");

    @Inject("markdown")
    Config config;

    @Inject
    PlanNodeRepository repository;

    @Inject
    ResourceFinder resourceFinder;

    private Parser markdownParser;
    private Pattern idTagPattern;

    @PostConstruct
    public void init() {
        this.markdownParser = Parser.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();
        this.idTagPattern = Pattern.compile(
            config.getString(OpenBBTConfig.ID_TAG_PATTERN).orElseThrow()
        );
    }

    @Override
    public Optional<PlanNodeID> assembleSuite(TestSuite testSuite) {
        ResourceSet resourceSet = resourceFinder.findResources("*.md");
        if (resourceSet.isEmpty()) return Optional.empty();

        PlanNode suiteNode = new PlanNode(NodeType.TEST_SUITE).name(testSuite.name());
        PlanNodeID suiteId = repository.persistNode(suiteNode);

        for (Resource resource : resourceSet) {
            assembleMarkdownFile(resource, testSuite)
                .ifPresent(id -> repository.attachChildNodeLast(suiteId, id));
        }

        if (repository.countNodeChildren(suiteId) == 0) {
            repository.deleteNode(suiteId);
            return Optional.empty();
        }
        return Optional.of(suiteId);
    }


    private Optional<PlanNodeID> assembleMarkdownFile(Resource resource, TestSuite testSuite) {
        try (var inputStream = resource.open()) {
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            String relativePath = resource.relativePath().toString();
            List<MarkdownFeature> sections = parseMarkdownSections(content);

            if (sections.isEmpty()) return Optional.empty();

            if (sections.size() == 1) {
                return buildFeatureNode(sections.getFirst(), relativePath, testSuite.tagExpression());
            }

            // Multiple H1 sections in one file: wrap in a file-level aggregator
            PlanNode fileNode = new PlanNode(NodeType.TEST_AGGREGATOR)
                .name(resource.relativePath().getFileName().toString())
                .source(relativePath);
            PlanNodeID fileId = repository.persistNode(fileNode);
            for (MarkdownFeature section : sections) {
                buildFeatureNode(section, relativePath, testSuite.tagExpression())
                    .ifPresent(id -> repository.attachChildNodeLast(fileId, id));
            }
            if (repository.countNodeChildren(fileId) == 0) {
                repository.deleteNode(fileId);
                return Optional.empty();
            }
            return Optional.of(fileId);

        } catch (IOException e) {
            log.error(e, "Cannot read markdown file {}", resource.relativePath());
            return Optional.empty();
        }
    }


    private Optional<PlanNodeID> buildFeatureNode(
        MarkdownFeature feature,
        String relativePath,
        TagExpression tagExpression
    ) {
        Set<String> featureTags = filterIdTags(feature.tags);
        PlanNode featureData = new PlanNode(NodeType.TEST_AGGREGATOR)
            .name(feature.name)
            .identifier(identifierFromTags(feature.tags))
            .display("{name}")
            .tags(featureTags)
            .description(feature.description)
            .source(relativePath)
            .addProperties(feature.properties)
            .addProperty(MARKDOWN_TYPE, MARKDOWN_TYPE_FEATURE);

        PlanNodeID featureId = repository.persistNode(featureData);

        for (MarkdownTestCase testCase : feature.testCases) {
            Set<String> testCaseTags = new HashSet<>(featureTags);
            testCaseTags.addAll(filterIdTags(testCase.tags));

            if (!tagExpression.evaluate(testCaseTags)) continue;

            PlanNode testCaseData = new PlanNode(NodeType.TEST_CASE)
                .name(testCase.name)
                .identifier(identifierFromTags(testCase.tags))
                .display("{name}")
                .tags(testCaseTags)
                .description(testCase.description)
                .source(relativePath)
                .addProperty(MARKDOWN_TYPE, MARKDOWN_TYPE_TESTCASE);

            PlanNodeID testCaseId = repository.persistNode(testCaseData);

            for (MarkdownStepGroup group : testCase.stepGroups) {
                for (MarkdownStep step : group.steps) {
                    PlanNode stepData = new PlanNode(NodeType.STEP)
                        .name(step.text)
                        .keyword(group.keyword)
                        .display("{keyword} {name}")
                        .source(relativePath);
                    if (step.dataTable != null) stepData.dataTable(step.dataTable);
                    if (step.document != null) stepData.document(step.document);
                    repository.attachChildNodeLast(testCaseId, repository.persistNode(stepData));
                }
            }

            repository.attachChildNodeLast(featureId, testCaseId);
        }

        if (repository.countNodeChildren(featureId) == 0) {
            repository.deleteNode(featureId);
            return Optional.empty();
        }
        return Optional.of(featureId);
    }


    // -------------------------------------------------------------------------
    // Markdown document parsing
    // -------------------------------------------------------------------------

    /**
     * Parses a Markdown document into a list of {@link MarkdownFeature} sections,
     * one per H1 heading found in the document.
     */
    private List<MarkdownFeature> parseMarkdownSections(String content) {
        Node document = markdownParser.parse(content);
        List<MarkdownFeature> features = new ArrayList<>();

        // Feature-level accumulators
        String featureName = null;
        Set<String> featureTags = new LinkedHashSet<>();
        Map<String, String> featureProps = new LinkedHashMap<>();
        String featureDesc = null;
        List<MarkdownTestCase> testCases = new ArrayList<>();

        // Test-case-level accumulators
        String testCaseName = null;
        Set<String> testCaseTags = new LinkedHashSet<>();
        String testCaseDesc = null;
        List<MarkdownStepGroup> stepGroups = new ArrayList<>();

        // Step-group accumulators
        String currentKeyword = null;
        List<MarkdownStep> currentSteps = new ArrayList<>();

        // context: 1=feature header, 2=testcase header, 3=step body
        int context = 0;

        for (Node node = document.getFirstChild(); node != null; node = node.getNext()) {

            if (node instanceof Heading h) {
                switch (h.getLevel()) {
                    case 1 -> {
                        finalizeStepGroup(stepGroups, currentKeyword, currentSteps);
                        currentSteps.clear(); currentKeyword = null;
                        finalizeTestCase(testCases, testCaseName, testCaseTags, testCaseDesc, stepGroups);
                        testCaseName = null; testCaseTags.clear(); testCaseDesc = null; stepGroups = new ArrayList<>();
                        if (featureName != null) {
                            features.add(new MarkdownFeature(featureName, Set.copyOf(featureTags), Map.copyOf(featureProps), featureDesc, List.copyOf(testCases)));
                            testCases.clear();
                        }
                        featureName = textOf(h);
                        featureTags.clear(); featureProps.clear(); featureDesc = null;
                        context = 1;
                    }
                    case 2 -> {
                        finalizeStepGroup(stepGroups, currentKeyword, currentSteps);
                        currentSteps.clear(); currentKeyword = null;
                        finalizeTestCase(testCases, testCaseName, testCaseTags, testCaseDesc, stepGroups);
                        testCaseName = textOf(h);
                        testCaseTags.clear(); testCaseDesc = null; stepGroups = new ArrayList<>();
                        context = 2;
                    }
                    case 3 -> {
                        finalizeStepGroup(stepGroups, currentKeyword, currentSteps);
                        currentSteps.clear();
                        currentKeyword = textOf(h);
                        context = 3;
                    }
                }

            } else if (node instanceof BlockQuote bq) {
                Set<String> tags = extractTags(bq);
                if (context == 1) featureTags.addAll(tags);
                else if (context == 2) testCaseTags.addAll(tags);

            } else if (node instanceof HtmlBlock html) {
                if (context == 1) featureProps.putAll(extractProperties(html.getLiteral()));

            } else if (node instanceof Paragraph p) {
                String text = textOf(p);
                if (context == 1 && featureDesc == null) featureDesc = text;
                else if (context == 2 && testCaseDesc == null) testCaseDesc = text;

            } else if (node instanceof BulletList list) {
                if (context == 3) extractStepsFromList(list).forEach(currentSteps::add);

            } else if (node instanceof TableBlock table) {
                // Table preceded by a blank line: sibling of the BulletList → attach to last step
                if (context == 3 && !currentSteps.isEmpty()) {
                    int last = currentSteps.size() - 1;
                    currentSteps.set(last, currentSteps.get(last).withDataTable(tableOf(table)));
                }

            } else if (node instanceof FencedCodeBlock code) {
                // Code block preceded by a blank line: sibling of the BulletList → attach to last step
                if (context == 3 && !currentSteps.isEmpty()) {
                    int last = currentSteps.size() - 1;
                    currentSteps.set(last, currentSteps.get(last).withDocument(documentOf(code)));
                }
            }
        }

        // Finalize last accumulated elements
        finalizeStepGroup(stepGroups, currentKeyword, currentSteps);
        finalizeTestCase(testCases, testCaseName, testCaseTags, testCaseDesc, stepGroups);
        if (featureName != null) {
            features.add(new MarkdownFeature(featureName, Set.copyOf(featureTags), Map.copyOf(featureProps), featureDesc, List.copyOf(testCases)));
        }

        return features;
    }

    private static void finalizeStepGroup(List<MarkdownStepGroup> stepGroups, String keyword, List<MarkdownStep> steps) {
        if (keyword != null && !steps.isEmpty()) {
            stepGroups.add(new MarkdownStepGroup(keyword, List.copyOf(steps)));
        }
    }

    private static void finalizeTestCase(
        List<MarkdownTestCase> testCases,
        String name,
        Set<String> tags,
        String description,
        List<MarkdownStepGroup> stepGroups
    ) {
        if (name != null) {
            testCases.add(new MarkdownTestCase(name, Set.copyOf(tags), description, List.copyOf(stepGroups)));
        }
    }


    // -------------------------------------------------------------------------
    // Markdown extraction helpers
    // -------------------------------------------------------------------------

    private static String textOf(Node node) {
        var sb = new StringBuilder();
        collectText(node, sb);
        return sb.toString().strip();
    }

    private static void collectText(Node node, StringBuilder sb) {
        if (node instanceof Text t) sb.append(t.getLiteral());
        for (var child = node.getFirstChild(); child != null; child = child.getNext()) {
            collectText(child, sb);
        }
    }

    private static Set<String> extractTags(BlockQuote blockQuote) {
        Set<String> tags = new LinkedHashSet<>();
        for (String word : textOf(blockQuote).split("\\s+")) {
            if (word.startsWith("@")) tags.add(word.substring(1));
        }
        return tags;
    }

    private static Map<String, String> extractProperties(String htmlLiteral) {
        Map<String, String> props = new LinkedHashMap<>();
        String content = htmlLiteral
            .replaceAll("<!--+\\s*", "")
            .replaceAll("\\s*--+>", "");
        for (String line : content.split("\n")) {
            Matcher m = PROPERTY_LINE.matcher(line);
            if (m.matches()) props.put(m.group(1), m.group(2).strip());
        }
        return props;
    }

    /**
     * Extracts steps from a bullet list. Each {@link ListItem} may contain:
     * <ul>
     *   <li>A {@link Paragraph} with the step text (always first child)</li>
     *   <li>An optional {@link TableBlock} child → {@link DataTable} argument</li>
     *   <li>An optional {@link FencedCodeBlock} child → {@link Document} argument</li>
     * </ul>
     * CommonMark places tables and fenced code blocks <em>inside</em> the list item
     * when they follow the step text without a blank line, so we look for them there.
     */
    private static List<MarkdownStep> extractStepsFromList(BulletList list) {
        List<MarkdownStep> steps = new ArrayList<>();
        for (Node item = list.getFirstChild(); item != null; item = item.getNext()) {
            if (!(item instanceof ListItem li)) continue;
            String text = null;
            DataTable table = null;
            Document document = null;
            for (Node child = li.getFirstChild(); child != null; child = child.getNext()) {
                if (child instanceof Paragraph p && text == null) {
                    text = firstLineOf(p);
                } else if (child instanceof TableBlock tb && table == null) {
                    table = tableOf(tb);
                } else if (child instanceof FencedCodeBlock fcb && document == null) {
                    document = documentOf(fcb);
                }
            }
            if (text != null) steps.add(new MarkdownStep(text, table, document));
        }
        return steps;
    }

    /** Returns only the text of the first line of a paragraph (up to the first soft/hard line break). */
    private static String firstLineOf(Paragraph p) {
        var sb = new StringBuilder();
        for (Node child = p.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof SoftLineBreak || child instanceof HardLineBreak) break;
            if (child instanceof Text t) sb.append(t.getLiteral());
        }
        return sb.toString().strip();
    }

    private static DataTable tableOf(TableBlock tableBlock) {
        List<List<String>> rows = new ArrayList<>();
        for (Node section = tableBlock.getFirstChild(); section != null; section = section.getNext()) {
            for (Node row = section.getFirstChild(); row != null; row = row.getNext()) {
                if (row instanceof TableRow tr) {
                    List<String> cells = new ArrayList<>();
                    for (Node cell = tr.getFirstChild(); cell != null; cell = cell.getNext()) {
                        if (cell instanceof TableCell tc) cells.add(textOf(tc));
                    }
                    rows.add(List.copyOf(cells));
                }
            }
        }
        return new DataTable(rows);
    }

    private static Document documentOf(FencedCodeBlock code) {
        String info = code.getInfo();
        return new Document(info.isBlank() ? null : info, code.getLiteral());
    }


    // -------------------------------------------------------------------------
    // Tag helpers
    // -------------------------------------------------------------------------

    private String identifierFromTags(Set<String> tags) {
        for (String tag : tags) {
            Matcher m = idTagPattern.matcher(tag);
            if (m.find()) return m.groupCount() > 0 ? m.group(1) : m.group(0);
        }
        return null;
    }

    private Set<String> filterIdTags(Set<String> tags) {
        return tags.stream()
            .filter(idTagPattern.asPredicate().negate())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }


    // -------------------------------------------------------------------------
    // Internal data structures
    // -------------------------------------------------------------------------

    private record MarkdownFeature(
        String name,
        Set<String> tags,
        Map<String, String> properties,
        String description,
        List<MarkdownTestCase> testCases
    ) {}

    private record MarkdownTestCase(
        String name,
        Set<String> tags,
        String description,
        List<MarkdownStepGroup> stepGroups
    ) {}

    private record MarkdownStepGroup(String keyword, List<MarkdownStep> steps) {}

    private record MarkdownStep(String text, DataTable dataTable, Document document) {
        static MarkdownStep of(String text) { return new MarkdownStep(text, null, null); }
        MarkdownStep withDataTable(DataTable dt) { return new MarkdownStep(text, dt, document); }
        MarkdownStep withDocument(Document doc) { return new MarkdownStep(text, dataTable, doc); }
    }
}