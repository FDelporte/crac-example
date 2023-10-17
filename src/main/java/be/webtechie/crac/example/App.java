package be.webtechie.crac.example;

import be.webtechie.crac.example.database.AppLog;
import be.webtechie.crac.example.database.Dao;
import be.webtechie.crac.example.database.PostgreSqlDao;
import be.webtechie.crac.example.manager.CsvManager;
import be.webtechie.crac.example.manager.ServerManager;
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

    private static Dao<AppLog, Integer> appLogDao;
    private static CsvManager csvManager;
    private static ServerManager serverManager;

    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting application from main");
        appLogDao = new PostgreSqlDao();
        appLogDao.save(new AppLog("Started from main"));
        csvManager = new CsvManager(appLogDao);
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
            rt.append(csvManager.getDataSet(request.getPathInfo().replace("/files/", "")).toCsv());
        } else if (request.getPathInfo().equals("/logs")) {
            rt.append(appLogDao.getAll().stream()
                    .map(AppLog::toString)
                    .collect(Collectors.joining("\n")));
        } else if (request.getPathInfo().equals("/")) {
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
        appLogDao.save(new AppLog("Handled request for " + request.getPathInfo(), (int) (end - start)));
        LOGGER.info("Handled request for {} in {}ms", request.getPathInfo(), (end - start));
    }
}
