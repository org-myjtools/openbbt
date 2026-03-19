package org.myjtools.openbbt.cli;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import picocli.CommandLine;

import java.util.List;
import java.util.Map;

@CommandLine.Command(
    name = "list-contributors",
    description = "List all registered contributors organized by type"
)
public final class ListContributorsCommand extends AbstractCommand {

    @CommandLine.Option(
        names = {"--json"},
        description = "Output as JSON array",
        defaultValue = "false"
    )
    boolean json;

    @Override
    protected void execute() {
        OpenBBTRuntime runtime = new OpenBBTRuntime(getContext().configuration());
        Map<String, List<String>> contributors = runtime.getContributors();

        if (json) {
            JsonArray result = new JsonArray();
            contributors.forEach((type, implementations) -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("type", type);
                JsonArray impls = new JsonArray();
                implementations.forEach(impls::add);
                obj.add("implementations", impls);
                result.add(obj);
            });
            System.out.println(result);
        } else {
            contributors.forEach((type, implementations) -> {
                System.out.println(type);
                implementations.forEach(impl -> System.out.println("  " + impl));
            });
        }
    }
}