package yo.knuckleseq;

import java.io.IOException;
import java.util.Set;

final class DoctorService {

    private final EnvironmentSupport environmentSupport;
    private final OpenAiModelsClient openAiModelsClient;

    DoctorService() {
        this(new EnvironmentSupport(), new OpenAiModelsClient());
    }

    DoctorService(EnvironmentSupport environmentSupport, OpenAiModelsClient openAiModelsClient) {
        this.environmentSupport = environmentSupport;
        this.openAiModelsClient = openAiModelsClient;
    }

    CommandResult validate(AppConfig config, boolean online) {
        try {
            FilesystemSupport.ensureRuntimeDirectories(config);
        } catch (IOException exception) {
            return CommandResult.usageError(CliTexts.LOCAL_ENV_INVALID);
        }

        if (!online) {
            return CommandResult.success(CliTexts.DOCTOR_LOCAL_OK);
        }

        return validateOnline(config);
    }

    private CommandResult validateOnline(AppConfig config) {
        var apiKey = environmentSupport.resolveEnvReference(config.openai().apiKeyRef());
        if (apiKey == null) {
            return CommandResult.usageError(CliTexts.MISSING_API_KEY);
        }

        try {
            var modelIds = openAiModelsClient.fetchModelIds(config.openai(), apiKey);
            if (!supportsConfiguredModels(config, modelIds)) {
                return CommandResult.externalError(CliTexts.UNSUPPORTED_MODELS);
            }

            return CommandResult.success(CliTexts.DOCTOR_ONLINE_OK);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return CommandResult.externalError(CliTexts.REMOTE_UNAVAILABLE);
        } catch (IOException exception) {
            return CommandResult.externalError(CliTexts.REMOTE_UNAVAILABLE);
        }
    }

    private boolean supportsConfiguredModels(AppConfig config, Set<String> modelIds) {
        return modelIds.contains(config.openai().transcriptionModel())
            && modelIds.contains(config.openai().cleanupModel())
            && modelIds.contains(config.openai().analysisModel());
    }
}
