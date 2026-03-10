package org.myjtools.openbbt.cli;

import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.cli.serve.JsonRpcServer;
import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import picocli.CommandLine;

@CommandLine.Command(
    name = "serve",
    description = "Start OpenBBT in server mode, serving JSON-RPC 2.0 requests over stdio"
)
public final class ServeCommand extends AbstractCommand {

    @Override
    protected void execute() {
        OpenBBTContext context = getContext();
        Config config = context.configuration();
        new JsonRpcServer(
            System.in,
            System.out,
            () -> OpenBBTRuntime.repositoryOnly(config).getRepository(
                org.myjtools.openbbt.core.persistence.TestPlanRepository.class
            )
        ).run();
    }
}