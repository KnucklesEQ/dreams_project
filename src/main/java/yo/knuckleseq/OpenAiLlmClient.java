package yo.knuckleseq;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class OpenAiLlmClient {

    private final HttpClient httpClient;

    OpenAiLlmClient() {
        this(HttpClient.newHttpClient());
    }

    OpenAiLlmClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    CompletionPayload completeJson(AppConfig.OpenAiConfig openAiConfig, String apiKey, String promptInput) throws IOException, InterruptedException {
        var endpoint = URI.create(openAiConfig.baseUrl() + (openAiConfig.baseUrl().endsWith("/") ? "" : "/")).resolve("responses");
        var request = HttpRequest.newBuilder(endpoint)
            .timeout(Duration.ofSeconds(openAiConfig.timeoutSeconds()))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"input\":\"" + escapeJson(promptInput) + "\"}"))
            .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Unexpected status code");
        }

        var root = JsonSupport.LENIENT_MAPPER.readTree(response.body());
        var output = root.path("output").path(0).path("content").path(0).path("text").asText(null);
        if (output == null || output.isBlank()) {
            output = root.path("choices").path(0).path("message").path("content").asText("");
        }

        if (output == null || output.isBlank()) {
            throw new IOException("Missing LLM output text");
        }

        return new CompletionPayload(response.body(), output);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    record CompletionPayload(String rawResponseBody, String outputText) {
    }
}
