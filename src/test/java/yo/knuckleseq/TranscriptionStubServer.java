package yo.knuckleseq;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

final class TranscriptionStubServer implements AutoCloseable {

    private final HttpServer server;

    private TranscriptionStubServer(HttpServer server) {
        this.server = server;
    }

    static TranscriptionStubServer start(int statusCode, String responseBody) throws IOException {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var handler = new JsonHandler(statusCode, responseBody);

        server.createContext("/v1/audio/transcriptions", handler);
        server.createContext("/audio/transcriptions", handler);
        server.start();

        return new TranscriptionStubServer(server);
    }

    static String successfulVerboseJsonResponse() {
        return """
            {
              "text": "transcripcion cruda del sueno",
              "language": "es",
              "segments": [
                {
                  "id": 1,
                  "text": "transcripcion cruda del sueno"
                }
              ]
            }
            """;
    }

    static String emptyVerboseJsonResponse() {
        return """
            {
              "text": "",
              "language": "es",
              "segments": []
            }
            """;
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private record JsonHandler(int statusCode, String body) implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            var bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);

            try (var outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        }
    }
}
