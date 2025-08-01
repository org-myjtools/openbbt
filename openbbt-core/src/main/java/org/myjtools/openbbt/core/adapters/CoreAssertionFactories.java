package org.myjtools.openbbt.core.adapters;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.jexten.Scope;
import org.myjtools.openbbt.core.AssertionFactory;
import org.myjtools.openbbt.core.contributors.AssertionFactoryProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.stream.Stream;

@Extension(scope = Scope.SINGLETON)
public class CoreAssertionFactories implements AssertionFactoryProvider {

    @Inject("assertions")
    Messages messages;

    public CoreAssertionFactories() {
        // default constructor
    }

    public CoreAssertionFactories(Messages messages) {
        // this constructor only exists for testing purposes
        this.messages = messages;
    }


    @Override
    public Stream<AssertionFactory<?>> assertionFactories() {
        return Stream.of(
            new ComparableAssertionFactory<>("number-assertion", Integer::valueOf, CoreDataTypes.NUMBER, messages),
            new ComparableAssertionFactory<>("decimal-assertion", BigDecimal::new, CoreDataTypes.DECIMAL, messages),
            new TemporalAssertionFactory<>("date-assertion", LocalDate::parse, CoreDataTypes.DATE, messages),
            new TemporalAssertionFactory<>("time-assertion", LocalTime::parse, CoreDataTypes.TIME, messages),
            new TemporalAssertionFactory<>("datetime-assertion", LocalDateTime::parse, CoreDataTypes.DATE_TIME, messages),
            new StringAssertionFactory("text-assertion", CoreDataTypes.TEXT, messages)
        );
    }

}
