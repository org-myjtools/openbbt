package org.myjtools.openbbt.api.util;


import org.myjtools.openbbt.api.OpenBBTException;

import java.time.chrono.IsoChronology;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;
import java.util.stream.*;

public final class DateTimeFormats {

    private DateTimeFormats() { }


    public record FormatStyles(FormatStyle date, FormatStyle time) { }
    public record Criteria(Locale locale, boolean withDate, boolean withTime) { }

    private static final List<FormatStyle> FORMAT_STYLES = List.of(
        FormatStyle.SHORT,
        FormatStyle.MEDIUM,
        FormatStyle.LONG,
        FormatStyle.FULL
    );

    private static final String[] ISO_8601_DATE_FORMATS = {
        "yyyy-MM-dd"
    };

    private static final String[] ISO_8601_TIME_FORMATS = {
        "hh:mm",
        "hh:mm:ss",
        "hh:mm:ss.SSS"
    };

    private static final String[] ISO_8601_DATETIME_FORMATS = {
        "yyyy-MM-dd'T'hh:mm",
        "yyyy-MM-dd'T'hh:mm:ss",
        "yyyy-MM-dd'T'hh:mm:ss.SSS"
    };

    private static final String REGEX_ALPHAS = "[^\\s]+";
    private static final String REGEX_2_NUMBER = "[0-9]{2}";
    private static final String REGEX_3_NUMBER = "[0-9]{3}";
    private static final String REGEX_4_NUMBER = "[0-9]{4}";
    private static final String REGEX_1_2_NUMBER = "[0-9]{1,2}";
    private static final String REGEX_1_3_NUMBER = "[0-9]{1,3}";
    private static final String REGEX_2_3_NUMBER = "[0-9]{2,3}";
    private static final String REGEX_2_4_NUMBER = "[0-9]{2,4}";

    private static final Map<String, String> regexSymbols = Stream.of(
        Map.entry("MMMM", REGEX_ALPHAS),
        Map.entry("EEEE", REGEX_ALPHAS),
        Map.entry("yyyy", REGEX_4_NUMBER ),
        Map.entry("MMM", REGEX_ALPHAS ),
        Map.entry("EEE", REGEX_ALPHAS ),
        Map.entry("SSS", REGEX_3_NUMBER ),
        Map.entry("MM", REGEX_2_NUMBER ),
        Map.entry("EE", REGEX_ALPHAS ),
        Map.entry("HH", REGEX_2_NUMBER ),
        Map.entry("hh", REGEX_2_NUMBER ),
        Map.entry("dd", REGEX_2_NUMBER ),
        Map.entry("mm", REGEX_2_NUMBER ),
        Map.entry("SS", REGEX_2_3_NUMBER ),
        Map.entry("yy", REGEX_2_NUMBER ),
        Map.entry("ss", REGEX_2_NUMBER ),
        Map.entry("M", REGEX_1_2_NUMBER ),
        Map.entry("d", REGEX_1_2_NUMBER ),
        Map.entry("E", REGEX_ALPHAS ),
        Map.entry("H", REGEX_1_2_NUMBER ),
        Map.entry("h", REGEX_1_2_NUMBER ),
        Map.entry("m", REGEX_1_2_NUMBER ),
        Map.entry("s", REGEX_1_2_NUMBER ),
        Map.entry("S", REGEX_1_3_NUMBER ),
        Map.entry("a", REGEX_ALPHAS ),
        Map.entry("z", REGEX_ALPHAS ),
        Map.entry("y", REGEX_2_4_NUMBER )
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b)->a, LinkedHashMap::new));

    private static final List<String> REGEX_SPECIAL_SYMBOLS = List.of(
        "\\",
        "[",
        "^",
        "$",
        ".",
        ",",
        "|",
        "?",
        "*",
        "+",
        "(",
        ")",
        "/",
        "-",
        ":"
    );


    public static String dateTimeRegex(Criteria criteria) {
        final Set<String> regexs = new HashSet<>();
        for (var a : formatStyles(criteria.withDate,criteria.withTime)) {
            regexs.add(dateTimeRegex(criteria.locale, a));
        }
        regexs.addAll(dateTimeRegexISO(criteria.withDate, criteria.withTime));
        return "(" + String.join(")|(", regexs) + ")";
    }



    private static List<String> dateTimeRegexISO(boolean withDate, boolean withTime) {
        String[] formats;
        if (withDate && withTime) formats = ISO_8601_DATETIME_FORMATS;
        else if (withDate) formats = ISO_8601_DATE_FORMATS;
        else formats = ISO_8601_TIME_FORMATS;
        return Stream.of(formats).map(DateTimeFormats::patternToRegex).toList();
    }


    private static String patternToRegex(String formatPattern) {
        List<String> tokens = new ArrayList<>(regexSymbols.keySet());
        tokens.addAll(REGEX_SPECIAL_SYMBOLS);
        tokens.add(" ");
        TokenParser parser = new TokenParser(formatPattern, tokens, List.of("'[^']*'"));
        StringBuilder regex = new StringBuilder();
        while (parser.hasMoreTokens()) {
            String nextToken = parser.nextToken();
            if (nextToken.equals(" ")) {
                regex.append(" ");
            } else if (REGEX_SPECIAL_SYMBOLS.contains(nextToken)) {
                regex.append("\\").append(nextToken);
            } else if (nextToken.startsWith("'")) {
                regex.append(nextToken.replace("'", ""));
            } else {
                String regexSymbol = regexSymbols.get(nextToken);
                if (regexSymbol == null) {
                    throw new OpenBBTException("Date/time format symbol '{}' has no equivalent regex", nextToken);
                }
                regex.append(regexSymbols.get(nextToken));
            }
        }
        return regex.toString();
    }


    private static String dateTimeRegex(Locale locale, FormatStyles formatStyles) {
        return patternToRegex(dateTimePattern(locale, formatStyles));
    }


    private static String dateTimePattern(Locale locale, FormatStyles formatStyles) {
        return DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            formatStyles.date,
            formatStyles.time,
            IsoChronology.INSTANCE,
            locale
        );
    }




    public static List<FormatStyles> formatStyles(boolean withDate, boolean withTime) {
        List<FormatStyles> result = new ArrayList<>();
        if (withDate && withTime) {
            for (FormatStyle date : FORMAT_STYLES) {
                for (FormatStyle time : FORMAT_STYLES) {
                    result.add(new FormatStyles(date,time));
                }
            }
        } else {
            return
                FORMAT_STYLES.stream()
                    .map(it -> new FormatStyles(withDate ? it : null, withTime ? it : null))
                    .toList();
        }
        return result;
    }




    public static List<String> dateTimePatterns(Criteria criteria) {
        List<String> patterns = new ArrayList<>();
        if (criteria.withDate && criteria.withTime) {
            patterns.addAll(List.of(ISO_8601_DATETIME_FORMATS));
        } else if (criteria.withDate) {
            patterns.addAll(List.of(ISO_8601_DATE_FORMATS));
        } else {
            patterns.addAll(List.of(ISO_8601_TIME_FORMATS));
        }
        for (var formatStyles : formatStyles(criteria.withDate, criteria.withTime)) {
            patterns.add(dateTimePattern(criteria.locale,formatStyles));
        }
        return patterns;
    }




    public static DateTimeFormatter formatter(
        Locale locale,
        FormatStyles formatStyles
    ) {
        return new DateTimeFormatterBuilder().parseCaseInsensitive().append(
            DateTimeFormatter.ofPattern(dateTimePattern(locale, formatStyles))
        ).toFormatter(locale);
    }



    public static <T extends TemporalAccessor> T parse(
        List<DateTimeFormatter> formatters,
        String input,
        TemporalQuery<T> temporalQuery
    ) {
        RuntimeException ex = new RuntimeException();
        for (DateTimeFormatter formatter : formatters) {
            try {
                return formatter.parse(input, temporalQuery);
            } catch (DateTimeParseException e) {
                ex = e;
            }
        }
        throw ex;
    }




}
