package org.myjtools.openbbt.lsp;

import org.eclipse.lsp4j.*;
import org.myjtools.gherkinparser.DefaultKeywordMapProvider;
import org.myjtools.gherkinparser.KeywordType;
import org.myjtools.openbbt.core.backend.StepProviderBackend;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FeatureDiagnosticsProvider {

    static final String SOURCE = "openbbt-step";
    static final int MAX_HINTS = 5;

    private final StepProviderBackend backend;
    private final DefaultKeywordMapProvider keywordProvider;

    public FeatureDiagnosticsProvider(StepProviderBackend backend, DefaultKeywordMapProvider keywordProvider) {
        this.backend = backend;
        this.keywordProvider = keywordProvider;
    }

    public List<Diagnostic> validate(String content) {
        Locale locale = localeFromContent(content);
        List<String> stepKeywords = stepKeywordsForLocale(locale);
        String[] lines = content.split("\n", -1);
        var diagnostics = new ArrayList<Diagnostic>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.stripLeading();

            for (String kw : stepKeywords) {
                if (!trimmed.startsWith(kw + " ")) continue;

                String stepText = trimmed.substring(kw.length() + 1).stripTrailing();
                if (stepText.isBlank()) break;

                if (!backend.isValidStep(stepText, locale)) {
                    int startCol = line.indexOf(kw) + kw.length() + 1;
                    int endCol   = line.stripTrailing().length();
                    var range = new Range(new Position(i, startCol), new Position(i, endCol));
                    var diag  = new Diagnostic(range, "Undefined step: '" + stepText + "'",
                        DiagnosticSeverity.Error, SOURCE);
                    diagnostics.add(diag);
                }
                break;
            }
        }
        return diagnostics;
    }

    public List<String> hintsFor(String stepText, Locale locale) {
        return backend.hintsForStep(stepText, locale, MAX_HINTS);
    }

    private List<String> stepKeywordsForLocale(Locale locale) {
        return keywordProvider.keywordMap(locale)
            .map(km -> km.keywords(KeywordType.STEP))
            .orElse(List.of("Given", "When", "Then", "And", "But"));
    }

    private Locale localeFromContent(String content) {
        for (String line : content.split("\n", 5)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# language:")) {
                return Locale.forLanguageTag(trimmed.substring("# language:".length()).trim());
            }
        }
        return Locale.getDefault();
    }
}
