package org.myjtools.openbbt.core.messages;

import org.myjtools.jexten.ExtensionPoint;

import java.util.Locale;
import java.util.Optional;

@ExtensionPoint
public interface MessageProvider {

    /**
     * Returns a Messages instance for the specified locale.
     *
     * @param locale the locale for which messages are requested
     * @return a Messages instance containing localized messages
    
 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
    Optional<LocaleMessages> messages(Locale locale);

}
