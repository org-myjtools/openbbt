package org.myjtools.openbbt.core.test;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.contributors.EventObserver;
import org.myjtools.openbbt.core.events.Event;
import org.myjtools.openbbt.core.events.EventBus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestEventBus {

	@Test
	void publishNotifiesRegisteredObserversInOrder() {
		var eventBus = new EventBus();
		var received = new ArrayList<String>();
		var event = new SampleEvent("payload", Instant.EPOCH);

		eventBus.registerObserver(capture(received, "first"));
		eventBus.registerObserver(capture(received, "second"));

		eventBus.publish(event);

		assertThat(received).containsExactly("first:payload", "second:payload");
	}

	private static EventObserver capture(List<String> received, String name) {
		return event -> received.add(name + ":" + ((SampleEvent) event).value());
	}

	private record SampleEvent(String value, Instant instant) implements Event {}
}
