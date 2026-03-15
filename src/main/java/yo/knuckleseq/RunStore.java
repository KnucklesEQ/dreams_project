package yo.knuckleseq;

import java.io.IOException;
import java.nio.file.Path;

final class RunStore {

    Path write(AppConfig config, RunDocument runDocument) throws IOException {
        var runPath = config.paths().runsDir().resolve(runDocument.runId()).resolve("run.json").toAbsolutePath().normalize();
        FilesystemSupport.atomicWrite(runPath, JsonSupport.toStableJson(runDocument));
        return runPath;
    }
}
