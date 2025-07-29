package org.myjtools.openbbt.core;

import org.myjtools.openbbt.core.adapters.AssertionFactory;

import java.util.HashMap;
import java.util.Map;

public class AssertionFactories {


    public static AssertionFactories of(AssertionFactory... assertionFactories) {
        return new AssertionFactories(assertionFactories);
    }

    private final Map<String, AssertionFactory> assertionFactoriesByName = new HashMap<>();

    private AssertionFactories(AssertionFactory[] assertionFactories) {
        for (AssertionFactory assertionFactory : assertionFactories) {
            this.assertionFactoriesByName.put(assertionFactory.name(), assertionFactory);
        }
    }

    public AssertionFactory byName(String value) {
        AssertionFactory assertionFactory = assertionFactoriesByName.get(value);
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
