package org.myjtools.openbbt.core.messages;

import java.util.List;


/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
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
