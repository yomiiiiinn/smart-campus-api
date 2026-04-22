package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;

public class Main {

    // Grizzly needs the full base URI. The @ApplicationPath on the Application
    // subclass isn't picked up automatically by the embedded server, so we
    // mount it here at /api/v1 to match.
    public static final String BASE_URI = "http://localhost:8080/api/v1/";

    public static HttpServer startServer() {
        ResourceConfig config = ResourceConfig.forApplication(new SmartCampusApplication());
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = startServer();
        System.out.printf("Smart Campus API running at %s%nPress Ctrl+C to stop.%n", BASE_URI);
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
        Thread.currentThread();
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
