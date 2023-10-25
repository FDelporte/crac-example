package be.webtechie.crac.example;

import be.webtechie.crac.example.manager.CsvManager;
import be.webtechie.crac.example.manager.DatabaseManager;
import be.webtechie.crac.example.manager.ServerManager;
import be.webtechie.crac.example.model.AppLog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

public class App extends AbstractHandler {

    private static final Logger LOGGER = LogManager.getLogger(App.class);

    private static CsvManager csvManager;
    private static DatabaseManager databaseManager;

    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting application from main");
        // Init database and DAO
        databaseManager = new DatabaseManager();
        databaseManager.save(new AppLog("==================================================="));
        databaseManager.save(new AppLog("Started from main"));
        // Init CSV
        csvManager = new CsvManager(databaseManager);
        // Init Jetty server
        ServerManager serverManager = new ServerManager(8080, new App());
        serverManager.getServer().join();
    }

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        LOGGER.debug("Handling request {}", request.getPathInfo());
        var start = System.currentTimeMillis();
        var storeDuration = false;
        StringBuilder rt = new StringBuilder();
        if (request.getPathInfo().contains("/files/")) {
            rt.append(csvManager.getDataSet(request.getPathInfo().replace("/files/", "")).toCsv());
            storeDuration = true;
        } else if (request.getPathInfo().equals("/logs")) {
            rt.append(databaseManager.getAll().stream()
                    .map(AppLog::toString)
                    .collect(Collectors.joining("\n")));
        } else if (request.getPathInfo().equals("/")) {
            rt.append("Running on:<br/>");
            rt.append(System.getProperty("java.runtime.version")).append("<br/>");
            rt.append(System.getProperty("java.vendor")).append("<br/>");
            rt.append(System.getProperty("java.vendor.version")).append("<br/>");
            rt.append("<br/><br/>");
            rt.append("Files:<br/>");
            for (String file : CsvManager.FILES) {
                rt.append("<a href='/files/").append(file).append("' target='_blank'>").append(file).append("</a></br>");
            }
            rt.append("</br></br>");
            rt.append("<a href='/logs' target='_blank'>Logs</a></br>");
        } else {
            // Don't reply
            return;
        }
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println(rt);
        var end = System.currentTimeMillis();
        if (storeDuration) {
            databaseManager.save(new AppLog("Handled request for " + request.getPathInfo(), (int) (end - start)));
        }
        LOGGER.info("Handled request for {} in {}ms", request.getPathInfo(), (end - start));
    }
}
