package yo.knuckleseq;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

final class ProcessCommand {

    private final ProcessService processService;
    private final ScanService scanService;

    ProcessCommand() {
        this(new ProcessService(), new ScanService());
    }

    ProcessCommand(ProcessService processService, ScanService scanService) {
        this.processService = processService;
        this.scanService = scanService;
    }

    CommandResult execute(List<String> arguments) {
        Options options;

        try {
            options = parseOptions(arguments);
        } catch (IllegalArgumentException exception) {
            return CommandResult.usageError(CliTexts.PROCESS_USAGE);
        }

        try {
            var config = AppConfig.load(options.configPath());
            if (options.dreamId() == null) {
                var scanResult = scanService.execute(config);
                if (scanResult.exitCode() != 0) {
                    return scanResult;
                }

                return processService.processAll(config);
            }

            return processService.processKnownItem(config, options.dreamId());
        } catch (IOException | IllegalArgumentException exception) {
            return CommandResult.usageError(CliTexts.PROCESS_FAILED);
        }
    }

    private Options parseOptions(List<String> arguments) {
        Path configPath = AppConfig.defaultConfigPath();
        String dreamId = null;

        for (int index = 0; index < arguments.size(); index++) {
            var argument = arguments.get(index);

            switch (argument) {
                case "--config" -> {
                    if (index + 1 >= arguments.size()) {
                        throw new IllegalArgumentException("Missing config path");
                    }

                    configPath = Path.of(arguments.get(++index));
                }
                default -> {
                    if (argument.startsWith("--") || dreamId != null) {
                        throw new IllegalArgumentException("Invalid process arguments");
                    }

                    dreamId = argument;
                }
            }
        }

        return new Options(configPath.toAbsolutePath().normalize(), dreamId);
    }

    private record Options(Path configPath, String dreamId) {
    }
}
