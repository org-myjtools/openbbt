package org.myjtools.openbbt.core.adapters;

import org.myjtools.openbbt.core.contributors.LocaleMessages;
import org.myjtools.openbbt.core.contributors.MessageProvider;

import java.util.List;


public class Messages {

    private final List<MessageProvider> providers;


    public Messages(List<MessageProvider> providers) {
        this.providers = providers;
    }


    public LocaleMessages forLocale(java.util.Locale locale) {
        return providers.stream()
                .flatMap(provider -> provider.messages(locale).stream())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No messages found for locale: " + locale));
    }

}
