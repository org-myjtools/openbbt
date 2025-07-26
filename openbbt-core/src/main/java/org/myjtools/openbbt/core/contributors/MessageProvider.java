package org.myjtools.openbbt.core.contributors;

import org.myjtools.jexten.ExtensionPoint;

import java.util.Locale;

@ExtensionPoint
public interface MessageProvider {

    /**
     * Interface for providing localized messages.
     */
    interface Messages {
        String get(String key);
    }

    /**
     * Returns a Messages instance for the specified locale.
     *
     * @param locale the locale for which messages are requested
     * @return a Messages instance containing localized messages
     */
    Messages messages(Locale locale);

}
