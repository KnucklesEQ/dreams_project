package yo.knuckleseq;

import java.io.PrintStream;
import java.util.List;

final class SonologCli {

    private final AppHomeInitializer appHomeInitializer = new AppHomeInitializer();
    private final DoctorCommand doctorCommand = new DoctorCommand();
    private final ScanCommand scanCommand = new ScanCommand();
    private final StatusCommand statusCommand = new StatusCommand();
    private final List<String> arguments;
    private final ApplicationMetadata metadata;

    SonologCli(String[] arguments) {
        this(arguments, ApplicationMetadata.load());
    }

    SonologCli(String[] arguments, ApplicationMetadata metadata) {
        this.arguments = List.of(arguments.clone());
        this.metadata = metadata;
    }

    int run(PrintStream stdout, PrintStream stderr) {
        var result = resolve();
        result.writeTo(stdout, stderr);
        return result.exitCode();
    }

    private CommandResult resolve() {
        if (arguments.isEmpty()) {
            return CommandResult.usageError(CliTexts.GENERAL_USAGE);
        }

        var command = arguments.getFirst();
        var commandArguments = arguments.subList(1, arguments.size());

        return switch (command) {
            case "version", "--version" -> resolveStandaloneCommand(CliTexts.versionOutput(metadata), CliTexts.VERSION_USAGE, commandArguments);
            case "help", "--help" -> resolveHelpCommand(commandArguments);
            case "init" -> appHomeInitializer.execute(commandArguments);
            case "doctor" -> doctorCommand.execute(commandArguments);
            case "scan" -> scanCommand.execute(commandArguments);
            case "status" -> statusCommand.execute(commandArguments);
            default -> CommandResult.usageError(CliTexts.GENERAL_USAGE);
        };
    }

    private CommandResult resolveStandaloneCommand(String stdout, String usageMessage, List<String> commandArguments) {
        if (commandArguments.isEmpty()) {
            return CommandResult.success(stdout);
        }

        return CommandResult.usageError(usageMessage);
    }

    private CommandResult resolveHelpCommand(List<String> commandArguments) {
        if (commandArguments.isEmpty()) {
            return CommandResult.success(CliTexts.generalHelpOutput(metadata));
        }

        if (commandArguments.size() == 1) {
            return resolveCommandSpecificHelp(commandArguments.getFirst());
        }

        return CommandResult.usageError(CliTexts.HELP_USAGE);
    }

    private CommandResult resolveCommandSpecificHelp(String command) {
        return switch (command) {
            case "process" -> CommandResult.success(CliTexts.PROCESS_HELP_OUTPUT);
            default -> CommandResult.usageError(CliTexts.UNKNOWN_COMMAND_HELP_MESSAGE);
        };
    }
}
