package be.webtechie.crac.example.manager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;

import java.util.Arrays;

public class ServerManager implements Resource {

    private static final Logger LOGGER = LogManager.getLogger(ServerManager.class);

    private final Server server;

    public ServerManager(int port, Handler handler) throws Exception {
        server = new Server(port);
        server.setHandler(handler);
        server.start();
        Core.getGlobalContext().register(this);
    }

    @Override
    public synchronized void beforeCheckpoint(Context<? extends Resource> context) {
        LOGGER.info("Executing beforeCheckpoint");
        // Stop the connectors only and keep the expensive application running
        Arrays.asList(server.getConnectors()).forEach(c -> LifeCycle.stop(c));
    }

    @Override
    public synchronized void afterRestore(Context<? extends Resource> context) {
        LOGGER.info("Executing afterRestore");
        Arrays.asList(server.getConnectors()).forEach(c -> LifeCycle.start(c));
    }

    public Server getServer() {
        return server;
    }
}
