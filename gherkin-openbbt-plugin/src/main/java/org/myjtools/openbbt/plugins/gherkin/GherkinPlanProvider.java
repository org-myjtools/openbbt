package org.myjtools.openbbt.plugins.gherkin;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.myjtools.openbbt.core.plan.PlanNodeID;
import org.myjtools.openbbt.core.plan.PlanProvider;
import org.myjtools.openbbt.core.resources.Resource;
import java.util.List;
import java.util.Optional;

@Extension(scope = Scope.SINGLETON)
public class FeaturePlanProvider implements PlanProvider {


    @Override
    public boolean accept(Resource resource) {
        return false;
    }

    @Override
    public Optional<PlanNodeID> providePlan(List<Resource> resources) {
        return Optional.empty();
    }
}
