package org.myjtools.openbbt.api.util;

import java.util.List;
import java.util.regex.*;
import java.util.stream.Collectors;


public class TokenParser {

    private final List<Pattern> tokens;

    private String remainString;
    private String nextToken;


    public TokenParser(String string, List<String> literals, List<String> regex) {
        this.remainString = string;
        this.tokens = regex.stream().map(TokenParser::regex).collect(Collectors.toList());
        for (String literal : literals) {
            this.tokens.add(TokenParser.literal(literal));
        }
        computeNextToken();
    }


    public boolean hasMoreTokens() {
        return nextToken != null;
    }


    public String nextToken() {
        String token = this.nextToken;
        computeNextToken();
        return token;
    }


    private void computeNextToken() {
        nextToken = computeMaxToken(remainString);
        if (nextToken == null) {
            remainString = null;
        } else {
            remainString = remainString.substring(nextToken.length());
        }
    }


    private String computeMaxToken(String string) {
        return tokens.stream()
            .map(token -> token.matcher(string))
            .filter(Matcher::matches)
            .map(matcher -> matcher.group(1))
            .reduce((match1, match2) -> match1.length() > match2.length() ? match1 : match2)
            .orElse(null);
    }


    private static Pattern regex(String regex) {
        return Pattern.compile("(" + regex + ").*");
    }


    private static Pattern literal(String literal) {
        return Pattern.compile("(" + Pattern.quote(literal) + ").*");
    }

}
