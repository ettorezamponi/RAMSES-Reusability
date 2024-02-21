package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class App {
    public static void main(String[] args) {
        String urlString = "http://persistence:8080/tools.descartes.teastore.persistence/rest/orders/prometheus";

        try {
            URL url = new URL(urlString);
            OpenConnectionDetails connectionDetails = openConnection(url);

            System.out.println("Open connection INPUT STREAM: " + connectionDetails.inputStream);
            System.out.println("Open connection TYPE: " + connectionDetails.contentType);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // see openConnection() for where this is used
    protected static class OpenConnectionDetails {
        public final InputStream inputStream;
        public final String contentType;

        public OpenConnectionDetails(InputStream is, String contentType) {
            this.inputStream = is;
            this.contentType = contentType;
        }
    }

    protected static OpenConnectionDetails openConnection(URL endpointUrl) throws IOException {
        URLConnection conn = endpointUrl.openConnection();
        //conn.setRequestProperty("Accept", "application/*");
        InputStream stream = conn.getInputStream();
        String contentType = conn.getContentType();
        return new OpenConnectionDetails(stream, contentType);
    }
}
