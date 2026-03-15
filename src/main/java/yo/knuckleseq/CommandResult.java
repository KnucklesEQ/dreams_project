package yo.knuckleseq;

import java.io.PrintStream;

record CommandResult(int exitCode, String stdout, String stderr) {

    private static final String EMPTY_OUTPUT = "";
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_USAGE_ERROR = 2;
    private static final int EXIT_EXTERNAL_ERROR = 3;
    private static final int EXIT_SAFETY_BLOCK = 4;

    static CommandResult success() {
        return success(EMPTY_OUTPUT);
    }

    static CommandResult success(String stdout) {
        return new CommandResult(EXIT_SUCCESS, stdout, EMPTY_OUTPUT);
    }

    static CommandResult usageError(String message) {
        return new CommandResult(EXIT_USAGE_ERROR, EMPTY_OUTPUT, message);
    }

    static CommandResult externalError(String message) {
        return new CommandResult(EXIT_EXTERNAL_ERROR, EMPTY_OUTPUT, message);
    }

    static CommandResult safetyBlock(String message) {
        return new CommandResult(EXIT_SAFETY_BLOCK, EMPTY_OUTPUT, message);
    }

    void writeTo(PrintStream stdoutStream, PrintStream stderrStream) {
        if (!stdout.isEmpty()) {
            stdoutStream.print(stdout);
        }

        if (!stderr.isEmpty()) {
            stderrStream.print(stderr);
        }
    }
}
