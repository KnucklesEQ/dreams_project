package yo.knuckleseq;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

final class TranscribeCommand {

    private final TranscribeService transcribeService;

    TranscribeCommand() {
        this(new TranscribeService());
    }

    TranscribeCommand(TranscribeService transcribeService) {
        this.transcribeService = transcribeService;
    }

    CommandResult execute(List<String> arguments) {
        Options options;

        try {
            options = parseOptions(arguments);
        } catch (IllegalArgumentException exception) {
            return CommandResult.usageError(CliTexts.TRANSCRIBE_USAGE);
        }

        try {
            var config = AppConfig.load(options.configPath());
            return transcribeService.execute(config, options.dreamId(), options.force());
        } catch (IOException | IllegalArgumentException exception) {
            return CommandResult.usageError(CliTexts.TRANSCRIBE_FAILED);
        }
    }

    private Options parseOptions(List<String> arguments) {
        Path configPath = AppConfig.defaultConfigPath();
        String dreamId = null;
        boolean force = false;

        for (int index = 0; index < arguments.size(); index++) {
            var argument = arguments.get(index);

            switch (argument) {
                case "--config" -> {
                    if (index + 1 >= arguments.size()) {
                        throw new IllegalArgumentException("Missing config path");
                    }

                    configPath = Path.of(arguments.get(++index));
                }
                case "--force" -> force = true;
                default -> {
                    if (argument.startsWith("--") || dreamId != null) {
                        throw new IllegalArgumentException("Invalid transcribe arguments");
                    }

                    dreamId = argument;
                }
            }
        }

        if (dreamId == null || dreamId.isBlank()) {
            throw new IllegalArgumentException("Missing dream id");
        }

        return new Options(configPath.toAbsolutePath().normalize(), dreamId, force);
    }

    private record Options(Path configPath, String dreamId, boolean force) {
    }
}
