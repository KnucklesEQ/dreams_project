package yo.knuckleseq;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;

final class OpenAiPipelineStubServer implements AutoCloseable {

    record StubResponse(int statusCode, String responseText) {
    }

    private final HttpServer server;

    private OpenAiPipelineStubServer(HttpServer server) {
        this.server = server;
    }

    static OpenAiPipelineStubServer start(StubResponse transcriptionResponse, StubResponse... llmResponses) throws IOException {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var llmQueue = new ArrayDeque<StubResponse>();

        for (var llmResponse : llmResponses) {
            llmQueue.add(llmResponse);
        }

        server.createContext(
            "/v1/audio/transcriptions",
            new StaticJsonHandler(transcriptionResponse.statusCode(), transcriptionResponse.responseText())
        );
        server.createContext(
            "/audio/transcriptions",
            new StaticJsonHandler(transcriptionResponse.statusCode(), transcriptionResponse.responseText())
        );
        server.createContext(
            "/v1/responses",
            new QueueJsonHandler(llmQueue, true)
        );
        server.createContext(
            "/responses",
            new QueueJsonHandler(llmQueue, true)
        );
        server.createContext(
            "/v1/chat/completions",
            new QueueJsonHandler(llmQueue, false)
        );
        server.createContext(
            "/chat/completions",
            new QueueJsonHandler(llmQueue, false)
        );
        server.start();

        return new OpenAiPipelineStubServer(server);
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private record StaticJsonHandler(int statusCode, String body) implements HttpHandler {

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

    private record QueueJsonHandler(Queue<StubResponse> responses, boolean wrapAsResponsesApi) implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            var response = responses.poll();

            if (response == null) {
                response = new StubResponse(500, "{\"error\":\"no stub response available\"}");
            }

            var body = wrapAsResponsesApi
                ? responsesPayload(response.responseText())
                : chatCompletionsPayload(response.responseText());
            var bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(response.statusCode(), bytes.length);

            try (var outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        }

        private static String responsesPayload(String responseText) {
            return """
                {
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": %s
                        }
                      ]
                    }
                  ]
                }
                """.formatted(jsonString(responseText));
        }

        private static String chatCompletionsPayload(String responseText) {
            return """
                {
                  "choices": [
                    {
                      "message": {
                        "content": %s
                      }
                    }
                  ]
                }
                """.formatted(jsonString(responseText));
        }

        private static String jsonString(String value) {
            return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
        }
    }
}
