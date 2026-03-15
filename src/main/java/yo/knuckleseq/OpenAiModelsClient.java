package yo.knuckleseq;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

final class OpenAiModelsClient {

    private final HttpClient httpClient;

    OpenAiModelsClient() {
        this(HttpClient.newHttpClient());
    }

    OpenAiModelsClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    Set<String> fetchModelIds(AppConfig.OpenAiConfig openAiConfig, String apiKey) throws IOException, InterruptedException {
        var baseUri = URI.create(openAiConfig.baseUrl() + (openAiConfig.baseUrl().endsWith("/") ? "" : "/"));
        var request = HttpRequest.newBuilder(baseUri.resolve("models"))
            .timeout(Duration.ofSeconds(openAiConfig.timeoutSeconds()))
            .header("Authorization", "Bearer " + apiKey)
            .GET()
            .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Unexpected status code");
        }

        var root = JsonSupport.LENIENT_MAPPER.readTree(response.body());
        var modelIds = new HashSet<String>();

        for (var modelNode : root.path("data")) {
            var modelId = modelNode.path("id").asText();
            if (!modelId.isBlank()) {
                modelIds.add(modelId);
            }
        }

        return modelIds;
    }
}
