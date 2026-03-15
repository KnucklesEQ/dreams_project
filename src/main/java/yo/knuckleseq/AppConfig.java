package yo.knuckleseq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

record AppConfig(
    Path configPath,
    int schemaVersion,
    Path appHomeDir,
    PathsConfig paths,
    OpenAiConfig openai,
    PipelineConfig pipeline,
    ExecutionConfig execution
) {

    static Path defaultHomeDir() {
        return Path.of(System.getProperty("user.home"), "Documents", "sonolog");
    }

    static Path defaultConfigPath() {
        return defaultHomeDir().resolve("config/config.json");
    }

    static String defaultConfigJson(Path appHome) {
        var normalizedAppHome = appHome.toAbsolutePath().normalize();

        return """
            {
              "schemaVersion": 1,
              "appHomeDir": "%s",
              "paths": {
                "inputDir": "%s",
                "archiveDir": "%s",
                "notesDir": "%s",
                "workspaceDir": "%s",
                "runsDir": "%s",
                "logsDir": "%s",
                "tmpDir": "%s"
              },
              "openai": {
                "apiKeyRef": "env:OPENAI_API_KEY",
                "baseUrl": "https://api.openai.com/v1",
                "transcriptionModel": "whisper-1",
                "cleanupModel": "gpt-4.1-mini",
                "analysisModel": "gpt-4.1-mini",
                "timeoutSeconds": 120,
                "maxRetries": 3
              },
              "pipeline": {
                "locale": "es",
                "promptSet": "es-v1",
                "skipIfUnchanged": true
              },
              "execution": {
                "maxParallelFiles": 2,
                "continueOnError": true
              }
            }
            """.formatted(
            normalizedAppHome,
            normalizedAppHome.resolve("input"),
            normalizedAppHome.resolve("archive"),
            normalizedAppHome.resolve("notes"),
            normalizedAppHome.resolve("workspace"),
            normalizedAppHome.resolve("runs"),
            normalizedAppHome.resolve("logs"),
            normalizedAppHome.resolve("tmp")
        );
    }

    static AppConfig load(Path configPath) throws IOException {
        var absoluteConfigPath = configPath.toAbsolutePath().normalize();
        var rawConfig = JsonSupport.STRICT_MAPPER.readValue(Files.readString(absoluteConfigPath), RawConfig.class);
        return from(rawConfig, absoluteConfigPath);
    }

    private static AppConfig from(RawConfig rawConfig, Path configPath) {
        require(rawConfig.schemaVersion() == 1, "schemaVersion invalido");

        var rawAppHome = required(rawConfig.appHomeDir(), "appHomeDir requerido");
        var appHome = resolvePath(rawAppHome, null);
        var rawPaths = requireNonNull(rawConfig.paths(), "paths requerido");
        var rawOpenAi = requireNonNull(rawConfig.openai(), "openai requerido");
        var rawPipeline = requireNonNull(rawConfig.pipeline(), "pipeline requerido");
        var rawExecution = requireNonNull(rawConfig.execution(), "execution requerido");

        var inputDir = resolvePath(required(rawPaths.inputDir(), "paths.inputDir requerido"), appHome);
        var archiveDir = resolvePath(defaulted(rawPaths.archiveDir(), appHome.resolve("archive")), appHome);
        var notesDir = resolvePath(required(rawPaths.notesDir(), "paths.notesDir requerido"), appHome);
        var workspaceDir = resolvePath(defaulted(rawPaths.workspaceDir(), appHome.resolve("workspace")), appHome);
        var runsDir = resolvePath(defaulted(rawPaths.runsDir(), appHome.resolve("runs")), appHome);
        var logsDir = resolvePath(defaulted(rawPaths.logsDir(), appHome.resolve("logs")), appHome);
        var tmpDir = resolvePath(defaulted(rawPaths.tmpDir(), appHome.resolve("tmp")), appHome);

        var apiKeyRef = required(rawOpenAi.apiKeyRef(), "openai.apiKeyRef requerido");
        var baseUrl = required(defaulted(rawOpenAi.baseUrl(), "https://api.openai.com/v1"), "openai.baseUrl requerido");
        var transcriptionModel = required(rawOpenAi.transcriptionModel(), "openai.transcriptionModel requerido");
        var cleanupModel = required(rawOpenAi.cleanupModel(), "openai.cleanupModel requerido");
        var analysisModel = required(rawOpenAi.analysisModel(), "openai.analysisModel requerido");
        var timeoutSeconds = rawOpenAi.timeoutSeconds() == null ? 120 : rawOpenAi.timeoutSeconds();
        var maxRetries = rawOpenAi.maxRetries() == null ? 3 : rawOpenAi.maxRetries();

        var locale = required(rawPipeline.locale(), "pipeline.locale requerido");
        var promptSet = required(rawPipeline.promptSet(), "pipeline.promptSet requerido");
        var skipIfUnchanged = rawPipeline.skipIfUnchanged() == null || rawPipeline.skipIfUnchanged();
        var maxParallelFiles = rawExecution.maxParallelFiles() == null ? 2 : rawExecution.maxParallelFiles();
        var continueOnError = rawExecution.continueOnError() == null || rawExecution.continueOnError();

        require(apiKeyRef.startsWith("env:") && apiKeyRef.length() > 4, "openai.apiKeyRef invalido");
        require("es".equals(locale), "pipeline.locale invalido");
        require(timeoutSeconds > 0, "openai.timeoutSeconds invalido");
        require(maxRetries >= 0, "openai.maxRetries invalido");
        require(maxParallelFiles >= 1, "execution.maxParallelFiles invalido");

        return new AppConfig(
            configPath,
            rawConfig.schemaVersion(),
            appHome,
            new PathsConfig(inputDir, archiveDir, notesDir, workspaceDir, runsDir, logsDir, tmpDir),
            new OpenAiConfig(apiKeyRef, baseUrl, transcriptionModel, cleanupModel, analysisModel, timeoutSeconds, maxRetries),
            new PipelineConfig(locale, promptSet, skipIfUnchanged),
            new ExecutionConfig(maxParallelFiles, continueOnError)
        );
    }

    private static String required(String value, String message) {
        require(value != null && !value.isBlank(), message);
        return value;
    }

    private static String defaulted(String value, Path fallback) {
        return value == null || value.isBlank() ? fallback.toString() : value;
    }

    private static String defaulted(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static <T> T requireNonNull(T value, String message) {
        require(value != null, message);
        return value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static Path resolvePath(String rawPath, Path appHomeDir) {
        var expandedPath = expandHome(rawPath);
        var path = Path.of(expandedPath);

        if (path.isAbsolute()) {
            return path.toAbsolutePath().normalize();
        }

        if (appHomeDir == null) {
            return path.toAbsolutePath().normalize();
        }

        return appHomeDir.resolve(path).toAbsolutePath().normalize();
    }

    private static String expandHome(String rawPath) {
        if (rawPath.startsWith("~/")) {
            return Path.of(System.getProperty("user.home"), rawPath.substring(2)).toString();
        }

        if (rawPath.equals("~")) {
            return System.getProperty("user.home");
        }

        return rawPath;
    }

    record PathsConfig(
        Path inputDir,
        Path archiveDir,
        Path notesDir,
        Path workspaceDir,
        Path runsDir,
        Path logsDir,
        Path tmpDir
    ) {
    }

    record OpenAiConfig(
        String apiKeyRef,
        String baseUrl,
        String transcriptionModel,
        String cleanupModel,
        String analysisModel,
        int timeoutSeconds,
        int maxRetries
    ) {
    }

    record PipelineConfig(String locale, String promptSet, boolean skipIfUnchanged) {
    }

    record ExecutionConfig(int maxParallelFiles, boolean continueOnError) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    private record RawConfig(
        int schemaVersion,
        String appHomeDir,
        RawPaths paths,
        RawOpenAi openai,
        RawPipeline pipeline,
        RawExecution execution
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    private record RawPaths(
        String inputDir,
        String archiveDir,
        String notesDir,
        String workspaceDir,
        String runsDir,
        String logsDir,
        String tmpDir
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    private record RawOpenAi(
        String apiKeyRef,
        String baseUrl,
        String transcriptionModel,
        String cleanupModel,
        String analysisModel,
        Integer timeoutSeconds,
        Integer maxRetries
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    private record RawPipeline(String locale, String promptSet, Boolean skipIfUnchanged) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    private record RawExecution(Integer maxParallelFiles, Boolean continueOnError) {
    }
}
