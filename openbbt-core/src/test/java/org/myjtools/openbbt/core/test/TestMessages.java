package org.myjtools.openbbt.core.test;

import org.myjtools.openbbt.core.messages.LocaleMessages;
import org.myjtools.openbbt.core.messages.MessageProvider;
import org.myjtools.openbbt.core.messages.Messages;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class TestMessages extends Messages {

	public TestMessages (Map<Locale, Map<String,String>> messages) {
		super(List.of(createMessageProvider(messages)));
	}

	private static MessageProvider createMessageProvider(Map<Locale, Map<String,String>> messages) {
		return locale -> {
			Map<String, String> localizedMessages = messages.get(locale);
			if (localizedMessages != null) {
				return Optional.of(new LocaleMessages() {
					@Override
					public String get(String key) {
						return localizedMessages.getOrDefault(key, key);
					}
				});
			}
			return Optional.empty();
		};
	}
}
