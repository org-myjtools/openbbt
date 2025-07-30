package org.myjtools.openbbt.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AssertionFactories {


    public static AssertionFactories of(AssertionFactory<?>... assertionFactories) {
        return new AssertionFactories(assertionFactories);
    }

    public static AssertionFactories of(Collection<AssertionFactory<?>> assertionFactories) {
        return new AssertionFactories(assertionFactories.toArray(AssertionFactory[]::new));
    }

    private final Map<String, AssertionFactory<?>> assertionFactoriesByName = new HashMap<>();

    private AssertionFactories(AssertionFactory<?>[] assertionFactories) {
        for (AssertionFactory<?> assertionFactory : assertionFactories) {
            this.assertionFactoriesByName.put(assertionFactory.name(), assertionFactory);
        }
    }

    public AssertionFactory<?> byName(String value) {
        AssertionFactory<?> assertionFactory = assertionFactoriesByName.get(value);
        if (assertionFactory == null) {
            throw new OpenBBTException(
                "Unknown assertion {}\n\tAccepted assertions are: {}",
                value,
                String.join(", ", assertionFactoriesByName.keySet())
            );
        }
        return assertionFactory;
    }

}
