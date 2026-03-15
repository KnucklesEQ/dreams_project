package yo.knuckleseq;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class AppHomeInitializer {

    private static final List<String> INIT_DIRS = List.of(
        "config",
        "input",
        "archive",
        "notes",
        "workspace",
        "runs",
        "logs",
        "tmp"
    );

    CommandResult execute(List<String> arguments) {
        InitOptions options;

        try {
            options = parseOptions(arguments);
        } catch (IllegalArgumentException exception) {
            return CommandResult.usageError(CliTexts.INIT_USAGE);
        }

        return initialize(options);
    }

    private InitOptions parseOptions(List<String> arguments) {
        Path home = null;
        boolean force = false;

        for (int index = 0; index < arguments.size(); index++) {
            var argument = arguments.get(index);

            switch (argument) {
                case "--home" -> {
                    if (index + 1 >= arguments.size()) {
                        throw new IllegalArgumentException("Missing init home argument");
                    }

                    home = Path.of(arguments.get(++index));
                }
                case "--force" -> force = true;
                default -> throw new IllegalArgumentException("Unknown init argument");
            }
        }

        return new InitOptions(home, force);
    }

    private CommandResult initialize(InitOptions options) {
        var appHome = resolveHome(options.home());
        var configPath = appHome.resolve("config/config.json");

        try {
            if (Files.exists(configPath) && !options.force()) {
                return CommandResult.safetyBlock(CliTexts.INIT_OVERWRITE_BLOCK_MESSAGE);
            }

            createHomeLayout(appHome);
            Files.writeString(configPath, AppConfig.defaultConfigJson(appHome));
            return CommandResult.success();
        } catch (IOException exception) {
            return CommandResult.usageError("No se pudo inicializar el directorio de inicio: " + exception.getMessage() + "\n");
        }
    }

    private Path resolveHome(Path requestedHome) {
        if (requestedHome == null) {
            return AppConfig.defaultHomeDir().toAbsolutePath().normalize();
        }

        return requestedHome.toAbsolutePath().normalize();
    }

    private void createHomeLayout(Path home) throws IOException {
        for (var directory : INIT_DIRS) {
            Files.createDirectories(home.resolve(directory));
        }
    }

    private record InitOptions(Path home, boolean force) {
    }
}
