
package com.ing.ide.main.dashboard.server;

import com.ing.ide.main.dashboard.server.websocket.HarServlet;
import com.ing.ide.settings.AppSettings;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.api.StatusCode;

@SuppressWarnings("unchecked")
public class DashBoardServer extends Thread {

    private static final Logger LOG = Logger.getLogger(DashBoardServer.class.getName());
    
    public Server server;
    private static final String HOME = "/home.html";
    private static final String R_BASE = "./web";

    public DashBoardServer() {
        this("DBServer@" + port());
    }

    public DashBoardServer(String server) {
        super(server);
    }

    public void prepare() {
        try {
            Tools.verifyLocalPort("DBServer ", port());
            server = new Server();
            DefaultHandler webHandler = new DefaultHandler();
            HandlerList handlers = new HandlerList();
            handlers.setHandlers(new Handler[]{getResourceHandler(),
                getUIWSHandler(), webHandler});

            ServerConnector connector = new ServerConnector(server);
            connector.setPort(port());
            server.setConnectors(new Connector[]{connector});
            server.setHandler(handlers);

            LOG.log(Level.INFO, "DB Server on : http://{0}:{1}",
                    new Object[]{Tools.IP(), port() + ""});

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @Override
    public void run() {
        try {
            server.start();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public String url() {
        return "http://127.0.0.1:" + port();
    }

    public static int port() {
        return Integer.valueOf(AppSettings.get(AppSettings.APP_SETTINGS.HAR_PORT.getKey()));
    }

    ContextHandler getResourceHandler() {
        ContextHandler root = new ContextHandler();
        root.setContextPath("/*");

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(true);

        resourceHandler.setWelcomeFiles(new String[]{HOME});
        resourceHandler.setResourceBase(R_BASE);

        root.setHandler(resourceHandler);

        return root;

    }

    protected ContextHandler getUIWSHandler() {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        ServletHolder holderEvents = getServlet();
        context.addServlet(holderEvents,
                "/dashboard/har");
        return context;
    }

    public ServletHolder getServlet() {
        return new ServletHolder(HarServlet.class);
    }

    public void stopServer() {
        try {
            if (server.isStarted()) {
                HarCompareHandler.closeAll(StatusCode.SHUTDOWN, "EXIT");
                server.stop();
                server.join();
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
