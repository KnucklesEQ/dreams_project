package yo.knuckleseq;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

final class DoctorCommand {

    private final DoctorService doctorService;

    DoctorCommand() {
        this(new DoctorService());
    }

    DoctorCommand(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    CommandResult execute(List<String> arguments) {
        DoctorOptions options;

        try {
            options = parseOptions(arguments);
        } catch (IllegalArgumentException exception) {
            return CommandResult.usageError(CliTexts.DOCTOR_USAGE);
        }

        AppConfig config;

        try {
            config = AppConfig.load(options.configPath());
        } catch (IOException | IllegalArgumentException exception) {
            return CommandResult.usageError(CliTexts.CONFIG_INVALID);
        }

        return doctorService.validate(config, options.online());
    }

    private DoctorOptions parseOptions(List<String> arguments) {
        Path configPath = AppConfig.defaultConfigPath();
        boolean online = false;

        for (int index = 0; index < arguments.size(); index++) {
            var argument = arguments.get(index);

            switch (argument) {
                case "--online" -> online = true;
                case "--config" -> {
                    if (index + 1 >= arguments.size()) {
                        throw new IllegalArgumentException("Missing config path");
                    }

                    configPath = Path.of(arguments.get(++index));
                }
                default -> throw new IllegalArgumentException("Unknown doctor option");
            }
        }

        return new DoctorOptions(configPath.toAbsolutePath().normalize(), online);
    }

    private record DoctorOptions(Path configPath, boolean online) {
    }
}
