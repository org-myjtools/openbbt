package org.myjtools.openbbt.api.expressions;

import org.myjtools.openbbt.api.Assertions;
import org.myjtools.openbbt.api.DataTypes;
import org.myjtools.openbbt.api.util.Patterns;

import java.nio.CharBuffer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ExpressionMatcherBuilder {


    private static final Pattern regexSymbols = CharBuffer.wrap(ExpressionTokenizer.symbols)
            .chars()
            .mapToObj(n -> "\\"+Character.toString(n))
            .reduce((a,b)->a+b)
            .map(it -> Pattern.compile("["+it+"]"))
            .orElseThrow();


    private final DataTypes dataTypes;
    private final Assertions assertions;


    public ExpressionMatcherBuilder(DataTypes dataTypes, Assertions assertions) {
        this.dataTypes = dataTypes;
        this.assertions = assertions;
    }


    public ExpressionMatcher buildExpressionMatcher(String expression) {
        var tree = new ExpressionASTBuilder(expression).buildTree();
        List<FragmentMatcher> fragments = buildFragmentMatchers(tree);
        return new ExpressionMatcher(fragments);
    }



    private String regex(ExpressionASTNode tree) {
        return switch (tree.type()) {
            case WILDCARD -> "(.*)";
            case NEGATION -> "(?!"+regex(tree.firstChild())+")" + (isWord(tree.firstChild()) ? "\\S+" : ".*");
            case LITERAL -> "("+ regexSymbols.matcher(tree.value).replaceAll("\\\\$0").replace(" ","\\s+")+")";
            case OPTIONAL -> regex(tree.firstChild())+"?";
            case CHOICE -> tree.children().stream().map(this::regex).collect(Collectors.joining("|","(",")"));
            case SEQUENCE -> tree.children().stream().map(this::regex).collect(Collectors.joining());
            default -> "";
        };
    }

    private String literal(ExpressionASTNode tree) {
        var child = tree.firstChild();
        return switch (tree.type()) {
            case WILDCARD -> " * ";
            case NEGATION -> isWord(child) ? "^"+literal(child) : "^["+literal(child)+"]";
            case LITERAL -> escape(tree.value());
            case OPTIONAL -> "("+literal(child)+")";
            case CHOICE -> tree.children().stream().map(this::regex).collect(Collectors.joining("|","(",")"));
            case SEQUENCE -> tree.children().stream().map(this::regex).collect(Collectors.joining());
            default -> "";
          };
    }


    private String joinRegex(PatternFragmentMatcher regex1, PatternFragmentMatcher regex2) {
        String regex1Pattern = regex1.pattern().pattern();
        String regex2Pattern = regex2.pattern().pattern();
        if (regex1Pattern.endsWith("s+)") && regex2Pattern.endsWith(")?")) {
            regex1Pattern = regex1Pattern.substring(0, regex1Pattern.length() - 3) + "s*)";
        }
        if (regex1Pattern.endsWith("s+)") && regex2Pattern.equals("(.*)")) {
            regex1Pattern = regex1Pattern.substring(0, regex1Pattern.length() - 3) + "s*)";
        }
        String regex = regex1Pattern + regex2Pattern;
        // temporary replacement required to proper match of the following expression
        regex = regex.replace("\\)",">>>>>");
        regex = Patterns.replace(
            regex,
            "\\\\s\\+\\)\\(([^)]+)\\)\\?\\(\\\\s\\+",
            "\\\\s+)($1\\\\s+)?("
        );
        regex = regex.replace(">>>>>","\\)");
        return regex;
    }


    private String joinLiteral(PatternFragmentMatcher regex1, PatternFragmentMatcher regex2) {
        return regex1.literal()+regex2.literal();
    }


    private FragmentMatcher buildSingleFragment(ExpressionASTNode tree) {
        String regex = regex(tree);
        String literal = literal(tree);
        if (regex != null) {
            return new PatternFragmentMatcher(regex,literal);
        } else if (tree.type == ExpressionASTNode.Type.ARGUMENT) {
            String[] valueParts = tree.value.split(":");
            if (valueParts.length == 1) {
                return new ArgumentFragmentMatcher(dataTypes.byName(tree.value).orElseThrow());
            } else {
                return new ArgumentFragmentMatcher(valueParts[0],dataTypes.byName(valueParts[1]).orElseThrow());
            }
        } else if (tree.type == ExpressionASTNode.Type.ASSERTION) {
            return new AssertionFragmentMatcher(assertions.byName(tree.value).orElseThrow());
        }
        throw new ExpressionException("cannot build a single fragment for {}", tree);
    }



    private List<FragmentMatcher> buildFragmentMatchers(ExpressionASTNode tree) {
        if (tree.type() == ExpressionASTNode.Type.SEQUENCE) {
            LinkedList<FragmentMatcher> fragments = new LinkedList<>();
            FragmentMatcher lastFragment = null;
            for (ExpressionASTNode child : tree.children) {
                FragmentMatcher childFragment = buildSingleFragment(child);
                if (lastFragment instanceof PatternFragmentMatcher regex1 &&
                        childFragment instanceof PatternFragmentMatcher regex2
                ) {
                    fragments.removeLast();
                    fragments.add(new PatternFragmentMatcher(
                        joinRegex(regex1, regex2),
                        joinLiteral(regex1,regex2)
                    ));
                } else {
                    fragments.add(childFragment);
                }
                lastFragment = fragments.getLast();
            }
            return fragments;
        } else {
            return List.of(buildSingleFragment(tree));
        }
    }



    private static boolean isWord(ExpressionASTNode tree) {
        return tree.type() == ExpressionASTNode.Type.LITERAL && !tree.value().strip().contains(" ");
    }


    private static String escape(String text) {
        return regexSymbols.matcher(text).replaceAll("\\\\$0");
    }

}
