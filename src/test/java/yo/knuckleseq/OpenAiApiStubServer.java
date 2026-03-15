package yo.knuckleseq;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

final class OpenAiApiStubServer implements AutoCloseable {

    private final HttpServer server;

    private OpenAiApiStubServer(HttpServer server) {
        this.server = server;
    }

    static OpenAiApiStubServer startWithModels(String... modelIds) throws IOException {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var responseBody = modelsResponse(modelIds);
        var handler = new JsonHandler(200, responseBody);

        server.createContext("/v1/models", handler);
        server.createContext("/models", handler);
        server.start();

        return new OpenAiApiStubServer(server);
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private static String modelsResponse(String... modelIds) {
        var data = Arrays.stream(modelIds)
            .map(id -> """
                {
                  "id": "%s",
                  "object": "model"
                }
                """.formatted(id))
            .toList();

        return """
            {
              "object": "list",
              "data": [%s]
            }
            """.formatted(String.join(",", data));
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
