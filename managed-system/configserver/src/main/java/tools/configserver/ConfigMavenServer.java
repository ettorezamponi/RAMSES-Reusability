package tools.configserver;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

@WebServlet("/configserver/*")
public class ConfigMavenServer extends HttpServlet {
    // http://localhost:8081/configserver/teastore-auth.properties

    private static final String GITHUB_BASE_URL = "https://raw.githubusercontent.com/ettorezamponi/teastore-configserver/main/";
    public static void main(String[] args) {
        Server server = new Server(8081);

        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setContextPath("/");

        contextHandler.addServlet(new ServletHolder(new ConfigMavenServer()), "/configserver/*");

        server.setHandler(contextHandler);

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String fileName = request.getPathInfo().substring(1);

        try {
            String configuration = readGitHubProperties(fileName);
            response.getWriter().write(configuration);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Errore durante la lettura del file.");
        }
    }

    private String readGitHubProperties(String fileName) throws IOException, URISyntaxException {
        String githubUrl = GITHUB_BASE_URL + fileName;
        URI uri = new URI(githubUrl);
        System.out.println("URL TO REACH: "+uri);
        try {
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");

            try (InputStream inputStream = connection.getInputStream();
                 Scanner scanner = new Scanner(inputStream)) {

                scanner.useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : "";
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
