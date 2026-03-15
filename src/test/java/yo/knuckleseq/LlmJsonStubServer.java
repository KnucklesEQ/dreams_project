package yo.knuckleseq;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

final class LlmJsonStubServer implements AutoCloseable {

    private final HttpServer server;

    private LlmJsonStubServer(HttpServer server) {
        this.server = server;
    }

    static LlmJsonStubServer start(int statusCode, String responseText) throws IOException {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/v1/responses", new JsonHandler(statusCode, responsesPayload(responseText)));
        server.createContext("/responses", new JsonHandler(statusCode, responsesPayload(responseText)));
        server.createContext("/v1/chat/completions", new JsonHandler(statusCode, chatCompletionsPayload(responseText)));
        server.createContext("/chat/completions", new JsonHandler(statusCode, chatCompletionsPayload(responseText)));
        server.start();

        return new LlmJsonStubServer(server);
    }

    static String successfulCleanupJson(String dreamId) {
        return """
            {
              "schemaVersion": 1,
              "dreamId": "%s",
              "createdAt": "2026-03-14T09:17:00Z",
              "sourceHash": "sha256:source",
              "transcriptHash": "sha256:transcript",
              "promptId": "cleanup-es-v1",
              "promptHash": "sha256:prompt-clean",
              "cleanText": "texto limpio y legible",
              "paragraphs": ["texto limpio y legible"],
              "changes": [],
              "uncertainSpans": []
            }
            """.formatted(dreamId);
    }

    static String cleanupJsonWithUnknownKey(String dreamId) {
        return successfulCleanupJson(dreamId).replace(
            "\"uncertainSpans\": []",
            "\"uncertainSpans\": [],\n  \"unexpected\": true"
        );
    }

    static String cleanupJsonWithHallucination(String dreamId) {
        return successfulCleanupJson(dreamId).replace(
            "texto limpio y legible",
            "texto limpio y legible con un dragon inventado"
        );
    }

    static String successfulAnalysisJson(String dreamId) {
        return """
            {
              "schemaVersion": 1,
              "dreamId": "%s",
              "createdAt": "2026-03-14T09:17:40Z",
              "updatedAt": "2026-03-14T09:17:40Z",
              "pipelineVersion": "0.1",
              "locale": "es",
              "source": {
                "manifestPath": "/workspace/%s/manifest.json",
                "sourceAudioName": "A-20250108-071440.wav",
                "sourceSha256": "sha256:source",
                "transcriptHash": "sha256:transcript",
                "cleanedHash": "sha256:cleaned",
                "recordedAt": "2025-01-08T07:14:40",
                "recordedAtSource": "filename",
                "sourceTextUsed": "cleaned"
              },
              "provenance": {
                "runId": "2026-03-14T09-15-02Z",
                "provider": "openai",
                "model": "gpt-4.1-mini",
                "promptId": "analysis-es-v1",
                "promptHash": "sha256:prompt-analysis",
                "inputHash": "sha256:analyze-input"
              },
              "review": {
                "needsReview": true,
                "reviewReasons": []
              },
              "titleCandidate": {
                "text": "Doctor excentrico",
                "certainty": "explicit",
                "evidence": [
                  {
                    "text": "doctor excentrico",
                    "source": "cleaned",
                    "segmentIds": [1]
                  }
                ]
              },
              "entities": {
                "people": [],
                "places": [],
                "emotions": [],
                "motifs": [],
                "tags": []
              },
              "uncertainties": [],
              "coverage": {
                "evidencePolicy": "text_only",
                "unknownCount": 0
              }
            }
            """.formatted(dreamId, dreamId);
    }

    static String analysisJsonWithoutEvidence(String dreamId) {
        return successfulAnalysisJson(dreamId).replace(
            "\"evidence\": [\n                  {\n                    \"text\": \"doctor excentrico\",\n                    \"source\": \"cleaned\",\n                    \"segmentIds\": [1]\n                  }\n                ]",
            "\"evidence\": []"
        );
    }

    static String analysisJsonWithUnsupportedValues(String dreamId) {
        return successfulAnalysisJson(dreamId)
            .replace("\"certainty\": \"explicit\"", "\"certainty\": \"invented\"")
            .replace("\"source\": \"cleaned\"", "\"source\": \"memory\"");
    }

    static String analysisJsonWithoutTitleEvidence(String dreamId) {
        return successfulAnalysisJson(dreamId).replace(
            "\"evidence\": [\n                  {\n                    \"text\": \"doctor excentrico\",\n                    \"source\": \"cleaned\",\n                    \"segmentIds\": [1]\n                  }\n                ]",
            "\"evidence\": []"
        );
    }

    static String analysisJsonWithHallucination(String dreamId) {
        return successfulAnalysisJson(dreamId).replace("Doctor excentrico", "Dragon interdimensional");
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    @Override
    public void close() {
        server.stop(0);
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
