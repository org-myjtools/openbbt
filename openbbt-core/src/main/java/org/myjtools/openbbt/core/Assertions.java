package org.myjtools.openbbt.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Assertions {


    public static Assertions of(Assertion... assertions) {
        return new Assertions(assertions);
    }

    private final Map<String, Assertion> assertionsByName = new HashMap<>();

    private Assertions(Assertion[] assertions) {
        for (Assertion assertion : assertions) {
            this.assertionsByName.put(assertion.name(), assertion);
        }
    }

    public Assertion byName(String value) {
        Assertion assertion = assertionsByName.get(value);
        if (assertion == null) {
            throw new OpenBBTException(
                "Unknown assertion {}\n\tAccepted assertions are: {}",
                value,
                String.join(", ", assertionsByName.keySet())
            );
        }
        return assertion;
    }

}
