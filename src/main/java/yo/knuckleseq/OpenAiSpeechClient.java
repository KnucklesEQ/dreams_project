package yo.knuckleseq;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

final class OpenAiSpeechClient {

    private final HttpClient httpClient;

    OpenAiSpeechClient() {
        this(HttpClient.newHttpClient());
    }

    OpenAiSpeechClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    TranscriptionPayload transcribe(AppConfig.OpenAiConfig openAiConfig, Path audioPath, String apiKey) throws IOException, InterruptedException {
        var endpoint = URI.create(openAiConfig.baseUrl() + (openAiConfig.baseUrl().endsWith("/") ? "" : "/")).resolve("audio/transcriptions");
        var request = HttpRequest.newBuilder(endpoint)
            .timeout(Duration.ofSeconds(openAiConfig.timeoutSeconds()))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/octet-stream")
            .POST(HttpRequest.BodyPublishers.ofByteArray(Files.readAllBytes(audioPath)))
            .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Unexpected status code");
        }

        var root = JsonSupport.LENIENT_MAPPER.readTree(response.body());
        var transcriptText = root.path("text").asText("");
        var language = root.path("language").asText("");
        var segmentsNode = root.path("segments");

        if (!segmentsNode.isArray()) {
            throw new IOException("Malformed transcription payload");
        }

        return new TranscriptionPayload(response.body(), transcriptText, language, JsonSupport.toStableJson(segmentsNode));
    }

    record TranscriptionPayload(String rawResponseBody, String transcriptText, String language, String segmentsJson) {
    }
}
