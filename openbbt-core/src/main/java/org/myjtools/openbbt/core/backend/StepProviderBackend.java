package org.myjtools.openbbt.core.backend;

import org.myjtools.openbbt.core.*;
import org.myjtools.openbbt.core.contributors.AssertionFactoryProvider;
import org.myjtools.openbbt.core.contributors.DataTypeProvider;
import org.myjtools.openbbt.core.contributors.StepProvider;
import org.myjtools.openbbt.core.execution.NoMatchingStepException;
import org.myjtools.openbbt.core.expressions.Match;
import org.myjtools.openbbt.core.messages.MessageProvider;
import org.myjtools.openbbt.core.messages.Messages;
import org.myjtools.openbbt.core.plan.NodeArgument;
import org.myjtools.openbbt.core.util.Pair;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StepProviderBackend {

	private final List<StepProviderService> services = new ArrayList<>();
	private final ConcurrentHashMap<String,Object> variables = new ConcurrentHashMap<>();
	private final StepProviderHinter hinter;

	public StepProviderBackend(OpenBBTRuntime cm) {
		var dataTypes = DataTypes.of(cm.getExtensions(DataTypeProvider.class)
			.flatMap(DataTypeProvider::dataTypes)
			.toList());
		var assertionFactories = AssertionFactories.of(cm.getExtensions(AssertionFactoryProvider.class)
			.flatMap(AssertionFactoryProvider::assertionFactories)
			.toList());
		var stepProviders = cm.getExtensions(StepProvider.class).toList();
		for (var stepProvider : stepProviders) {
			Messages messages = Messages.of(cm.getExtensions(MessageProvider.class)
				.filter(mp -> mp.providerFor(stepProvider.getClass().getSimpleName()))
				.toList()
			);
			services.add(new StepProviderService(stepProvider, dataTypes, assertionFactories, messages));
		}
		this.hinter = new StepProviderHinter(services);
	}

	public void setUp() {
		for (var service : services) {
			service.setUp();
		}
	}

	public void tearDown() {
		for (var service : services) {
			service.tearDown();
		}
	}

	private Optional<Pair<StepProviderMethod, Match>> matchingStep(String step, Locale locale) {
		for (var service : services) {
			var match = service.matchingStep(step, locale);
			if (match.isPresent()) {
				return match;
			}
		}
		return Optional.empty();
	}

	public void run(String step, Locale locale, NodeArgument nodeArgument) {
		var matchingStep = matchingStep(step,locale).orElseThrow(
			() -> new NoMatchingStepException(
				"No matching step found for '{}'\n{}",
				step,
				hints(step,locale)
			)
		);
		var stepMethod = matchingStep.left();
		var match = matchingStep.right();
		Map<String,Object> arguments = match.interpolateArguments(variables);
		Assertion assertion = match.assertion();
		try {
			// expected either an assertion or a node argument, but not both.
			// If both are provided, the node argument takes precedence over the assertion
			stepMethod.run(arguments, nodeArgument != null ? nodeArgument : assertion);
		} catch (AssertionError | OpenBBTException e) {
			// AssertionErrors are not wrapped in OpenBBTException, as they are expected to be thrown by
			// the step methods when an assertion fails
			throw e;
		}  catch (Throwable e) {
			throw new OpenBBTException(e);
		}
	}



	private String hints(String invalidStep, Locale locale) {
		int maxSuggestions = 5;
		StringBuilder hint = new StringBuilder(
			"Perhaps you mean one of the following:\n\t----------\n\t"
		);
		var allSuggestions = hinter.getHintsForInvalidStep(invalidStep, locale,  -1);
		if (allSuggestions.size() > maxSuggestions) {
			allSuggestions = hinter.getHintsForInvalidStep(invalidStep, locale,  maxSuggestions);
		}
		for (String stepHint : allSuggestions) {
			hint.append(stepHint).append("\n\t");
		}
		return hint.toString();
	}


}
