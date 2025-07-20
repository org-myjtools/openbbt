package org.myjtools.openbbt.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Assertions {


    public static Assertions of(Assertion... assertions) {
        return new Assertions(assertions);
    }

    private final Map<String, Assertion> assertions = new HashMap<>();

    private Assertions(Assertion[] assertions) {
        for (Assertion assertion : assertions) {
            this.assertions.put(assertion.name(), assertion);
        }
    }

    public Optional<Assertion> byName(String value) {
        return Optional.ofNullable(assertions.get(value));
    }
}
