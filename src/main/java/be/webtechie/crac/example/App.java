package be.webtechie.crac.example;

import be.webtechie.crac.example.manager.DataManager;
import be.webtechie.crac.example.manager.ServerManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class App extends AbstractHandler {

    private static final Logger LOGGER = LogManager.getLogger(App.class);

    static ServerManager serverManager;
    static DataManager dataManager;

    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting application from main");
        dataManager = new DataManager();
        serverManager = new ServerManager(8080, new App());
        serverManager.getServer().join();
    }

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        LOGGER.debug("Handling request {}", request.getPathInfo());
        var start = System.currentTimeMillis();
        StringBuilder rt = new StringBuilder();
        if (request.getPathInfo().contains("/files/")) {
            rt.append(dataManager.getDataSet(request.getPathInfo().replace("/files/", "")).toCsv());
        } else if (request.getPathInfo().equals("/")) {
            rt.append("Files:<br/>");
            for (String file : DataManager.FILES) {
                rt.append("<a href='/files/").append(file).append("' target='_blank'>").append(file).append("</a></br>");
            }
        } else {
            // Don't reply
            return;
        }
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println(rt);
        var end = System.currentTimeMillis();
        LOGGER.info("Handled request for {} in {}ms", request.getPathInfo(), (end - start));
    }
}
