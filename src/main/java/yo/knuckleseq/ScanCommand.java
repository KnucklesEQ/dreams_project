package yo.knuckleseq;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

final class ScanCommand {

    private final ScanService scanService;

    ScanCommand() {
        this(new ScanService());
    }

    ScanCommand(ScanService scanService) {
        this.scanService = scanService;
    }

    CommandResult execute(List<String> arguments) {
        Path configPath;

        try {
            configPath = parseConfigPath(arguments);
        } catch (IllegalArgumentException exception) {
            return CommandResult.usageError(CliTexts.SCAN_USAGE);
        }

        try {
            return scanService.execute(AppConfig.load(configPath));
        } catch (IOException | IllegalArgumentException exception) {
            return CommandResult.usageError(CliTexts.SCAN_FAILED);
        }
    }

    private Path parseConfigPath(List<String> arguments) {
        Path configPath = AppConfig.defaultConfigPath();

        for (int index = 0; index < arguments.size(); index++) {
            var argument = arguments.get(index);

            if (!"--config".equals(argument) || index + 1 >= arguments.size()) {
                throw new IllegalArgumentException("Invalid scan options");
            }

            configPath = Path.of(arguments.get(++index));
        }

        return configPath.toAbsolutePath().normalize();
    }
}
