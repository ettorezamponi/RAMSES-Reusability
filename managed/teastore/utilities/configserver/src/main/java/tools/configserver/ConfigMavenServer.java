package tools.configserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

public class ConfigMavenServer {

    public static void main(String[] args) throws IOException {


        int port = 8081;

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/configserver", new ConfigServerHandler());
        server.setExecutor(null); // Usa l'executor predefinito
        server.start();

    }

    static class ConfigServerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            String fileName = requestPath.substring(requestPath.lastIndexOf("/") + 1);

            try {
                String configuration = readGitHubProperties(fileName);
                sendResponse(exchange, configuration);
            } catch (IOException e) {
                e.printStackTrace();
                sendResponse(exchange, "Errore durante la lettura del file.");
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        private String readGitHubProperties(String fileName) throws IOException, URISyntaxException {
            // Costruisci l'URL GitHub utilizzando l'URL base della tua repo e il nome del file
            String githubUrl = "https://raw.githubusercontent.com/ettorezamponi/config-server/main/" + fileName;

            URI uri = new URI(githubUrl);
            HttpGet httpGet = new HttpGet(uri);

            try (CloseableHttpClient httpClient = HttpClients.createDefault();
                 CloseableHttpResponse response = httpClient.execute(httpGet)) {

                String responseBody = EntityUtils.toString(response.getEntity());
                return responseBody;
            }
        }

        private void sendResponse(HttpExchange exchange, String response) throws IOException {
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}

