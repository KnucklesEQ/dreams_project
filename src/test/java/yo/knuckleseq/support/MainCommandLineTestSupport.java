package yo.knuckleseq.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class MainCommandLineTestSupport {

    private MainCommandLineTestSupport() {
    }

    public static CommandLineExecutionResult runMain(String... args) throws IOException, InterruptedException {
        return runMain(Path.of("."), List.of(), Map.of(), args);
    }

    public static CommandLineExecutionResult runMain(Path workingDirectory, String... args)
        throws IOException, InterruptedException {
        return runMain(workingDirectory, List.of(), Map.of(), args);
    }

    public static CommandLineExecutionResult runMain(Path workingDirectory, List<String> jvmArguments, String... args)
        throws IOException, InterruptedException {
        return runMain(workingDirectory, jvmArguments, Map.of(), args);
    }

    public static CommandLineExecutionResult runMain(Path workingDirectory, Map<String, String> environment, String... args)
        throws IOException, InterruptedException {
        return runMain(workingDirectory, List.of(), environment, args);
    }

    public static CommandLineExecutionResult runMain(
        Path workingDirectory,
        List<String> jvmArguments,
        Map<String, String> environment,
        String... args
    )
        throws IOException, InterruptedException {
        var command = new ArrayList<String>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.addAll(jvmArguments);
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("yo.knuckleseq.Main");
        command.addAll(List.of(args));

        var processBuilder = new ProcessBuilder(command)
            .directory(workingDirectory.toFile());

        processBuilder.environment().putAll(environment);

        var process = processBuilder.start();

        var finished = process.waitFor(10, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("Main process did not finish in time");
        }

        var stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        var stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

        return new CommandLineExecutionResult(process.exitValue(), stdout, stderr);
    }

    public static record CommandLineExecutionResult(int exitCode, String stdout, String stderr) {
    }
}
