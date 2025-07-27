package org.myjtools.openbbt.core.adapters;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.contributors.MessageProvider;
import java.util.Locale;
import java.util.Properties;

/**
 * Abstract adapter for providing localized messages.
 * This class uses a resource file to load messages based on the locale.
 * The resource file should be named in the format "resourceName_language.properties".
 * For example, if the resourceName is "messages" and the locale is "en", the file should be "messages_en.properties".
 */
public abstract class MessageAdapter implements MessageProvider {



    protected static class ResourceMessages implements Messages {

        private final Properties properties;

        public ResourceMessages(String resourceName, Locale locale, Module module) {
            this.properties = new Properties();
            try {
                properties.load(module.getResourceAsStream(resourceName + "_" + locale.getLanguage() + ".properties"));
            } catch (Exception e) {
                throw new OpenBBTException(e,"Could not load messages for {} in locale {}", resourceName, locale);
            }
        }

        @Override
        public String get(String key) {
            return properties.getProperty(key);
        }
    }



    private final String resourceName;

    private final LoadingCache<Locale, Messages> messages = Caffeine.newBuilder()
        .maximumSize(10) // max number of messages to cache
        .build(this::createMessages);

    protected MessageAdapter(String resourceName) {
        this.resourceName = resourceName;
    }


    private Messages createMessages(Locale locale) {
        return new ResourceMessages(resourceName, locale, getClass().getModule());
    }


    @Override
    public Messages messages(Locale locale) {
        return messages.get(locale);
    }



}
